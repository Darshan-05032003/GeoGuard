package com.example.geoguard.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore
) {
    /** Reads the signed-in user's display name from users/{uid}. Only the owner can read that document. */
    suspend fun getMyName(): String {
        val user = auth.currentUser ?: error("You must be logged in")
        val name = db.collection("users").document(user.uid).get().await().getString("name")
        return name?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore('@') ?: "A member"
    }
}