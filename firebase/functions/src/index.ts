import { randomBytes, randomInt, createHash } from "node:crypto";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2/options";

initializeApp();
const db = getFirestore();
const messaging = getMessaging();

setGlobalOptions({ region: "europe-west1", enforceAppCheck: true, maxInstances: 10 });

const INVITE_TTL_MS = 15 * 60 * 1000;
const PARENT_INVITE_TTL_MS = 24 * 60 * 60 * 1000;
const COMMAND_TTL_MS = 10 * 60 * 1000;
const RATE_WINDOW_MS = 15 * 60 * 1000;
const MAX_ATTEMPTS = 8;
const MAX_COMMAND_VALUE_LENGTH = 64 * 1024;
const ALLOWED_COMMAND_ACTIONS = new Set([
  "LOCK_DEVICE", "UNLOCK_DEVICE", "GRANT_EXTRA_TIME", "SET_DAILY_LIMIT", "SET_SCHEDULE",
  "BLOCK_APP", "ALLOW_APP", "SET_APP_LIMIT", "SYNC_POLICY",
]);

function sha256(value: string): string { return createHash("sha256").update(value, "utf8").digest("hex"); }
function normalizeCode(value: unknown): string {
  const code = String(value ?? "").replace(/\D/g, "");
  if (!/^\d{6}$/.test(code)) throw new HttpsError("invalid-argument", "El código debe tener 6 dígitos.");
  return code;
}
function normalizeDeviceId(value: unknown): string {
  const id = String(value ?? "").trim();
  if (!/^[A-Za-z0-9._:-]{8,128}$/.test(id)) throw new HttpsError("invalid-argument", "El identificador del dispositivo no es válido.");
  return id;
}
function normalizeMemberId(value: unknown): string {
  const id = String(value ?? "").trim();
  if (!/^[A-Za-z0-9._:-]{8,128}$/.test(id)) throw new HttpsError("invalid-argument", "El identificador del miembro no es válido.");
  return id;
}
function requireParent(request: any): string {
  if (!request.auth) throw new HttpsError("unauthenticated", "Debes iniciar sesión.");
  if (request.auth.token.firebase?.sign_in_provider !== "google.com") throw new HttpsError("permission-denied", "Solo un adulto autenticado con Google puede realizar esta operación.");
  return request.auth.uid;
}
function requireAnonymousDevice(request: any): string {
  if (!request.auth) throw new HttpsError("unauthenticated", "El dispositivo todavía no tiene una identidad temporal.");
  if (request.auth.token.firebase?.sign_in_provider !== "anonymous") throw new HttpsError("permission-denied", "Esta operación está reservada al dispositivo infantil.");
  if (!request.app) throw new HttpsError("failed-precondition", "No se pudo verificar que la solicitud procede de Famyrex.");
  return request.auth.uid;
}
function validateCommandValue(action: string, value: string | null): void {
  switch (action) {
    case "LOCK_DEVICE":
    case "UNLOCK_DEVICE":
      if (value !== null) throw new HttpsError("invalid-argument", "Esta acción no admite contenido adicional.");
      return;
    case "GRANT_EXTRA_TIME":
    case "SET_DAILY_LIMIT": {
      const minutes = value === null ? NaN : Number(value.trim());
      if (!Number.isInteger(minutes) || minutes < 1 || minutes > 1440) throw new HttpsError("invalid-argument", "El valor debe ser un número entero entre 1 y 1440 minutos.");
      return;
    }
    case "SET_SCHEDULE": {
      const parts = value?.trim().split("-");
      const start = parts?.length === 2 ? Number(parts[0]) : NaN;
      const end = parts?.length === 2 ? Number(parts[1]) : NaN;
      if (!Number.isInteger(start) || !Number.isInteger(end) || start < 0 || start > 1439 || end < 0 || end > 1439) {
        throw new HttpsError("invalid-argument", "El horario debe usar minutos entre 0 y 1439.");
      }
      return;
    }
    case "BLOCK_APP":
    case "ALLOW_APP": {
      const packageName = value?.trim() ?? "";
      if (packageName.length < 1 || packageName.length > 255 || !/^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+$/.test(packageName)) {
        throw new HttpsError("invalid-argument", "El paquete de la aplicación no es válido.");
      }
      return;
    }
    case "SET_APP_LIMIT": {
      const parts = value?.split(":", 2);
      const packageName = parts?.[0]?.trim() ?? "";
      const minutes = parts?.length === 2 ? Number(parts[1].trim()) : NaN;
      if (packageName.length < 1 || packageName.length > 255 || !/^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+$/.test(packageName) || !Number.isInteger(minutes) || minutes < 1 || minutes > 1440) {
        throw new HttpsError("invalid-argument", "El límite de aplicación no es válido.");
      }
      return;
    }
    case "SYNC_POLICY":
      if (value === null || value.trim().length === 0) throw new HttpsError("invalid-argument", "SYNC_POLICY requiere un snapshot.");
      return;
    default:
      throw new HttpsError("invalid-argument", "Acción no permitida.");
  }
}

