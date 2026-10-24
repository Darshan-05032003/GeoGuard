package com.example.geoguard.data.repository

import com.example.geoguard.data.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await



class AuthRepository(
    private val auth : FirebaseAuth = Firebase.auth,
    private val db : FirebaseFirestore = Firebase.firestore, ){

    val currentUserId: String?
        get() = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun register(name: String, email:String, password: String ):Result<User> = runCatching {
        val authResult = auth.createUserWithEmailAndPassword(email,password).await()
        val uid = authResult.user?.uid?:error("Registration failed : no user returned")
        val user = User(id = uid , name = name ,email= email)
        db.collection("users").document(uid).set(user).await()
        user

    }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }
    fun logout() = auth.signOut()
}