package com.famyrex.app

data class FamilyZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 150f,
    val enabled: Boolean = true
)

enum class LocationPermissionState {
    DENIED,
    APPROXIMATE,
    PRECISE,
    BACKGROUND_READY
}