export const createPairingInvite = onCall(async (request) => {
  const parentUid = requireParent(request);
  const familyId = String(request.data?.familyId ?? "").trim();
  const childLabel = String(request.data?.childLabel ?? "Perfil infantil").trim().slice(0, 40) || "Perfil infantil";
  const famyrexMemberId = normalizeMemberId(request.data?.famyrexMemberId);
  if (!familyId) throw new HttpsError("invalid-argument", "Falta el identificador de familia.");
  const memberRef = db.doc(`families/${familyId}/members/${parentUid}`);
  const member = await memberRef.get();
  if (!member.exists || member.data()?.role !== "parent") throw new HttpsError("permission-denied", "No perteneces a esta familia como adulto.");
  const family = await db.doc(`families/${familyId}`).get();
  if (!family.exists) throw new HttpsError("not-found", "No existe la familia indicada.");
  const now = Date.now();
  const expiresAt = Timestamp.fromMillis(now + INVITE_TTL_MS);
  const inviteId = randomBytes(16).toString("hex");
  const token = randomBytes(32).toString("base64url");
  let code = ""; let codeHash = "";
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const candidate = randomInt(0, 1_000_000).toString().padStart(6, "0");
    const candidateHash = sha256(candidate);
    const existing = await db.collectionGroup("invites").where("codeHash", "==", candidateHash).where("status", "==", "active").limit(1).get();
    if (existing.empty) { code = candidate; codeHash = candidateHash; break; }
  }
  if (!code) throw new HttpsError("resource-exhausted", "No se pudo generar un código de vinculación único.");
  await db.doc(`families/${familyId}/invites/${inviteId}`).set({ codeHash, tokenHash: sha256(token), createdByUid: parentUid, famyrexMemberId, childLabel, createdAt: Timestamp.fromMillis(now), expiresAt, status: "active", usedAt: null, usedByUid: null });
  return { inviteId, code, token, famyrexMemberId, expiresAtMs: expiresAt.toMillis() };
});

export const redeemPairingCode = onCall(async (request) => {
  const deviceUid = requireAnonymousDevice(request);
  const code = normalizeCode(request.data?.code);
  const token = String(request.data?.token ?? "").trim();
  if (!token) throw new HttpsError("invalid-argument", "Falta el token de vinculación.");
  const childLabel = String(request.data?.childLabel ?? "Perfil infantil").trim().slice(0, 40) || "Perfil infantil";
  const requestedMemberId = request.data?.famyrexMemberId == null ? null : normalizeMemberId(request.data?.famyrexMemberId);
  const famyrexDeviceId = normalizeDeviceId(request.data?.famyrexDeviceId);
  const now = Date.now();
  const rateRef = db.doc(`pairingRateLimits/${deviceUid}`);
  await db.runTransaction(async (tx) => {
    const rateSnap = await tx.get(rateRef); const rate = rateSnap.exists ? rateSnap.data() ?? {} : {};
    const windowStartedAt = Number(rate.windowStartedAtMs ?? 0); const attempts = Number(rate.attempts ?? 0); const inWindow = now - windowStartedAt < RATE_WINDOW_MS;
    if (inWindow && attempts >= MAX_ATTEMPTS) throw new HttpsError("resource-exhausted", "Demasiados intentos. Espera unos minutos antes de volver a probar.");
    tx.set(rateRef, { windowStartedAtMs: inWindow ? windowStartedAt : now, attempts: inWindow ? attempts + 1 : 1, updatedAt: Timestamp.fromMillis(now) });
  });
  const matches = await db.collectionGroup("invites").where("codeHash", "==", sha256(code)).where("status", "==", "active").limit(1).get();
  if (matches.empty) throw new HttpsError("not-found", "Código no válido o caducado.");
  const inviteDoc = matches.docs[0]; const inviteData = inviteDoc.data(); const familyRef = inviteDoc.ref.parent.parent;
  if (!familyRef) throw new HttpsError("internal", "Invitación de familia inválida.");
  if (typeof inviteData.tokenHash !== "string" || inviteData.tokenHash !== sha256(token)) {
    throw new HttpsError("permission-denied", "El token de vinculación no coincide.");
  }
  const expiresAt = inviteData.expiresAt as Timestamp | undefined;
  if (!expiresAt || expiresAt.toMillis() <= now) { await inviteDoc.ref.update({ status: "expired" }); throw new HttpsError("deadline-exceeded", "El código de vinculación ha caducado."); }
  const invitedMemberId = normalizeMemberId(inviteData.famyrexMemberId);
  if (requestedMemberId != null && requestedMemberId !== invitedMemberId) throw new HttpsError("permission-denied", "El perfil infantil no coincide con la invitación.");
  const familyId = familyRef.id; const memberRef = familyRef.collection("members").doc(deviceUid); const deviceRef = familyRef.collection("devices").doc(deviceUid);
  await db.runTransaction(async (tx) => {
    const freshInvite = await tx.get(inviteDoc.ref); if (!freshInvite.exists || freshInvite.data()?.status !== "active") throw new HttpsError("already-exists", "Esta invitación ya ha sido utilizada.");
    const freshMember = await tx.get(memberRef);
    if (freshMember.exists) {
      if (freshMember.data()?.role === "child" && freshMember.data()?.famyrexDeviceId === famyrexDeviceId && freshMember.data()?.memberId === invitedMemberId) { tx.update(inviteDoc.ref, { status: "used", usedAt: Timestamp.fromMillis(now), usedByUid: deviceUid }); return; }
      throw new HttpsError("already-exists", "Este dispositivo ya pertenece a una familia.");
    }
    tx.set(memberRef, { uid: deviceUid, role: "child", memberId: invitedMemberId, displayName: inviteData.childLabel || childLabel || "Perfil infantil", createdAt: Timestamp.fromMillis(now), status: "active", deviceUid, famyrexDeviceId });
    tx.set(deviceRef, { uid: deviceUid, memberUid: deviceUid, role: "child", famyrexMemberId: invitedMemberId, famyrexDeviceId, createdAt: Timestamp.fromMillis(now), linkedAt: Timestamp.fromMillis(now) }, { merge: true });
    tx.update(inviteDoc.ref, { status: "used", usedAt: Timestamp.fromMillis(now), usedByUid: deviceUid });
  });
  return { familyId, childUid: deviceUid, famyrexMemberId: invitedMemberId, famyrexDeviceId, linkedAtMs: now };
});

