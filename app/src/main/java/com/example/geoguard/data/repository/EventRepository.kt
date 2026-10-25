package com.example.geoguard.data.repository

import com.example.geoguard.data.model.ZoneEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class EventRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore,
    private val userRepository: UserRepository = UserRepository(auth, db)
) {

    private fun events(groupId: String) =
        db.collection("groups").document(groupId).collection("events")

    /**
     * Writes groups/{groupId}/events/{deterministicId}. Creating this document is what
     * triggers the Cloud Function that notifies the other members.
     */
    suspend fun reportTransition(groupId: String, type: String, timestamp: Long): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("You must be logged in")
        val eventId = ZoneEvent.documentId(uid, type, timestamp)
        val event = ZoneEvent(
            id = eventId,
            userId = uid,
            userName = userRepository.getMyName(),
            type = type,
            timestamp = timestamp
        )
        events(groupId).document(eventId).set(event).await()
        Unit
    }

    fun observeRecentEvents(groupId: String, limit: Long = 20): Flow<List<ZoneEvent>> =
        events(groupId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(ZoneEvent::class.java) }
}