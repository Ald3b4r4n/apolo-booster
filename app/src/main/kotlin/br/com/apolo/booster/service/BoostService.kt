package br.com.apolo.booster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import br.com.apolo.booster.MainActivity
import br.com.apolo.booster.R
import br.com.apolo.booster.audio.BoostController
import br.com.apolo.booster.audio.BoostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Foreground service que mantem o processo vivo enquanto o boost esta ativo.
 * O processo morre, o efeito morre; por isso o servico existe.
 */
class BoostService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundStarted = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        scope.launch {
            BoostController.state.collect { state ->
                if (!foregroundStarted) return@collect
                if (!state.enabled) {
                    stopSelf()
                } else {
                    notificationManager().notify(NOTIFICATION_ID, buildNotification(state))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                BoostController.setEnabled(false)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> BoostController.setEnabled(!BoostController.state.value.enabled)
        }

        startForegroundSafe()
        foregroundStarted = true
        if (BoostController.state.value.enabled) {
            BoostController.setEnabled(true)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        foregroundStarted = false
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundSafe() {
        val notification = buildNotification(BoostController.state.value)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Throwable) {
            stopSelf()
        }
    }

    private fun buildNotification(state: BoostState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val text = if (state.enabled) {
            getString(R.string.notif_text_on, state.percent)
        } else {
            getString(R.string.notif_text_off)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_boost)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, getString(R.string.notif_action_toggle), servicePending(ACTION_TOGGLE, 1))
            .addAction(0, getString(R.string.notif_action_stop), servicePending(ACTION_STOP, 2))
            .build()
    }

    private fun servicePending(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, BoostService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
            setSound(null, null)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NotificationManager::class.java)

    companion object {
        private const val CHANNEL_ID = "apolo_boost"
        private const val NOTIFICATION_ID = 42
        private const val ACTION_STOP = "br.com.apolo.booster.action.STOP"
        private const val ACTION_TOGGLE = "br.com.apolo.booster.action.TOGGLE"

        fun start(context: Context) {
            val intent = Intent(context, BoostService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BoostService::class.java))
        }
    }
}

