package com.bipolarmood.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun observeMoodEntries(): Flow<List<MoodEntryEntity>>

    @Query("SELECT COUNT(*) FROM mood_entries")
    suspend fun countMoodEntries(): Int

    @Insert
    suspend fun insertMoodEntry(entry: MoodEntryEntity)

    @Delete
    suspend fun deleteMoodEntry(entry: MoodEntryEntity)

    @Query("SELECT * FROM impulse_entries ORDER BY timestamp DESC")
    fun observeImpulseEntries(): Flow<List<ImpulseEntryEntity>>

    @Insert
    suspend fun insertImpulseEntry(entry: ImpulseEntryEntity)

    @Update
    suspend fun updateImpulseEntry(entry: ImpulseEntryEntity)

    @Delete
    suspend fun deleteImpulseEntry(entry: ImpulseEntryEntity)

    @Query("SELECT * FROM medications ORDER BY time ASC, name ASC")
    fun observeMedications(): Flow<List<MedicationEntity>>

    @Insert
    suspend fun insertMedication(entry: MedicationEntity): Long

    @Update
    suspend fun updateMedication(entry: MedicationEntity)

    @Delete
    suspend fun deleteMedication(entry: MedicationEntity)

    @Query("SELECT * FROM medication_intakes ORDER BY scheduledAt DESC")
    fun observeMedicationIntakes(): Flow<List<MedicationIntakeEntity>>

    @Insert
    suspend fun insertMedicationIntake(entry: MedicationIntakeEntity)

    @Query("SELECT * FROM diary_entries ORDER BY timestamp DESC")
    fun observeDiaryEntries(): Flow<List<DiaryEntryEntity>>

    @Insert
    suspend fun insertDiaryEntry(entry: DiaryEntryEntity)

    @Update
    suspend fun updateDiaryEntry(entry: DiaryEntryEntity)

    @Delete
    suspend fun deleteDiaryEntry(entry: DiaryEntryEntity)

    @Query("SELECT * FROM sleep_entries ORDER BY date DESC")
    fun observeSleepEntries(): Flow<List<SleepEntryEntity>>

    @Insert
    suspend fun insertSleepEntry(entry: SleepEntryEntity)

    @Query("SELECT * FROM trusted_people ORDER BY revoked ASC, name ASC")
    fun observeTrustedPeople(): Flow<List<TrustedPersonEntity>>

    @Insert
    suspend fun insertTrustedPerson(entry: TrustedPersonEntity)

    @Update
    suspend fun updateTrustedPerson(entry: TrustedPersonEntity)

    @Delete
    suspend fun deleteTrustedPerson(entry: TrustedPersonEntity)

    @Query("SELECT * FROM profile WHERE id = 1")
    fun observeProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)
}
