package com.bipolarmood.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val mood: Double,
    val category: String,
    val symptoms: String,
    val note: String
)

@Entity(tableName = "impulse_entries")
data class ImpulseEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val description: String,
    val cost: Double?,
    val category: String,
    val autoScore: Int,
    val authorScore: Int,
    val authorComment: String,
    val trustedScore: Int?,
    val trustedComment: String
)

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val time: String,
    val frequency: String,
    val timeZone: String,
    val missedReminders: Int = 0,
    val lastTakenAt: Long? = null
)

@Entity(tableName = "medication_intakes")
data class MedicationIntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val scheduledAt: Long,
    val takenAt: Long?,
    val status: String
)

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val text: String,
    val photoUris: String
)

@Entity(tableName = "sleep_entries")
data class SleepEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val asleepAt: String,
    val wokeAt: String,
    val quality: Int,
    val mixedStateMarkers: String
)

@Entity(tableName = "trusted_people")
data class TrustedPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val avatarLabel: String,
    val accessToken: String,
    val revoked: Boolean = false
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Long = 1,
    val userName: String,
    val avatarLabel: String,
    val birthday: String,
    val darkTheme: Boolean,
    val soundEnabled: Boolean,
    val timeZone: String,
    val reminderIntervalMinutes: Int,
    val doctorContact: String,
    val crisisNumbers: String,
    val copingStrategies: String
)
