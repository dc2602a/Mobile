package com.bipolarmood

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bipolarmood.data.AppDatabase
import com.bipolarmood.data.DiaryEntryEntity
import com.bipolarmood.data.ImpulseEntryEntity
import com.bipolarmood.data.MedicationEntity
import com.bipolarmood.data.MedicationIntakeEntity
import com.bipolarmood.data.MoodEntryEntity
import com.bipolarmood.data.ProfileEntity
import com.bipolarmood.data.SleepEntryEntity
import com.bipolarmood.data.TrustedPersonEntity
import com.bipolarmood.notifications.MedicationReminderWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).dao()

    val moodEntries = dao.observeMoodEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val impulseEntries = dao.observeImpulseEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val medications = dao.observeMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val medicationIntakes = dao.observeMedicationIntakes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val diaryEntries = dao.observeDiaryEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sleepEntries = dao.observeSleepEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trustedPeople = dao.observeTrustedPeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profile = dao.observeProfile()
        .map { it ?: defaultProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), defaultProfile())

    init {
        viewModelScope.launch {
            if (dao.getProfile() == null) {
                dao.upsertProfile(defaultProfile())
            }
            if (dao.countMoodEntries() == 0) {
                seedFirstRunData()
            }
        }
    }

    fun addMoodEntry(mood: Double, category: String, symptoms: List<String>, note: String, timestamp: Long = now()) {
        viewModelScope.launch {
            dao.insertMoodEntry(
                MoodEntryEntity(
                    timestamp = timestamp,
                    mood = mood,
                    category = category,
                    symptoms = symptoms.joinToString("|"),
                    note = note.trim()
                )
            )
        }
    }

    fun deleteMoodEntry(entry: MoodEntryEntity) {
        viewModelScope.launch { dao.deleteMoodEntry(entry) }
    }

    fun addImpulse(description: String, cost: Double?, category: String, authorScore: Int, comment: String) {
        viewModelScope.launch {
            val autoScore = calculateImpulseScore(description, cost, category)
            dao.insertImpulseEntry(
                ImpulseEntryEntity(
                    timestamp = now(),
                    description = description.trim(),
                    cost = cost,
                    category = category,
                    autoScore = autoScore,
                    authorScore = authorScore.coerceIn(1, 10),
                    authorComment = comment.trim(),
                    trustedScore = null,
                    trustedComment = ""
                )
            )
        }
    }

    fun addTrustedImpulseScore(entry: ImpulseEntryEntity, score: Int, comment: String) {
        viewModelScope.launch {
            dao.updateImpulseEntry(
                entry.copy(
                    trustedScore = score.coerceIn(1, 10),
                    trustedComment = comment.trim()
                )
            )
        }
    }

    fun deleteImpulse(entry: ImpulseEntryEntity) {
        viewModelScope.launch { dao.deleteImpulseEntry(entry) }
    }

    fun addMedication(name: String, dosage: String, time: String, frequency: String, timeZone: String) {
        viewModelScope.launch {
            val medicationId = dao.insertMedication(
                MedicationEntity(
                    name = name.trim(),
                    dosage = dosage.trim(),
                    time = time.trim(),
                    frequency = frequency,
                    timeZone = timeZone.ifBlank { TimeZone.getDefault().id }
                )
            )
            scheduleMedicationReminder(medicationId, name.trim(), dosage.trim())
        }
    }

    fun markMedicationTaken(medication: MedicationEntity) {
        viewModelScope.launch {
            val takenAt = now()
            dao.updateMedication(medication.copy(missedReminders = 0, lastTakenAt = takenAt))
            dao.insertMedicationIntake(
                MedicationIntakeEntity(
                    medicationId = medication.id,
                    medicationName = medication.name,
                    scheduledAt = takenAt,
                    takenAt = takenAt,
                    status = "taken"
                )
            )
        }
    }

    fun registerMissedMedication(medication: MedicationEntity) {
        viewModelScope.launch {
            val missed = medication.missedReminders + 1
            dao.updateMedication(medication.copy(missedReminders = missed))
            dao.insertMedicationIntake(
                MedicationIntakeEntity(
                    medicationId = medication.id,
                    medicationName = medication.name,
                    scheduledAt = now(),
                    takenAt = null,
                    status = if (missed >= 5) "escalated" else "missed"
                )
            )
        }
    }

    fun deleteMedication(entry: MedicationEntity) {
        viewModelScope.launch { dao.deleteMedication(entry) }
    }

    fun addDiaryEntry(text: String, photoUris: List<String>) {
        viewModelScope.launch {
            dao.insertDiaryEntry(
                DiaryEntryEntity(
                    timestamp = now(),
                    text = text.trim(),
                    photoUris = photoUris.joinToString("|")
                )
            )
        }
    }

    fun updateDiaryEntry(entry: DiaryEntryEntity, text: String, photoUris: List<String>) {
        viewModelScope.launch {
            dao.updateDiaryEntry(entry.copy(text = text.trim(), photoUris = photoUris.joinToString("|")))
        }
    }

    fun deleteDiaryEntry(entry: DiaryEntryEntity) {
        viewModelScope.launch { dao.deleteDiaryEntry(entry) }
    }

    fun addSleepEntry(date: String, asleepAt: String, wokeAt: String, quality: Int, markers: List<String>) {
        viewModelScope.launch {
            dao.insertSleepEntry(
                SleepEntryEntity(
                    date = date.trim(),
                    asleepAt = asleepAt.trim(),
                    wokeAt = wokeAt.trim(),
                    quality = quality.coerceIn(1, 10),
                    mixedStateMarkers = markers.joinToString("|")
                )
            )
        }
    }

    fun addTrustedPerson(name: String, phone: String) {
        viewModelScope.launch {
            dao.insertTrustedPerson(
                TrustedPersonEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    avatarLabel = name.trim().take(1).ifBlank { "Б" },
                    accessToken = UUID.randomUUID().toString()
                )
            )
        }
    }

    fun revokeTrustedAccess(person: TrustedPersonEntity) {
        viewModelScope.launch { dao.updateTrustedPerson(person.copy(revoked = true)) }
    }

    fun restoreTrustedAccess(person: TrustedPersonEntity) {
        viewModelScope.launch { dao.updateTrustedPerson(person.copy(revoked = false)) }
    }

    fun deleteTrustedPerson(person: TrustedPersonEntity) {
        viewModelScope.launch { dao.deleteTrustedPerson(person) }
    }

    fun saveProfile(profile: ProfileEntity) {
        viewModelScope.launch { dao.upsertProfile(profile) }
    }

    fun exportCsv(): String {
        val moods = moodEntries.value.joinToString("\n") {
            "mood,${formatDateTime(it.timestamp)},${it.mood},${it.category},\"${it.note.escapeCsv()}\""
        }
        val impulses = impulseEntries.value.joinToString("\n") {
            "impulse,${formatDateTime(it.timestamp)},${it.authorScore},${it.autoScore},\"${it.description.escapeCsv()}\""
        }
        val meds = medications.value.joinToString("\n") {
            "medication,${it.time},${it.name},${it.dosage},${it.missedReminders}"
        }
        return listOf("type,date,value,extra,note", moods, impulses, meds)
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun calculateImpulseScore(description: String, cost: Double?, category: String): Int {
        var score = 1
        score += when {
            cost == null -> 0
            cost < 1_000.0 -> 1
            cost < 5_000.0 -> 2
            cost < 20_000.0 -> 4
            else -> 6
        }
        score += when (category) {
            "Кредиты/финансовые обязательства" -> 6
            "Отношения/крупные решения" -> 5
            "Путешествия/билеты" -> 4
            "Техника/гаджеты" -> 3
            "Одежда/косметика" -> 2
            "Хобби/мелочи" -> 1
            else -> 1
        }
        val hotWords = listOf("все", "сейчас", "немедленно", "навсегда", "срочно", "точно надо")
        score += hotWords.count { description.lowercase(Locale.getDefault()).contains(it) }.coerceAtMost(3)
        val latestElevatedMood = moodEntries.value
            .firstOrNull { now() - it.timestamp <= TimeUnit.HOURS.toMillis(2) }
            ?.mood ?: 0.0
        if (latestElevatedMood >= 4.0) score += 2
        return score.coerceIn(1, 10)
    }

    private fun scheduleMedicationReminder(medicationId: Long, name: String, dosage: String) {
        val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(30, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    MedicationReminderWorker.KEY_MEDICATION_ID to medicationId,
                    MedicationReminderWorker.KEY_MEDICATION_NAME to name,
                    MedicationReminderWorker.KEY_DOSAGE to dosage
                )
            )
            .build()
        WorkManager.getInstance(getApplication()).enqueue(request)
    }

    private suspend fun seedFirstRunData() {
        val base = now()
        dao.insertMoodEntry(
            MoodEntryEntity(
                timestamp = base - TimeUnit.HOURS.toMillis(6),
                mood = -1.5,
                category = "нейтральное",
                symptoms = "нарушение сна",
                note = "Первичная демонстрационная запись. Можно удалить и начать вести дневник с нуля."
            )
        )
        dao.insertMoodEntry(
            MoodEntryEntity(
                timestamp = base - TimeUnit.HOURS.toMillis(2),
                mood = 1.0,
                category = "нейтральное",
                symptoms = "",
                note = "Состояние стало ровнее после прогулки."
            )
        )
        dao.insertDiaryEntry(
            DiaryEntryEntity(
                timestamp = base - TimeUnit.HOURS.toMillis(1),
                text = "Добро пожаловать в БАРсик. Здесь можно вести заметки, прикреплять фото и отслеживать состояние.",
                photoUris = ""
            )
        )
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        fun defaultProfile(): ProfileEntity = ProfileEntity(
            userName = "Пользователь",
            avatarLabel = "Б",
            birthday = "",
            darkTheme = true,
            soundEnabled = true,
            timeZone = TimeZone.getDefault().id,
            reminderIntervalMinutes = 30,
            doctorContact = "Укажите контакт врача в настройках",
            crisisNumbers = "112",
            copingStrategies = "Позвонить близкому|Выпить воды|Отложить важные решения на 24 часа|Перейти в тихое место"
        )
    }
}

class AppViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

fun formatDateTime(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
}

private fun String.escapeCsv(): String = replace("\"", "\"\"")

fun Double.prettyMood(): String {
    return if (this.roundToInt().toDouble() == this) {
        this.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", this)
    }
}
