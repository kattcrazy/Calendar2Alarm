package kattcrazy.calendar2alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmForegroundService : Service() {
    private var player: AlarmSoundPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val payload = intent?.let { AlarmExtras.read(it, this) } ?: return START_NOT_STICKY
        if (AlarmSuppression.shouldSuppressAlarm(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = NotificationHelper.buildAlarmNotification(this, payload)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        player?.stop()
        player = AlarmSoundPlayer(this).also { it.start() }
        return START_STICKY
    }

    override fun onDestroy() {
        player?.stop()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context, payload: AlarmPayload) {
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                AlarmExtras.write(this, payload)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmForegroundService::class.java))
        }
    }
}

object NotificationHelper {
    fun buildAlarmNotification(context: Context, payload: AlarmPayload): android.app.Notification {
        val channelId = "calendar2alarm_alarms"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                context.getString(R.string.alarm_channel_name),
                android.app.NotificationManager.IMPORTANCE_HIGH,
            )
            nm.createNotificationChannel(channel)
        }

        val fullScreen = Intent(context, AlarmActivity::class.java).apply {
            AlarmExtras.write(this, payload)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fullScreenPi = android.app.PendingIntent.getActivity(
            context,
            payload.requestCode,
            fullScreen,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.alarm_notification_title))
            .setContentText(payload.title)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreenPi, true)
            .setOngoing(true)
            .build()
    }
}
