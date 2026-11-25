package com.safeassist.maramag.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_reports")
data class EmergencyReport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val whoNeedsHelp: WhoNeedsHelp = WhoNeedsHelp.ME,
    val personName: String = "",
    val barangay: String = "",
    val purok: String = "",
    val street: String = "",
    val landmark: String = "",
    val householdDescription: String = "",
    val incidentType: IncidentType = IncidentType.MEDICAL,
    val assistanceNeeds: List<AssistanceNeed> = emptyList(),
    val extraDetails: String = "",
    val occurredAtMillis: Long = System.currentTimeMillis(),
    val createdAtMillis: Long = System.currentTimeMillis()
)