export const registerDeviceToken = onCall(async (request) => {
  const deviceUid = requireAnonymousDevice(request);
  const familyId = String(request.data?.familyId ?? "").trim();
  const token = String(request.data?.token ?? "").trim();
  if (!familyId || !token || token.length > 4096) throw new HttpsError("invalid-argument", "Datos de dispositivo incompletos.");
  const memberRef = db.doc(`families/${familyId}/members/${deviceUid}`); const member = await memberRef.get();
  if (!member.exists || member.data()?.role !== "child" || member.data()?.deviceUid !== deviceUid || typeof member.data()?.famyrexDeviceId !== "string") throw new HttpsError("permission-denied", "El dispositivo no pertenece a esta familia.");
  const famyrexDeviceId = normalizeDeviceId(member.data()?.famyrexDeviceId);
  await db.doc(`families/${familyId}/devices/${deviceUid}`).set({ uid: deviceUid, memberUid: deviceUid, role: "child", famyrexMemberId: member.data()?.memberId, famyrexDeviceId, fcmToken: token, updatedAt: Timestamp.now() }, { merge: true });
  return { registered: true, famyrexDeviceId };
});

export const issueFamilyCommand = onCall(async (request) => {
  const parentUid = requireParent(request);
  const familyId = String(request.data?.familyId ?? "").trim(); const targetDeviceUid = String(request.data?.targetDeviceUid ?? "").trim(); const memberId = normalizeMemberId(request.data?.memberId);
  const commandId = String(request.data?.commandId ?? randomBytes(16).toString("hex")).trim(); const action = String(request.data?.action ?? "").trim(); const value = request.data?.value == null ? null : String(request.data.value);
  if (!familyId || !targetDeviceUid || !memberId || !commandId || !ALLOWED_COMMAND_ACTIONS.has(action)) throw new HttpsError("invalid-argument", "Comando incompleto o acción no permitida.");
  if (value !== null && value.length > MAX_COMMAND_VALUE_LENGTH) throw new HttpsError("invalid-argument", "El contenido del comando es demasiado grande.");
  validateCommandValue(action, value);
  const memberRef = db.doc(`families/${familyId}/members/${parentUid}`); const targetRef = db.doc(`families/${familyId}/members/${targetDeviceUid}`); const targetDeviceRef = db.doc(`families/${familyId}/devices/${targetDeviceUid}`);
  const [parent, target, device] = await Promise.all([memberRef.get(), targetRef.get(), targetDeviceRef.get()]);
  if (!parent.exists || parent.data()?.role !== "parent") throw new HttpsError("permission-denied", "No perteneces a esta familia como adulto.");
  if (!target.exists || target.data()?.role !== "child" || target.data()?.uid !== targetDeviceUid) throw new HttpsError("permission-denied", "El dispositivo destino no es un perfil infantil válido.");
  if (target.data()?.memberId !== memberId) throw new HttpsError("permission-denied", "El miembro destino no coincide.");
  if (!device.exists || device.data()?.uid !== targetDeviceUid || typeof device.data()?.fcmToken !== "string") throw new HttpsError("failed-precondition", "El dispositivo infantil no tiene un token de entrega registrado.");
  const famyrexDeviceId = normalizeDeviceId(device.data()?.famyrexDeviceId ?? target.data()?.famyrexDeviceId);
  const issuedAtMs = Date.now(); const expiresAtMs = issuedAtMs + COMMAND_TTL_MS;
  const command = { commandId, familyId, memberId, deviceId: famyrexDeviceId, action, issuedAtMs, expiresAtMs, value, requiresAdultConfirmation: true };
  const commandRef = db.doc(`families/${familyId}/commands/${commandId}`); const existing = await commandRef.get();
  if (existing.exists) throw new HttpsError("already-exists", "El identificador de comando ya existe.");
  await commandRef.create({ ...command, targetDeviceUid, createdByUid: parentUid, status: "sent", createdAt: Timestamp.fromMillis(issuedAtMs) });
  try {
    await messaging.send({ token: device.data()!.fcmToken, data: { famyrex_command: JSON.stringify(command) } });
  } catch (error: any) {
    // Keep the command recoverable: WorkManager only polls `sent` commands.
    console.warn("FCM delivery failed; keeping command pending for recovery", error?.code ?? "unknown");
    throw new HttpsError("unavailable", "No se pudo entregar el comando ahora; quedará pendiente para recuperación.");
  }
  return { commandId, expiresAtMs };
});

