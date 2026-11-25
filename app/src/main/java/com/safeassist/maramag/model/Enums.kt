package com.safeassist.maramag.model

enum class MobilityProfile(val displayName: String, val defaultSirenEnabled: Boolean) {
    MOBILITY("Needs mobility support", true),
    SENIOR("Senior / health support", false),
    GENERAL("No special mobility help", false),
    OTHER("Other assistance", false);

    companion object {
        fun fromName(name: String?): MobilityProfile {
            return entries.firstOrNull { it.name == name } ?: GENERAL
        }
    }
}

enum class WhoNeedsHelp(val label: String) {
    ME("Me"),
    FAMILY_MEMBER("Family"),
    SOMEONE_ELSE("Someone else");

    companion object {
        fun fromName(name: String?): WhoNeedsHelp {
            return entries.firstOrNull { it.name == name } ?: ME
        }
    }
}

enum class IncidentType(val label: String) {
    MEDICAL("Medical emergency"),
    FLOOD("Flood"),
    FIRE("Fire"),
    LANDSLIDE("Landslide"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): IncidentType {
            return entries.firstOrNull { it.name == name } ?: MEDICAL
        }
    }
}

enum class AssistanceNeed(val label: String) {
    MEDICAL_CARE("Medical care"),
    RESCUE("Rescue"),
    EVACUATION("Evacuation"),
    FOOD_AND_WATER("Food & water"),
    MOBILITY_SUPPORT("Mobility support");

    companion object {
        fun fromName(name: String?): AssistanceNeed? {
            return entries.firstOrNull { it.name == name }
        }
    }
}
