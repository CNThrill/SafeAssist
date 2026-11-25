package com.safeassist.maramag.model

data class AppSettings(
    val hasCompletedSetup: Boolean = false,
    val hasRequestedInitialPermissions: Boolean = false,
    val sirenEnabled: Boolean = false,
    val sirenUserOverridden: Boolean = false,
    val askGpsBeforeSending: Boolean = true,
    val caregiverModeEnabled: Boolean = false,
    val mdrrmoHotline: String = DEFAULT_MDRRMO_HOTLINE
) {
    companion object {
        const val DEFAULT_MDRRMO_HOTLINE = "0966-768-2770"
    }
}

sealed class LocationResult {
    data object Loading : LocationResult()
    data class Available(
        val latitude: Double,
        val longitude: Double
    ) : LocationResult() {
        fun toPlainText(): String {
            val latDir = if (latitude >= 0) "N" else "S"
            val lonDir = if (longitude >= 0) "E" else "W"
            return String.format(
                java.util.Locale.US,
                "%.4f° %s, %.4f° %s",
                kotlin.math.abs(latitude),
                latDir,
                kotlin.math.abs(longitude),
                lonDir
            )
        }

        fun toCompactText(): String {
            return String.format(java.util.Locale.US, "%.4f, %.4f", latitude, longitude)
        }
    }

    data class Unavailable(val reason: String) : LocationResult()
}
