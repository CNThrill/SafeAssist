package com.safeassist.maramag.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.safeassist.maramag.model.EmergencyContact
import com.safeassist.maramag.model.EmergencyReport
import com.safeassist.maramag.model.UserProfile

@Database(
    entities = [
        UserProfile::class,
        EmergencyContact::class,
        EmergencyReport::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(SafeAssistConverters::class)
abstract class SafeAssistDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        @Volatile
        private var INSTANCE: SafeAssistDatabase? = null

        fun getInstance(context: Context): SafeAssistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeAssistDatabase::class.java,
                    "safeassist_local.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
