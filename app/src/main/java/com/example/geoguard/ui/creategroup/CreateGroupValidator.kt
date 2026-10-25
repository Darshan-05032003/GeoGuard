package com.example.geoguard.ui.creategroup

object CreateGroupValidator {

    const val MAX_NAME_LENGTH = 40

    const val ERROR_NAME_BLANK = "Enter a group name"
    const val ERROR_NAME_TOO_LONG = "Group name must be at most $MAX_NAME_LENGTH characters"
    const val ERROR_NO_LOCATION = "Set the zone location first"
    const val ERROR_BAD_LOCATION = "That location doesn't look valid. Try again."
    const val ERROR_BAD_RADIUS = "Radius must be between 150 m and 1000 m"

    /** Returns an error message, or null when the input is valid. */
    fun validate(name: String, latitude: Double?, longitude: Double?, radiusMeters: Float): String? = when {
        name.isBlank() -> ERROR_NAME_BLANK
        name.trim().length > MAX_NAME_LENGTH -> ERROR_NAME_TOO_LONG
        latitude == null || longitude == null -> ERROR_NO_LOCATION
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0 -> ERROR_BAD_LOCATION
        radiusMeters !in MIN_RADIUS_METERS..MAX_RADIUS_METERS -> ERROR_BAD_RADIUS
        else -> null
    }
}