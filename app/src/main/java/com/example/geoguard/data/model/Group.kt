package com.example.geoguard.data.model

data class Group(
    val id : String = "",
    val name : String = "",
    val inviteCode : String ="",
    val ownerId : String = "",
    val  memberIds : List<String> = emptyList(),
    val zone : SafeZone = SafeZone(),
)