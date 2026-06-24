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
            inputData.getLong(KEY_MEDICATION_ID, 1L).toInt(),
            notification
        )
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
            description = "Повторные напоминания о приеме назначенных препаратов"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_MEDICATION_NAME = "medication_name"
        const val KEY_DOSAGE = "dosage"
        private const val CHANNEL_ID = "medication_reminders"
    }
}
