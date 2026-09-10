import { getAuth } from "firebase-admin/auth";
import { getFirestore, QueryDocumentSnapshot } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";

const db = getFirestore();
const auth = getAuth();

async function deleteQueryResults(
  snapshots: QueryDocumentSnapshot[],
): Promise<void> {
  if (snapshots.length === 0) return;
  const batch = db.batch();
  snapshots.forEach((snapshot) => batch.delete(snapshot.ref));
  await batch.commit();
}

async function deleteByQuery(
  query: FirebaseFirestore.Query,
): Promise<void> {
  while (true) {
    const snapshot = await query.limit(400).get();
    if (snapshot.empty) return;
    await deleteQueryResults(snapshot.docs);
    if (snapshot.size < 400) return;
  }
}

export const deleteMyAccount = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Debes iniciar sesión para eliminar tu cuenta.");

  const memberships = await db.collectionGroup("members").where("uid", "==", uid).get();

  for (const member of memberships.docs) {
    const familyRef = member.ref.parent.parent;
    if (!familyRef) continue;
    const familyId = familyRef.id;
    const data = member.data();
    const role = data.role;

    await member.ref.delete();
    await db.doc(`families/${familyId}/devices/${uid}`).delete();
    await db.doc(`families/${familyId}/members/${uid}/location/latest`).delete();

    await deleteByQuery(
      db.collection(`families/${familyId}/invites`).where("createdByUid", "==", uid),
    );
    await deleteByQuery(
      db.collection(`families/${familyId}/parentInvites`).where("createdByUid", "==", uid),
    );
    await deleteByQuery(
      db.collection(`families/${familyId}/commands`).where("createdByUid", "==", uid),
    );

    if (role === "child") {
      await deleteByQuery(
        db.collection(`families/${familyId}/commands`).where("targetDeviceUid", "==", uid),
      );
    }
  }

  await db.doc(`pairingRateLimits/${uid}`).delete();
  await auth.deleteUser(uid);

  return { deleted: true };
});