export const createParentInvite = onCall(async (request) => {
  const parentUid = requireParent(request); const familyId = String(request.data?.familyId ?? "").trim();
  if (!familyId) throw new HttpsError("invalid-argument", "Falta el identificador de familia.");
  const member = await db.doc(`families/${familyId}/members/${parentUid}`).get(); if (!member.exists || member.data()?.role !== "parent") throw new HttpsError("permission-denied", "No perteneces a esta familia como adulto.");
  const inviteId = randomBytes(16).toString("hex"); const token = randomBytes(32).toString("base64url"); const expiresAt = Timestamp.fromMillis(Date.now() + PARENT_INVITE_TTL_MS);
  await db.doc(`families/${familyId}/parentInvites/${inviteId}`).set({ tokenHash: sha256(token), createdByUid: parentUid, createdAt: Timestamp.now(), expiresAt, status: "active" });
  return { inviteId, token, expiresAtMs: expiresAt.toMillis() };
});

export const acceptParentInvite = onCall(async (request) => {
  const newParentUid = requireParent(request); const familyId = String(request.data?.familyId ?? "").trim(); const inviteId = String(request.data?.inviteId ?? "").trim(); const token = String(request.data?.token ?? "").trim(); const displayName = String(request.data?.displayName ?? "Adulto autorizado").trim().slice(0, 60) || "Adulto autorizado";
  if (!familyId || !inviteId || !token) throw new HttpsError("invalid-argument", "La invitación de adulto está incompleta.");
  const inviteRef = db.doc(`families/${familyId}/parentInvites/${inviteId}`); const invite = await inviteRef.get();
  if (!invite.exists || invite.data()?.status !== "active") throw new HttpsError("not-found", "La invitación de adulto no es válida.");
  const data = invite.data()!; const expiresAt = data.expiresAt as Timestamp | undefined;
  if (!expiresAt || expiresAt.toMillis() <= Date.now()) { await inviteRef.update({ status: "expired" }); throw new HttpsError("deadline-exceeded", "La invitación de adulto ha caducado."); }
  if (data.tokenHash !== sha256(token)) throw new HttpsError("permission-denied", "La invitación de adulto no coincide.");
  const family = await db.doc(`families/${familyId}`).get(); if (!family.exists) throw new HttpsError("not-found", "La familia no existe.");
  const memberRef = db.doc(`families/${familyId}/members/${newParentUid}`); const existing = await memberRef.get();
  if (existing.exists) { if (existing.data()?.role === "parent") return { familyId, uid: newParentUid, alreadyMember: true }; throw new HttpsError("already-exists", "Esta cuenta ya está vinculada a otro perfil de la familia."); }
  await db.runTransaction(async (tx) => {
    const freshInvite = await tx.get(inviteRef); if (!freshInvite.exists || freshInvite.data()?.status !== "active") throw new HttpsError("already-exists", "La invitación ya ha sido utilizada.");
    tx.set(memberRef, { uid: newParentUid, role: "parent", displayName, status: "active", createdAt: Timestamp.now() });
    tx.update(inviteRef, { status: "used", usedAt: Timestamp.now(), usedByUid: newParentUid });
  });
  return { familyId, uid: newParentUid, alreadyMember: false };
});
