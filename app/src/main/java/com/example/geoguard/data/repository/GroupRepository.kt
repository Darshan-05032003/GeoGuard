package com.example.geoguard.data.repository

import com.example.geoguard.data.model.Group
import com.example.geoguard.data.model.SafeZone
import com.example.geoguard.util.InviteCodeGenerator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GroupRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore,
    private val userRepository: UserRepository = UserRepository(auth, db)
) {

    private val groups = db.collection("groups")
    private val inviteCodes = db.collection("inviteCodes")

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: error("You must be logged in")

    /**
     * Writes the group and its invite-code lookup document in one atomic batch.
     * Security rules refuse to overwrite an existing invite code, so a (very unlikely)
     * code collision fails the batch and we simply retry with a new code.
     */
    suspend fun createGroup(name: String, zone: SafeZone): Result<Group> = runCatching {
        val uid = requireUserId()
        val myName = userRepository.getMyName()

        var lastError: Exception? = null
        repeat(MAX_CODE_ATTEMPTS) {
            val docRef = groups.document()
            val group = Group(
                id = docRef.id,
                name = name.trim(),
                inviteCode = InviteCodeGenerator.generate(),
                ownerId = uid,
                memberIds = listOf(uid),
                memberNames = mapOf(uid to myName),
                zone = zone
            )
            try {
                db.batch()
                    .set(docRef, group)
                    .set(inviteCodes.document(group.inviteCode), mapOf("groupId" to group.id))
                    .commit()
                    .await()
                return@runCatching group
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Could not create group")
    }

    /**
     * Join flow without letting anyone list groups:
     * 1. Look up inviteCodes/{CODE} (single-document get; listing is blocked by rules).
     * 2. Add myself to memberIds/memberNames (rules only allow adding *yourself*).
     * 3. Read the group, which is now allowed because I'm a member.
     */
    suspend fun joinGroup(rawCode: String): Result<Group> = runCatching {
        val uid = requireUserId()
        val code = InviteCodeGenerator.normalize(rawCode)
        require(InviteCodeGenerator.isValid(code)) { "Invite code must be 6 letters or digits" }

        val codeDoc = inviteCodes.document(code).get().await()
        val groupId = codeDoc.getString("groupId") ?: error("No group found with code $code")
        val myName = userRepository.getMyName()

        val groupRef = groups.document(groupId)
        groupRef.update(
            mapOf(
                "memberIds" to FieldValue.arrayUnion(uid),
                "memberNames.$uid" to myName
            )
        ).await()

        val snapshot = groupRef.get().await()
        snapshot.toObject(Group::class.java)?.copy(id = snapshot.id) ?: error("Could not read group data")
    }

    fun observeMyGroups(): Flow<List<Group>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return groups
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Group::class.java)?.copy(id = doc.id)
                }
            }
    }

    /** One-shot read used by background work (e.g. re-registering geofences after a reboot). */
    suspend fun getMyGroupsOnce(): List<Group> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return groups.whereArrayContains("memberIds", uid).get().await()
            .documents.mapNotNull { doc -> doc.toObject(Group::class.java)?.copy(id = doc.id) }
    }

    fun observeGroup(groupId: String): Flow<Group?> =
        groups.document(groupId)
            .snapshots()
            .map { doc -> doc.toObject(Group::class.java)?.copy(id = doc.id) }

    private companion object {
        const val MAX_CODE_ATTEMPTS = 3
    }
}