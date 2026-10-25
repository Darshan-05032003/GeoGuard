package com.example.geoguard.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    // Display names stored on the group so members never need to read each other's user documents.
    val memberNames: Map<String, String> = emptyMap(),
    val zone: SafeZone = SafeZone(),
)