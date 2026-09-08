import { randomBytes, randomInt, createHash } from "node:crypto";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2/options";

initializeApp();
const db = getFirestore();

// Pairing is intentionally short-lived and protected by App Check. The
// callable also rate-limits each anonymous device UID so the six-digit code
// is never treated as a standalone bearer credential.
setGlobalOptions({
  region: "europe-west1",
  enforceAppCheck: true,
  maxInstances: 10,
});

const INVITE_TTL_MS = 15 * 60 * 1000;
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

function requireParent(request: Parameters<typeof onCall>[0] extends never ? never : any): string {
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

  if (!familyId) {
    throw new HttpsError("invalid-argument", "Falta el identificador de familia.");
  }

  const memberRef = db.doc(`families/${familyId}/members/${parentUid}`);
  const member = await memberRef.get();
  if (!member.exists || member.data()?.role !== "parent") {
    throw new HttpsError("permission-denied", "No perteneces a esta familia como adulto.");
  }

  const family = await db.doc(`families/${familyId}`).get();
  if (!family.exists) {
    throw new HttpsError("not-found", "No existe la familia indicada.");
  }

  const now = Date.now();
  const expiresAt = Timestamp.fromMillis(now + INVITE_TTL_MS);
  const inviteId = randomBytes(16).toString("hex");
  const token = randomBytes(32).toString("base64url");

  let code = "";
  let codeHash = "";
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const candidate = randomInt(0, 1_000_000).toString().padStart(6, "0");
    const candidateHash = sha256(candidate);
    const existing = await db
      .collectionGroup("invites")
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

  if (!code) {
    throw new HttpsError("resource-exhausted", "No se pudo generar un código de vinculación único.");
  }

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

  // The raw token is returned only to the authenticated parent device. It is
  // never stored in Firestore and can later back a QR/deep-link flow.
  return {
    inviteId,
    code,
    token,
    expiresAtMs: expiresAt.toMillis(),
  };
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

  const codeHash = sha256(code);
  const matches = await db.collectionGroup("invites")
    .where("codeHash", "==", codeHash)
    .where("status", "==", "active")
    .limit(1)
    .get();

  if (matches.empty) {
    throw new HttpsError("not-found", "Código no válido o caducado.");
  }

  const inviteDoc = matches.docs[0];
  const inviteData = inviteDoc.data();
  const familyRef = inviteDoc.ref.parent.parent;
  if (!familyRef) {
    throw new HttpsError("internal", "Invitación de familia inválida.");
  }

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
      const existing = freshMember.data();
      if (existing?.role === "child") {
        tx.update(inviteDoc.ref, {
          status: "used",
          usedAt: Timestamp.fromMillis(now),
          usedByUid: deviceUid,
        });
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

    tx.update(inviteDoc.ref, {
      status: "used",
      usedAt: Timestamp.fromMillis(now),
      usedByUid: deviceUid,
    });
  });

  return {
    familyId,
    childUid: deviceUid,
    linkedAtMs: now,
  };
});
