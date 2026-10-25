package com.example.geoguard.ui.groupdetail

import com.example.geoguard.data.model.ZoneEvent

enum class ZoneState { INSIDE, OUTSIDE, UNKNOWN }

data class MemberStatus(
    val userId: String,
    val name: String,
    val state: ZoneState,
    val since: Long? = null

)

object MemberStatusCalculator {

    /** Each member's status is decided by their most recent ENTER/EXIT event. */
    fun calculate(
        memberIds: List<String>,
        memberNames: Map<String, String>,
        events: List<ZoneEvent>
    ): List<MemberStatus> {
        val latestByUser: Map<String, ZoneEvent> = events
            .groupBy { it.userId }
            .mapValues { (_, userEvents) -> userEvents.maxBy { it.timestamp } }

        return memberIds.distinct().map { userId ->
            val state = when (latestByUser[userId]?.type) {
                ZoneEvent.TYPE_ENTER -> ZoneState.INSIDE
                ZoneEvent.TYPE_EXIT -> ZoneState.OUTSIDE
                else -> ZoneState.UNKNOWN
            }
            MemberStatus(
                userId = userId,
                name = memberNames[userId]?.takeIf { it.isNotBlank() } ?: "Member",
                state = state,
                since = latestByUser[userId]?.timestamp
            )
        }
    }
}