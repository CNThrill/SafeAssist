package com.safeassist.maramag.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val fullName: String = "",
    val contactNumber: String = "",
    val barangay: String = "",
    val purok: String = "",
    val streetOrHousehold: String = "",
    val landmark: String = "",
    val mobilityProfile: MobilityProfile = MobilityProfile.GENERAL,
    val assistanceNotes: String = ""
) {
    fun formattedLocation(): String {
        val parts = listOfNotNull(
            barangay.takeIf { it.isNotBlank() },
            purok.takeIf { it.isNotBlank() },
            streetOrHousehold.takeIf { it.isNotBlank() },
            landmark.takeIf { it.isNotBlank() }?.let { "Near $it" }
        )
        return if (parts.isEmpty()) "No saved address" else parts.joinToString(", ")
    }
}
