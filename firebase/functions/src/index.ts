import { randomBytes, randomInt, createHash } from "node:crypto";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2/options";

initializeApp();
const db = getFirestore();

setGlobalOptions({
  region: "europe-west1",
  enforceAppCheck: true,
  maxInstances: 10,
});

const INVITE_TTL_MS = 15 * 60 * 1000;
const PARENT_INVITE_TTL_MS = 24 * 60 * 60 * 1000;
const RATE_WINDOW_MS = 15 * 60 * 1000;
const MAX_ATTEMPTS = 8;

function sha256(value: string): string {
  return createHash("sha256").update(value, "utf8").digest("hex");
}

function normalizeCode(value: unknown): string {
  const code = String(value ?? "").replace(/\D/g, "");
  if (!/^\d{6}$/.test(code)) {
    throw new HttpsError("invalid-argument", "El código debe tener 6 dígitos.");
  }
  return code;
}

function requireParent(request: any): string {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Debes iniciar sesión.");
  }
  if (request.auth.token.firebase?.sign_in_provider !== "google.com") {
    throw new HttpsError("permission-denied", "Solo un adulto autenticado con Google puede realizar esta operación.");
  }
  return request.auth.uid;
}

function requireAnonymousDevice(request: any): string {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "El dispositivo todavía no tiene una identidad temporal.");
  }
  if (request.auth.token.firebase?.sign_in_provider !== "anonymous") {
    throw new HttpsError("permission-denied", "Esta operación está reservada al dispositivo infantil.");
  }
  if (!request.app) {
    throw new HttpsError("failed-precondition", "No se pudo verificar que la solicitud procede de Famyrex.");
  }
  return request.auth.uid;
}

export const createPairingInvite = onCall(async (request) => {
  const parentUid = requireParent(request);
  const familyId = String(request.data?.familyId ?? "").trim();
  const childLabel = String(request.data?.childLabel ?? "Perfil infantil").trim().slice(0, 40) || "Perfil infantil";

  if (!familyId) throw new HttpsError("invalid-argument", "Falta el identificador de familia.");

  const memberRef = db.doc(`families/${familyId}/members/${parentUid}`);
  const member = await memberRef.get();
  if (!member.exists || member.data()?.role !== "parent") {
    throw new HttpsError("permission-denied", "No perteneces a esta familia como adulto.");
  }

  const family = await db.doc(`families/${familyId}`).get();
  if (!family.exists) throw new HttpsError("not-found", "No existe la familia indicada.");

  const now = Date.now();
  const expiresAt = Timestamp.fromMillis(now + INVITE_TTL_MS);
  const inviteId = randomBytes(16).toString("hex");
  const token = randomBytes(32).toString("base64url");

  let code = "";
  let codeHash = "";
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const candidate = randomInt(0, 1_000_000).toString().padStart(6, "0");
    const candidateHash = sha256(candidate);
    const existing = await db.collectionGroup("invites")
      .where("codeHash", "==", candidateHash)
      .where("status", "==", "active")
      .limit(1)
      .get();
    if (existing.empty) {
      code = candidate;
      codeHash = candidateHash;
      break;
    }
  }

  if (!code) throw new HttpsError("resource-exhausted", "No se pudo generar un código de vinculación único.");

  await db.doc(`families/${familyId}/invites/${inviteId}`).set({
    codeHash,
    tokenHash: sha256(token),
    createdByUid: parentUid,
    childLabel,
    createdAt: Timestamp.fromMillis(now),
    expiresAt,
    status: "active",
    usedAt: null,
    usedByUid: null,
  });

  return { inviteId, code, token, expiresAtMs: expiresAt.toMillis() };
});

