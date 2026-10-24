package com.example.geoguard.data.repository

import com.example.geoguard.data.model.Group
import com.example.geoguard.data.model.SafeZone
import com.example.geoguard.util.InviteCodeGenerator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GroupRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore
) {

    private val groups = db.collection("groups")

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: error("You must be logged in")

    suspend fun createGroup(name: String, zone: SafeZone): Result<Group> = runCatching {
        val uid = requireUserId()
        val docRef = groups.document()
        val group = Group(
            id = docRef.id,
            name = name.trim(),
            inviteCode = InviteCodeGenerator.generate(),
            ownerId = uid,
            memberIds = listOf(uid),
            zone = zone
        )
        docRef.set(group).await()
        group
    }

    suspend fun joinGroup(rawCode: String): Result<Group> = runCatching {
        val uid = requireUserId()
        val code = InviteCodeGenerator.normalize(rawCode)
        require(InviteCodeGenerator.isValid(code)) { "Invite code must be 6 letters or digits" }

        val snapshot = groups
            .whereEqualTo("inviteCode", code)
            .limit(1)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull() ?: error("No group found with code $code")
        val group = doc.toObject(Group::class.java) ?: error("Could not read group data")

        if (uid !in group.memberIds) {
            doc.reference.update("memberIds", FieldValue.arrayUnion(uid)).await()
        }
        group.copy(id = doc.id, memberIds = (group.memberIds + uid).distinct())
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

    fun observeGroup(groupId: String): Flow<Group?> =
        groups.document(groupId)
            .snapshots()
            .map { doc -> doc.toObject(Group::class.java)?.copy(id = doc.id) }
}