package com.example.geoguard.geofence

import com.example.geoguard.data.model.ZoneEvent
import com.google.android.gms.location.Geofence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeofenceTransitionMapperTest {

    @Test
    fun `exit transition maps to EXIT event`() {
        assertEquals(
            ZoneEvent.TYPE_EXIT,
            GeofenceTransitionMapper.toEventType(Geofence.GEOFENCE_TRANSITION_EXIT)
        )
    }

    @Test
    fun `enter transition maps to ENTER event`() {
        assertEquals(
            ZoneEvent.TYPE_ENTER,
            GeofenceTransitionMapper.toEventType(Geofence.GEOFENCE_TRANSITION_ENTER)
        )
    }

    @Test
    fun `dwell and unknown transitions are ignored`() {
        assertNull(GeofenceTransitionMapper.toEventType(Geofence.GEOFENCE_TRANSITION_DWELL))
        assertNull(GeofenceTransitionMapper.toEventType(-1))
        assertNull(GeofenceTransitionMapper.toEventType(999))
    }
}