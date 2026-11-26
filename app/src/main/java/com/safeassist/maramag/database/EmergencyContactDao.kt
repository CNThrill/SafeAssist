package com.safeassist.maramag.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.safeassist.maramag.model.EmergencyContact
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, name ASC")
    fun observeContacts(): Flow<List<EmergencyContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact): Long

    @Query("UPDATE emergency_contacts SET isPrimary = 0")
    suspend fun clearAllPrimary()

    @Query("UPDATE emergency_contacts SET isPrimary = 1 WHERE id = :contactId")
    suspend fun markPrimary(contactId: Long)

    @Transaction
    suspend fun pinPrimaryContact(contactId: Long) {
        clearAllPrimary()
        markPrimary(contactId)
    }

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)
}
