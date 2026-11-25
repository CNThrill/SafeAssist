package com.safeassist.maramag.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relationshipOrRole: String,
    val phoneNumber: String,
    val isPrimary: Boolean = false
)
