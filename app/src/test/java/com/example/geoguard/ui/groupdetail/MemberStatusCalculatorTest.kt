package com.example.geoguard.ui.groupdetail

import com.example.geoguard.data.model.ZoneEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class MemberStatusCalculatorTest {

    private fun event(userId: String, type: String, time: Long) =
        ZoneEvent(id = "$userId-$time", userId = userId, userName = userId, type = type, timestamp = time)

    @Test
    fun `member with no events has unknown status`() {
        val result = MemberStatusCalculator.calculate(
            memberIds = listOf("a"),
            memberNames = mapOf("a" to "Asha"),
            events = emptyList()
        )
        assertEquals(listOf(MemberStatus("a", "Asha", ZoneState.UNKNOWN)), result)
    }

    @Test
    fun `latest event decides status regardless of list order`() {
        val events = listOf(
            event("a", ZoneEvent.TYPE_ENTER, 300),
            event("a", ZoneEvent.TYPE_EXIT, 100),
            event("b", ZoneEvent.TYPE_ENTER, 100),
            event("b", ZoneEvent.TYPE_EXIT, 200)
        )
        val result = MemberStatusCalculator.calculate(
            memberIds = listOf("a", "b"),
            memberNames = mapOf("a" to "Asha", "b" to "Bala"),
            events = events
        )
        assertEquals(ZoneState.INSIDE, result.first { it.userId == "a" }.state)
        assertEquals(ZoneState.OUTSIDE, result.first { it.userId == "b" }.state)
    }

    @Test
    fun `events from users who are not members are ignored`() {
        val result = MemberStatusCalculator.calculate(
            memberIds = listOf("a"),
            memberNames = mapOf("a" to "Asha"),
            events = listOf(event("stranger", ZoneEvent.TYPE_EXIT, 1))
        )
        assertEquals(1, result.size)
        assertEquals(ZoneState.UNKNOWN, result.single().state)
    }

    @Test
    fun `missing name falls back to a readable label`() {
        val result = MemberStatusCalculator.calculate(
            memberIds = listOf("a"),
            memberNames = emptyMap(),
            events = emptyList()
        )
        assertEquals("Member", result.single().name)
    }

    @Test
    fun `members keep the order of memberIds`() {
        val result = MemberStatusCalculator.calculate(
            memberIds = listOf("c", "a", "b"),
            memberNames = emptyMap(),
            events = emptyList()
        )
        assertEquals(listOf("c", "a", "b"), result.map { it.userId })
    }

    @Test
    fun `duplicate member ids are shown once (a crafted join must not crash the list)`() {
        val result = MemberStatusCalculator.calculate(
            memberIds = listOf("a", "b", "b"),
            memberNames = emptyMap(),
            events = emptyList()
        )
        assertEquals(listOf("a", "b"), result.map { it.userId })
    }
    @Test
    fun `status carries the time of the latest event, or null when there is none`() {
        val events = listOf(
            event("a", ZoneEvent.TYPE_EXIT, 100),
            event("a", ZoneEvent.TYPE_ENTER, 500)
        )
        val result = MemberStatusCalculator.calculate(
            memberIds = listOf("a", "b"),
            memberNames = emptyMap(),
            events = events
        )
        assertEquals(500L, result.first { it.userId == "a" }.since)
        assertEquals(null, result.first { it.userId == "b" }.since)
    }
}