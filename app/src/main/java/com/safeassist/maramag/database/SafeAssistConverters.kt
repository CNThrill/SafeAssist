package com.safeassist.maramag.database

import androidx.room.TypeConverter
import com.safeassist.maramag.model.AssistanceNeed
import com.safeassist.maramag.model.IncidentType
import com.safeassist.maramag.model.MobilityProfile
import com.safeassist.maramag.model.WhoNeedsHelp

class SafeAssistConverters {

    @TypeConverter
    fun fromMobilityProfile(value: MobilityProfile): String = value.name

    @TypeConverter
    fun toMobilityProfile(value: String): MobilityProfile = MobilityProfile.fromName(value)

    @TypeConverter
    fun fromWhoNeedsHelp(value: WhoNeedsHelp): String = value.name

    @TypeConverter
    fun toWhoNeedsHelp(value: String): WhoNeedsHelp = WhoNeedsHelp.fromName(value)

    @TypeConverter
    fun fromIncidentType(value: IncidentType): String = value.name

    @TypeConverter
    fun toIncidentType(value: String): IncidentType = IncidentType.fromName(value)

    @TypeConverter
    fun fromAssistanceNeeds(needs: List<AssistanceNeed>): String {
        return needs.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toAssistanceNeeds(data: String): List<AssistanceNeed> {
        if (data.isBlank()) return emptyList()
        return data.split(",").mapNotNull { AssistanceNeed.fromName(it.trim()) }
    }
}
