package com.example.geoguard.ui.creategroup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateGroupValidatorTest {

    @Test
    fun `blank name is rejected`() {
        assertEquals(
            CreateGroupValidator.ERROR_NAME_BLANK,
            CreateGroupValidator.validate(name = "   ", latitude = 20.0, longitude = 74.0, radiusMeters = 200f)
        )
    }

    @Test
    fun `name longer than the limit is rejected`() {
        val longName = "x".repeat(CreateGroupValidator.MAX_NAME_LENGTH + 1)
        assertEquals(
            CreateGroupValidator.ERROR_NAME_TOO_LONG,
            CreateGroupValidator.validate(longName, 20.0, 74.0, 200f)
        )
    }

    @Test
    fun `missing location is rejected`() {
        assertEquals(
            CreateGroupValidator.ERROR_NO_LOCATION,
            CreateGroupValidator.validate("Home", null, 74.0, 200f)
        )
        assertEquals(
            CreateGroupValidator.ERROR_NO_LOCATION,
            CreateGroupValidator.validate("Home", 20.0, null, 200f)
        )
    }

    @Test
    fun `out of range coordinates are rejected`() {
        assertEquals(
            CreateGroupValidator.ERROR_BAD_LOCATION,
            CreateGroupValidator.validate("Home", 91.0, 74.0, 200f)
        )
        assertEquals(
            CreateGroupValidator.ERROR_BAD_LOCATION,
            CreateGroupValidator.validate("Home", 20.0, -181.0, 200f)
        )
    }

    @Test
    fun `radius outside the allowed range is rejected`() {
        assertEquals(
            CreateGroupValidator.ERROR_BAD_RADIUS,
            CreateGroupValidator.validate("Home", 20.0, 74.0, MIN_RADIUS_METERS - 1)
        )
        assertEquals(
            CreateGroupValidator.ERROR_BAD_RADIUS,
            CreateGroupValidator.validate("Home", 20.0, 74.0, MAX_RADIUS_METERS + 1)
        )
    }

    @Test
    fun `valid input returns no error`() {
        assertNull(CreateGroupValidator.validate("Home", 20.9, 74.77, 200f))
        assertNull(CreateGroupValidator.validate("Home", 20.9, 74.77, MIN_RADIUS_METERS))
        assertNull(CreateGroupValidator.validate("Home", 20.9, 74.77, MAX_RADIUS_METERS))
    }
}