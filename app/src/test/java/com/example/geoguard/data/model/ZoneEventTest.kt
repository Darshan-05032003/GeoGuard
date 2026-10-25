package com.example.geoguard.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ZoneEventTest {

    @Test
    fun `document id is deterministic so worker retries never create duplicates`() {
        val first = ZoneEvent.documentId("user1", ZoneEvent.TYPE_EXIT, 1_700_000_000_000L)
        val retry = ZoneEvent.documentId("user1", ZoneEvent.TYPE_EXIT, 1_700_000_000_000L)
        assertEquals(first, retry)
    }

    @Test
    fun `document id differs for different user, type or time`() {
        val base = ZoneEvent.documentId("user1", ZoneEvent.TYPE_EXIT, 1L)
        assertNotEquals(base, ZoneEvent.documentId("user2", ZoneEvent.TYPE_EXIT, 1L))
        assertNotEquals(base, ZoneEvent.documentId("user1", ZoneEvent.TYPE_ENTER, 1L))
        assertNotEquals(base, ZoneEvent.documentId("user1", ZoneEvent.TYPE_EXIT, 2L))
    }
}