package com.bipolarmood.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bipolarmood.data.MedicationEntity
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object MedicationReminderScheduler {
    private const val TAG = "medication_reminder"

    fun schedule(context: Context, medication: MedicationEntity, intervalMinutes: Int = 30) {
        val delayMs = computeDelayUntilNextDose(medication.time, medication.timeZone, medication.frequency)
            .coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
        val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .setInputData(
                workDataOf(
                    MedicationReminderWorker.KEY_MEDICATION_ID to medication.id,
                    MedicationReminderWorker.KEY_MEDICATION_NAME to medication.name,
                    MedicationReminderWorker.KEY_DOSAGE to medication.dosage,
                    MedicationReminderWorker.KEY_TIME to medication.time,
                    MedicationReminderWorker.KEY_TIMEZONE to medication.timeZone,
                    MedicationReminderWorker.KEY_FREQUENCY to medication.frequency,
                    MedicationReminderWorker.KEY_INTERVAL_MINUTES to intervalMinutes.coerceAtLeast(15)
                )
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(medication.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, medicationId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(medicationId))
    }

    fun notificationId(medicationId: Long): Int = abs((medicationId xor (medicationId shr 32)).toInt())

    internal fun uniqueWorkName(medicationId: Long): String = "med_reminder_$medicationId"

    internal fun computeDelayUntilNextDose(time: String, timeZoneId: String, frequency: String): Long {
        val parts = time.trim().split(":")
        if (parts.size < 2) return TimeUnit.HOURS.toMillis(1)
        val hour = parts[0].toIntOrNull() ?: return TimeUnit.HOURS.toMillis(1)
        val minute = parts[1].toIntOrNull() ?: return TimeUnit.HOURS.toMillis(1)

        val zone = runCatching { TimeZone.getTimeZone(timeZoneId.ifBlank { TimeZone.getDefault().id }) }
            .getOrDefault(TimeZone.getDefault())
        val now = Calendar.getInstance(zone)
        val target = Calendar.getInstance(zone).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        if (!target.after(now)) {
            val daysToAdd = when (frequency.lowercase(Locale.getDefault())) {
                "через день" -> 2
                else -> 1
            }
            target.add(Calendar.DAY_OF_YEAR, daysToAdd)
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}
