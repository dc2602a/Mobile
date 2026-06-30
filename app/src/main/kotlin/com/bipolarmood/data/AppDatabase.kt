package com.bipolarmood.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MoodEntryEntity::class,
        ImpulseEntryEntity::class,
        MedicationEntity::class,
        MedicationIntakeEntity::class,
        DiaryEntryEntity::class,
        SleepEntryEntity::class,
        TrustedPersonEntity::class,
        ProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?:                 Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bipolar_mood.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
