package com.example.geoguard.data.model

data class User(
    val id : String = "",
    val name: String = "",
    val email: String = "",
    val fcmToken: String? = null
)