export const redeemPairingCode = onCall(async (request) => {
  const deviceUid = requireAnonymousDevice(request);
  const code = normalizeCode(request.data?.code);
  const childLabel = String(request.data?.childLabel ?? "Perfil infantil").trim().slice(0, 40) || "Perfil infantil";
  const now = Date.now();
  const rateRef = db.doc(`pairingRateLimits/${deviceUid}`);

  await db.runTransaction(async (tx) => {
    const rateSnap = await tx.get(rateRef);
    const rate = rateSnap.exists ? rateSnap.data() ?? {} : {};
    const windowStartedAt = Number(rate.windowStartedAtMs ?? 0);
    const attempts = Number(rate.attempts ?? 0);
    const inWindow = now - windowStartedAt < RATE_WINDOW_MS;
    const nextAttempts = inWindow ? attempts + 1 : 1;
    if (inWindow && attempts >= MAX_ATTEMPTS) {
      throw new HttpsError("resource-exhausted", "Demasiados intentos. Espera unos minutos antes de volver a probar.");
    }
    tx.set(rateRef, {
      windowStartedAtMs: inWindow ? windowStartedAt : now,
      attempts: nextAttempts,
      updatedAt: Timestamp.fromMillis(now),
    });
  });

  const matches = await db.collectionGroup("invites")
    .where("codeHash", "==", sha256(code))
    .where("status", "==", "active")
    .limit(1)
    .get();
  if (matches.empty) throw new HttpsError("not-found", "Código no válido o caducado.");

  const inviteDoc = matches.docs[0];
  const inviteData = inviteDoc.data();
  const familyRef = inviteDoc.ref.parent.parent;
  if (!familyRef) throw new HttpsError("internal", "Invitación de familia inválida.");

  const expiresAt = inviteData.expiresAt as Timestamp | undefined;
  if (!expiresAt || expiresAt.toMillis() <= now) {
    await inviteDoc.ref.update({ status: "expired" });
    throw new HttpsError("deadline-exceeded", "El código de vinculación ha caducado.");
  }

  const familyId = familyRef.id;
  const memberRef = familyRef.collection("members").doc(deviceUid);

  await db.runTransaction(async (tx) => {
    const freshInvite = await tx.get(inviteDoc.ref);
    if (!freshInvite.exists || freshInvite.data()?.status !== "active") {
      throw new HttpsError("already-exists", "Esta invitación ya ha sido utilizada.");
    }
    const freshMember = await tx.get(memberRef);
    if (freshMember.exists) {
      if (freshMember.data()?.role === "child") {
        tx.update(inviteDoc.ref, { status: "used", usedAt: Timestamp.fromMillis(now), usedByUid: deviceUid });
        return;
      }
      throw new HttpsError("already-exists", "Este dispositivo ya pertenece a una familia.");
    }
    tx.set(memberRef, {
      uid: deviceUid,
      role: "child",
      displayName: childLabel || inviteData.childLabel || "Perfil infantil",
      createdAt: Timestamp.fromMillis(now),
      status: "active",
      deviceUid,
    });
    tx.update(inviteDoc.ref, { status: "used", usedAt: Timestamp.fromMillis(now), usedByUid: deviceUid });
  });

  return { familyId, childUid: deviceUid, linkedAtMs: now };
});

export const createParentInvite = onCall(async (request) => {
  const parentUid = requireParent(request);
  const familyId = String(request.data?.familyId ?? "").trim();
  if (!familyId) throw new HttpsError("invalid-argument", "Falta el identificador de familia.");

  const member = await db.doc(`families/${familyId}/members/${parentUid}`).get();
  if (!member.exists || member.data()?.role !== "parent") {
    throw new HttpsError("permission-denied", "No perteneces a esta familia como adulto.");
  }

  const inviteId = randomBytes(16).toString("hex");
  const token = randomBytes(32).toString("base64url");
  const expiresAt = Timestamp.fromMillis(Date.now() + PARENT_INVITE_TTL_MS);

  await db.doc(`families/${familyId}/parentInvites/${inviteId}`).set({
    tokenHash: sha256(token),
    createdByUid: parentUid,
    createdAt: Timestamp.now(),
    expiresAt,
    status: "active",
  });

  return { inviteId, token, expiresAtMs: expiresAt.toMillis() };
});

export const acceptParentInvite = onCall(async (request) => {
  const newParentUid = requireParent(request);
  const familyId = String(request.data?.familyId ?? "").trim();
  const inviteId = String(request.data?.inviteId ?? "").trim();
  const token = String(request.data?.token ?? "").trim();
  const displayName = String(request.data?.displayName ?? "Adulto autorizado").trim().slice(0, 60) || "Adulto autorizado";

  if (!familyId || !inviteId || !token) {
    throw new HttpsError("invalid-argument", "La invitación de adulto está incompleta.");
  }

  const inviteRef = db.doc(`families/${familyId}/parentInvites/${inviteId}`);
  const invite = await inviteRef.get();
  if (!invite.exists || invite.data()?.status !== "active") {
    throw new HttpsError("not-found", "La invitación de adulto no es válida.");
  }

  const data = invite.data()!;
  const expiresAt = data.expiresAt as Timestamp | undefined;
  if (!expiresAt || expiresAt.toMillis() <= Date.now()) {
    await inviteRef.update({ status: "expired" });
    throw new HttpsError("deadline-exceeded", "La invitación de adulto ha caducado.");
  }

  if (data.tokenHash !== sha256(token)) {
    throw new HttpsError("permission-denied", "La invitación de adulto no coincide.");
  }

  const family = await db.doc(`families/${familyId}`).get();
  if (!family.exists) throw new HttpsError("not-found", "La familia no existe.");

  const memberRef = db.doc(`families/${familyId}/members/${newParentUid}`);
  const existing = await memberRef.get();
  if (existing.exists) {
    if (existing.data()?.role === "parent") return { familyId, uid: newParentUid, alreadyMember: true };
    throw new HttpsError("already-exists", "Esta cuenta ya está vinculada a otro perfil de la familia.");
  }

  await db.runTransaction(async (tx) => {
    const freshInvite = await tx.get(inviteRef);
    if (!freshInvite.exists || freshInvite.data()?.status !== "active") {
      throw new HttpsError("already-exists", "La invitación ya ha sido utilizada.");
    }
    tx.set(memberRef, {
      uid: newParentUid,
      role: "parent",
      displayName,
      status: "active",
      createdAt: Timestamp.now(),
    });
    tx.update(inviteRef, { status: "used", usedAt: Timestamp.now(), usedByUid: newParentUid });
  });

  return { familyId, uid: newParentUid, alreadyMember: false };
});
