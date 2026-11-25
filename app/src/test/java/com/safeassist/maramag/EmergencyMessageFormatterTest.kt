package com.safeassist.maramag

import com.safeassist.maramag.model.AssistanceNeed
import com.safeassist.maramag.model.EmergencyReport
import com.safeassist.maramag.model.IncidentType
import com.safeassist.maramag.model.LocationResult
import com.safeassist.maramag.model.MobilityProfile
import com.safeassist.maramag.model.UserProfile
import com.safeassist.maramag.model.WhoNeedsHelp
import com.safeassist.maramag.utils.EmergencyMessageFormatter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyMessageFormatterTest {

    @Test
    fun sosMessage_includesSavedFallbackAndOmitsGpsWhenExcluded() {
        val profile = UserProfile(
            fullName = "Juan Dela Cruz",
            barangay = "Brgy. Poblacion",
            purok = "Purok 4",
            landmark = "Barangay Chapel",
            mobilityProfile = MobilityProfile.MOBILITY,
            assistanceNotes = "Uses crutches; safe transfer"
        )
        val location = LocationResult.Available(7.7685, 125.0048)

        val message = EmergencyMessageFormatter.formatSosMessage(
            profile = profile,
            caregiverMode = false,
            locationResult = location,
            includeGps = false
        )

        assertTrue(message.contains("Juan Dela Cruz"))
        assertTrue(message.contains("Brgy. Poblacion, Purok 4, Near Barangay Chapel"))
        assertTrue(message.contains("Uses crutches; safe transfer"))
        assertFalse(message.contains("7.7685"))
    }

    @Test
    fun sosMessage_includesPlainTextGpsWhenExplicitlyIncluded() {
        val profile = UserProfile(barangay = "Brgy. Poblacion")
        val location = LocationResult.Available(7.7685, 125.0048)

        val message = EmergencyMessageFormatter.formatSosMessage(
            profile = profile,
            caregiverMode = false,
            locationResult = location,
            includeGps = true
        )

        assertTrue(message.contains("GPS: 7.7685, 125.0048"))
        assertFalse(message.contains("http"))
    }

    @Test
    fun preparedReport_handlesUnknownAndOptionalFieldsGracefully() {
        val report = EmergencyReport(
            whoNeedsHelp = WhoNeedsHelp.SOMEONE_ELSE,
            personName = "",
            barangay = "",
            incidentType = IncidentType.FLOOD,
            assistanceNeeds = listOf(AssistanceNeed.RESCUE, AssistanceNeed.EVACUATION),
            extraDetails = ""
        )

        val formatted = EmergencyMessageFormatter.formatPreparedReport(report, UserProfile())

        assertTrue(formatted.contains("Who: Someone else"))
        assertTrue(formatted.contains("Where: Unknown / Not specified"))
        assertTrue(formatted.contains("What: Flood"))
        assertTrue(formatted.contains("Rescue, Evacuation"))
    }

    @Test
    fun sosMessage_usesSimplifiedUserPovLabelsWithoutSystemJargon() {
        val selfMessage = EmergencyMessageFormatter.formatSosMessage(
            profile = UserProfile(),
            caregiverMode = false,
            locationResult = null,
            includeGps = false
        )
        assertTrue(selfMessage.contains("Who: Me"))
        assertTrue(selfMessage.contains("Where: No saved address"))
        assertFalse(selfMessage.contains("Saved Profile"))

        val caregiverMessage = EmergencyMessageFormatter.formatSosMessage(
            profile = UserProfile(
                fullName = "Juan Dela Cruz",
                contactNumber = "09171234567",
                barangay = "Brgy. Poblacion",
                mobilityProfile = MobilityProfile.MOBILITY,
                assistanceNotes = "Uses crutches"
            ),
            caregiverMode = true,
            locationResult = null,
            includeGps = false
        )
        assertTrue(caregiverMessage.contains("Who: Someone else"))
        assertTrue(caregiverMessage.contains("Callback #: 09171234567"))
        assertTrue(caregiverMessage.contains("Where: Location not specified"))
        assertFalse(caregiverMessage.contains("Brgy. Poblacion"))
        assertFalse(caregiverMessage.contains("Uses crutches"))
        assertFalse(caregiverMessage.contains("Caregiver Mode"))
    }

    @Test
    fun preparedReport_includesCallbackNumberOnlyWhenSelfSelected() {
        val profile = UserProfile(fullName = "Maria Santos", contactNumber = "09180001111")

        val selfReport = EmergencyMessageFormatter.formatPreparedReport(
            report = EmergencyReport(whoNeedsHelp = WhoNeedsHelp.ME),
            profile = profile
        )
        assertTrue(selfReport.contains("Who: Me (Maria Santos)"))
        assertTrue(selfReport.contains("Callback #: 09180001111"))

        val otherReport = EmergencyMessageFormatter.formatPreparedReport(
            report = EmergencyReport(whoNeedsHelp = WhoNeedsHelp.SOMEONE_ELSE, personName = "Pedro"),
            profile = profile
        )
        assertTrue(otherReport.contains("Who: Someone else (Pedro)"))
        assertFalse(otherReport.contains("09180001111"))
    }
}
