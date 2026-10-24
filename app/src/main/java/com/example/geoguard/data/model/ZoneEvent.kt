package com.example.geoguard.data.model

data class ZoneEvent(
    val id : String = "",
    val userId : String = "",
    val userName : String = "",
    val type : String = TYPE_EXIT,
    val timestamp: Long = 0L
){
    companion object {
        const val TYPE_EXIT = "EXIT"
        const val TYPE_ENTER = "ENTER"
    }

}