package com.example.geoguard.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Push tokens live in fcmTokens/{uid}, a collection only the owner (and the Cloud Function,
 * via the Admin SDK) can read. Keeping them out of users/{uid} means no other user can ever see them.
 */
class TokenRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore
) {

    suspend fun syncCurrentToken(): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        saveToken(token).getOrThrow()
    }

    suspend fun saveToken(token: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        db.collection("fcmTokens").document(uid).set(
            mapOf(
                "token" to token,
                "platform" to "android",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    /** Called on logout so this phone stops receiving alerts for the old account. */
    suspend fun clearToken(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        db.collection("fcmTokens").document(uid).delete().await()
        Unit
    }
}