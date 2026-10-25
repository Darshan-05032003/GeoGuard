package com.example.geoguard.data.model

data class ZoneEvent(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val type: String = TYPE_EXIT,
    val timestamp: Long = 0L
) {
    companion object {
        const val TYPE_EXIT = "EXIT"
        const val TYPE_ENTER = "ENTER"

        /**
         * Deterministic id: if the upload worker retries after a write actually succeeded,
         * it overwrites the same document instead of creating a duplicate event (and a duplicate push).
         */
        fun documentId(userId: String, type: String, timestamp: Long): String =
            "${userId}_${type}_$timestamp"
    }
}