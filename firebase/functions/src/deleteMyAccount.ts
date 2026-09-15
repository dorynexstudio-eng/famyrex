import { getAuth } from "firebase-admin/auth";
import { getFirestore, Query, QueryDocumentSnapshot } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";

function firestore() {
  return getFirestore();
}

async function deleteQueryResults(snapshots: QueryDocumentSnapshot[]): Promise<void> {
  if (snapshots.length === 0) return;
  const batch = firestore().batch();
  snapshots.forEach((snapshot) => batch.delete(snapshot.ref));
  await batch.commit();
}

async function deleteByQuery(query: Query): Promise<void> {
  while (true) {
    const snapshot = await query.limit(400).get();
    if (snapshot.empty) return;
    await deleteQueryResults(snapshot.docs);
    if (snapshot.size < 400) return;
  }
}

async function deleteCommandsByQuery(query: Query): Promise<void> {
  while (true) {
    const snapshot = await query.limit(400).get();
    if (snapshot.empty) return;
    for (const command of snapshot.docs) {
      await deleteByQuery(command.ref.collection("receipts"));
      await command.ref.delete();
    }
    if (snapshot.size < 400) return;
  }
}

export const deleteMyAccount = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Debes iniciar sesión para eliminar tu cuenta.");

  const db = firestore();
  const memberships = await db.collectionGroup("members").where("uid", "==", uid).get();

  for (const member of memberships.docs) {
    const familyRef = member.ref.parent.parent;
    if (!familyRef) continue;
    const familyId = familyRef.id;
    const role = member.data().role;

    await member.ref.delete();
    await db.doc(`families/${familyId}/devices/${uid}`).delete();
    await db.doc(`families/${familyId}/members/${uid}/location/latest`).delete();
    await deleteByQuery(db.collection(`families/${familyId}/invites`).where("createdByUid", "==", uid));
    await deleteCommandsByQuery(db.collection(`families/${familyId}/commands`).where("createdByUid", "==", uid));

    if (role === "child") {
      await deleteByQuery(db.collection(`families/${familyId}/alerts`).where("childUid", "==", uid));
      await deleteCommandsByQuery(db.collection(`families/${familyId}/commands`).where("targetDeviceUid", "==", uid));

      // Keep the family child profile reusable, but remove the deleted
      // anonymous device's identity so it cannot point at a deleted account.
      const childProfiles = await db.collection(`families/${familyId}/childProfiles`)
        .where("linkedUid", "==", uid)
        .get();
      for (const profile of childProfiles.docs) {
        await profile.ref.update({
          status: "pending",
          linkedAt: null,
          linkedUid: null,
          linkedDeviceId: null,
        });
      }
    }
  }

  // Adult invites are stored at the Firestore root, not inside the family.
  // Remove invites created by this account so account deletion does not leave
  // a live invitation that can later be used to add another adult.
  await deleteByQuery(db.collection("parentInvites").where("createdByUid", "==", uid));

  await db.doc(`pairingRateLimits/${uid}`).delete();
  await getAuth().deleteUser(uid);
  return { deleted: true };
});
