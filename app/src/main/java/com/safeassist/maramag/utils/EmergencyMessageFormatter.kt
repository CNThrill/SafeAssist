package com.safeassist.maramag.utils

import com.safeassist.maramag.model.EmergencyReport
import com.safeassist.maramag.model.LocationResult
import com.safeassist.maramag.model.MobilityProfile
import com.safeassist.maramag.model.UserProfile
import com.safeassist.maramag.model.WhoNeedsHelp

object EmergencyMessageFormatter {

    fun formatSosMessage(
        profile: UserProfile,
        caregiverMode: Boolean,
        locationResult: LocationResult?,
        includeGps: Boolean
    ): String {
        val sb = StringBuilder()
        sb.appendLine("EMERGENCY HELP NEEDED (SafeAssist)")

        val whoText = when {
            caregiverMode -> "Someone else"
            profile.fullName.isNotBlank() -> profile.fullName
            else -> "Me"
        }
        sb.appendLine("Who: $whoText")

        if (profile.contactNumber.isNotBlank()) {
            sb.appendLine("Callback #: ${profile.contactNumber}")
        }

        val availableGps = if (includeGps) locationResult as? LocationResult.Available else null
        if (caregiverMode) {
            sb.appendLine(if (availableGps != null) "Where: See GPS coordinates" else "Where: Location not specified")
        } else {
            sb.appendLine("Where: ${profile.formattedLocation()}")
        }

        if (availableGps != null) {
            sb.appendLine("GPS: ${availableGps.toCompactText()}")
        }

        if (!caregiverMode) {
            val needsList = buildList {
                if (profile.mobilityProfile != MobilityProfile.GENERAL) {
                    add(profile.mobilityProfile.displayName)
                }
                if (profile.assistanceNotes.isNotBlank()) {
                    add(profile.assistanceNotes)
                }
            }
            if (needsList.isNotEmpty()) {
                sb.appendLine("Needs: ${needsList.joinToString(" | ")}")
            }
        }

        return sb.toString().trim()
    }

    fun formatPreparedReport(
        report: EmergencyReport,
        profile: UserProfile
    ): String {
        val sb = StringBuilder()
        sb.appendLine("EMERGENCY REPORT (SafeAssist)")

        val whoLabel = buildString {
            append(report.whoNeedsHelp.label)
            if (report.whoNeedsHelp == WhoNeedsHelp.ME && profile.fullName.isNotBlank()) {
                append(" (${profile.fullName})")
            } else if (report.personName.isNotBlank()) {
                append(" (${report.personName})")
            }
        }
        sb.appendLine("Who: $whoLabel")

        if (report.whoNeedsHelp == WhoNeedsHelp.ME && profile.contactNumber.isNotBlank()) {
            sb.appendLine("Callback #: ${profile.contactNumber}")
        }

        val locationParts = listOfNotNull(
            report.barangay.takeIf { it.isNotBlank() },
            report.purok.takeIf { it.isNotBlank() },
            report.street.takeIf { it.isNotBlank() },
            report.landmark.takeIf { it.isNotBlank() }?.let { "Near $it" },
            report.householdDescription.takeIf { it.isNotBlank() }
        )
        val whereText = if (locationParts.isEmpty()) "Unknown / Not specified" else locationParts.joinToString(", ")
        sb.appendLine("Where: $whereText")

        sb.appendLine("What: ${report.incidentType.label}")
        sb.appendLine("When: ${DateTimeUtils.formatTimestamp(report.occurredAtMillis)}")

        val needsText = if (report.assistanceNeeds.isEmpty()) {
            "Not specified"
        } else {
            report.assistanceNeeds.joinToString(", ") { it.label }
        }
        sb.appendLine("Assistance Needs: $needsText")

        if (report.extraDetails.isNotBlank()) {
            sb.appendLine("Extra Details: ${report.extraDetails}")
        }

        return sb.toString().trim()
    }
}
