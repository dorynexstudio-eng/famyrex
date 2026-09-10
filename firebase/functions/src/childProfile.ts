import { randomBytes } from "node:crypto";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";

const db = getFirestore();
const AGE_RANGES = new Set(["under_6", "6_9", "10_12", "13_15", "16_plus"]);

function requireGoogleAdult(request: any): string {
  if (!request.auth) throw new HttpsError("unauthenticated", "Debes iniciar sesión con Google.");
  if (request.auth.token.firebase?.sign_in_provider !== "google.com") {
    throw new HttpsError("permission-denied", "Solo un adulto autenticado con Google puede crear perfiles infantiles.");
  }
  return request.auth.uid;
}

export const createPendingChildProfile = onCall(async (request) => {
  const parentUid = requireGoogleAdult(request);
  const familyId = String(request.data?.familyId ?? "").trim();
  const displayName = String(request.data?.displayName ?? "").trim().slice(0, 60);
  const ageRange = String(request.data?.ageRange ?? "").trim();

  if (!familyId) throw new HttpsError("invalid-argument", "Falta el identificador de familia.");
  if (displayName.length < 1) throw new HttpsError("invalid-argument", "El nombre del menor es obligatorio.");
  if (!AGE_RANGES.has(ageRange)) throw new HttpsError("invalid-argument", "El tramo de edad no es válido.");

  const familyRef = db.doc(`families/${familyId}`);
  const parentRef = familyRef.collection("members").doc(parentUid);
  const [family, parent] = await Promise.all([familyRef.get(), parentRef.get()]);
  if (!family.exists) throw new HttpsError("not-found", "La familia no existe.");
  if (!parent.exists || parent.data()?.role !== "parent") {
    throw new HttpsError("permission-denied", "No perteneces a esta familia como adulto.");
  }

  const memberId = randomBytes(16).toString("hex");
  const now = Timestamp.now();
  const memberRef = familyRef.collection("childProfiles").doc(memberId);
  await memberRef.create({
    memberId,
    role: "child",
    displayName,
    ageRange,
    status: "pending",
    createdByUid: parentUid,
    createdAt: now,
    linkedAt: null,
  });

  return { familyId, memberId, displayName, ageRange, status: "pending" };
});
