import { randomBytes } from "node:crypto";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";

const db = getFirestore();
const TTL_MS = 24 * 60 * 60 * 1000;

function requireGoogleAdult(request: any): string {
  if (!request.auth) throw new HttpsError("unauthenticated", "Debes iniciar sesión con Google.");
  if (request.auth.token.firebase?.sign_in_provider !== "google.com") {
    throw new HttpsError("permission-denied", "Solo un adulto autenticado con Google puede realizar esta operación.");
  }
  return request.auth.uid;
}

export const createSecureParentInvite = onCall(async (request) => {
  const parentUid = requireGoogleAdult(request);
  const familyId = String(request.data?.familyId ?? "").trim();
  if (!familyId) throw new HttpsError("invalid-argument", "Falta el identificador de familia.");

  const parentRef = db.doc(`families/${familyId}/members/${parentUid}`);
  const familyRef = db.doc(`families/${familyId}`);
  const [parent, family] = await Promise.all([parentRef.get(), familyRef.get()]);
  if (!family.exists) throw new HttpsError("not-found", "La familia no existe.");
  if (!parent.exists || parent.data()?.role !== "parent") {
    throw new HttpsError("permission-denied", "No perteneces a esta familia como adulto.");
  }

  const inviteId = randomBytes(32).toString("hex");
  const expiresAt = Timestamp.fromMillis(Date.now() + TTL_MS);
  await db.doc(`parentInvites/${inviteId}`).set({
    inviteId,
    familyId,
    createdByUid: parentUid,
    createdAt: Timestamp.now(),
    expiresAt,
    status: "active",
  });

  return { inviteId, expiresAtMs: expiresAt.toMillis() };
});

export const acceptSecureParentInvite = onCall(async (request) => {
  const newParentUid = requireGoogleAdult(request);
  const inviteId = String(request.data?.inviteId ?? "").trim();
  const displayName = String(request.data?.displayName ?? "Adulto autorizado").trim().slice(0, 60) || "Adulto autorizado";
  if (!/^[a-f0-9]{64}$/.test(inviteId)) throw new HttpsError("invalid-argument", "La invitación no es válida.");

  const inviteRef = db.doc(`parentInvites/${inviteId}`);
  const invite = await inviteRef.get();
  if (!invite.exists || invite.data()?.status !== "active") {
    throw new HttpsError("not-found", "La invitación de adulto no es válida.");
  }

  const data = invite.data()!;
  const familyId = String(data.familyId ?? "").trim();
  const expiresAt = data.expiresAt as Timestamp | undefined;
  if (!familyId || !expiresAt) throw new HttpsError("internal", "La invitación está incompleta.");
  if (expiresAt.toMillis() <= Date.now()) {
    await inviteRef.update({ status: "expired" });
    throw new HttpsError("deadline-exceeded", "La invitación de adulto ha caducado.");
  }

  const familyRef = db.doc(`families/${familyId}`);
  const memberRef = familyRef.collection("members").doc(newParentUid);
  const family = await familyRef.get();
  if (!family.exists) throw new HttpsError("not-found", "La familia no existe.");

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
