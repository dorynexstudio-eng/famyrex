import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";

const db = getFirestore();

function requireParent(request: any): string {
  if (!request.auth) throw new HttpsError("unauthenticated", "Debes iniciar sesión.");
  if (request.auth.token.firebase?.sign_in_provider !== "google.com") {
    throw new HttpsError("permission-denied", "Solo un adulto autenticado con Google puede recuperar la familia.");
  }
  return request.auth.uid;
}

function millis(value: unknown): number {
  return value instanceof Timestamp ? value.toMillis() : 0;
}

export const getSecureFamilySnapshot = onCall(async (request) => {
  const uid = requireParent(request);
  const familyId = String(request.data?.familyId ?? "").trim();
  if (!familyId) throw new HttpsError("invalid-argument", "Falta el identificador de familia.");

  const familyRef = db.doc(`families/${familyId}`);
  const parentRef = familyRef.collection("members").doc(uid);
  const [family, parent] = await Promise.all([familyRef.get(), parentRef.get()]);
  if (!family.exists) throw new HttpsError("not-found", "La familia no existe.");
  if (!parent.exists || parent.data()?.role !== "parent") {
    throw new HttpsError("permission-denied", "No perteneces a esta familia como adulto.");
  }

  const [members, childProfiles, devices] = await Promise.all([
    familyRef.collection("members").get(),
    familyRef.collection("childProfiles").get(),
    familyRef.collection("devices").get(),
  ]);

  const familyData = family.data() ?? {};
  const adults = members.docs
    .map((doc) => ({ doc, data: doc.data() }))
    .filter(({ data }) => data.role === "parent")
    .map(({ doc, data }) => ({
      uid: doc.id,
      role: "parent",
      displayName: String(data.displayName ?? "Adulto autorizado").trim().slice(0, 60) || "Adulto autorizado",
      status: String(data.status ?? "active"),
      createdAtMs: millis(data.createdAt),
    }));

  const children = childProfiles.docs.map((doc) => {
    const data = doc.data();
    return {
      memberId: String(data.memberId ?? doc.id),
      role: "child",
      displayName: String(data.displayName ?? "Perfil infantil").trim().slice(0, 60) || "Perfil infantil",
      ageRange: String(data.ageRange ?? "").trim().slice(0, 24),
      status: String(data.status ?? "pending"),
      linkedUid: typeof data.linkedUid === "string" ? data.linkedUid : null,
      linkedDeviceId: typeof data.linkedDeviceId === "string" ? data.linkedDeviceId : null,
      createdAtMs: millis(data.createdAt),
    };
  });

  const safeDevices = devices.docs.map((doc) => {
    const data = doc.data();
    return {
      uid: doc.id,
      role: String(data.role ?? "child"),
      famyrexMemberId: typeof data.famyrexMemberId === "string" ? data.famyrexMemberId : null,
      famyrexDeviceId: typeof data.famyrexDeviceId === "string" ? data.famyrexDeviceId : null,
      linkedAtMs: millis(data.linkedAt),
    };
  });

  return {
    familyId,
    ownerUid: String(familyData.ownerUid ?? ""),
    name: String(familyData.name ?? "Familia Famyrex").slice(0, 80),
    adults,
    children,
    devices: safeDevices,
  };
});
