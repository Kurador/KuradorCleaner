package com.kurador.cleaner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kurador.cleaner.MainActivity
import com.kurador.cleaner.engine.CleanBus
import com.kurador.cleaner.engine.CleanModule
import com.kurador.cleaner.engine.CleanerEngine
import com.kurador.cleaner.engine.LogEntry
import com.kurador.cleaner.engine.formatBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CleanService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engine by lazy { CleanerEngine(listOfNotNull(cacheDir, externalCacheDir)) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ids = intent?.getStringArrayExtra(EXTRA_MODULES)?.toSet().orEmpty()
        val modules = CleanModule.entries.filter { it.id in ids }.toSet()
        if (modules.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Iniciando limpieza…"))
        CleanBus.setRunning(true)
        CleanBus.log("Servicio iniciado · módulos: ${modules.joinToString { it.title }}")

        serviceScope.launch {
            try {
                engine.runModules(modules)
                val total = CleanBus.totalFreed.value
                CleanBus.log("Limpieza finalizada · Total liberado: ${formatBytes(total)}", LogEntry.Level.SUCCESS)
                updateNotification("Completado · ${formatBytes(total)} liberados")
            } catch (e: Exception) {
                CleanBus.log("Error fatal del motor: ${e.message}", LogEntry.Level.ERROR)
            } finally {
                CleanBus.setRunning(false)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        createChannel()
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Kurador Cleaner")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Limpieza en segundo plano", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object {
        const val EXTRA_MODULES = "com.kurador.cleaner.extra.MODULES"
        private const val CHANNEL_ID = "clean_service_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
