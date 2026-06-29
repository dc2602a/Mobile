package com.bipolarmood.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bipolarmood.R
import com.bipolarmood.data.AppDatabase
import com.bipolarmood.data.MedicationEntity

class MedicationReminderWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val medicationId = inputData.getLong(KEY_MEDICATION_ID, -1L)
        val medicationName = inputData.getString(KEY_MEDICATION_NAME).orEmpty().ifBlank { "препарат" }
        val dosage = inputData.getString(KEY_DOSAGE).orEmpty()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Пора принять лекарство")
            .setContentText(listOf(medicationName, dosage).filter { it.isNotBlank() }.joinToString(", "))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            MedicationReminderScheduler.notificationId(medicationId),
            notification
        )

        if (medicationId > 0) {
            val dao = AppDatabase.get(context).dao()
            val medication = dao.getAllMedications().firstOrNull { it.id == medicationId }
                ?: MedicationEntity(
                    id = medicationId,
                    name = medicationName,
                    dosage = dosage,
                    time = inputData.getString(KEY_TIME).orEmpty().ifBlank { "09:00" },
                    frequency = inputData.getString(KEY_FREQUENCY).orEmpty().ifBlank { "ежедневно" },
                    timeZone = inputData.getString(KEY_TIMEZONE).orEmpty().ifBlank { java.util.TimeZone.getDefault().id }
                )
            val interval = inputData.getInt(KEY_INTERVAL_MINUTES, 30).coerceAtLeast(15)
            MedicationReminderScheduler.schedule(context, medication, interval)
        }
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания о лекарствах",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Напоминания о приёме назначенных препаратов по расписанию"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_MEDICATION_NAME = "medication_name"
        const val KEY_DOSAGE = "dosage"
        const val KEY_TIME = "medication_time"
        const val KEY_TIMEZONE = "medication_timezone"
        const val KEY_FREQUENCY = "medication_frequency"
        const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val CHANNEL_ID = "medication_reminders"
    }
}
