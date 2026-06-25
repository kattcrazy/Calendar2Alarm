package kattcrazy.calendar2alarm

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log

object AlarmSuppression {
    private const val TAG = "Calendar2Alarm"
    private const val ZEN_MODE_OFF = 0
    private const val ZEN_MODE_IMPORTANT_INTERRUPTIONS = 1
    private const val ZEN_MODE_NO_INTERRUPTIONS = 2
    private const val ZEN_MODE_ALARMS = 3

    fun shouldSuppressAlarm(context: Context): Boolean {
        val reason = suppressionReason(context)
        if (reason != null) {
            Log.i(TAG, "Alarm suppressed: $reason")
        }
        return reason != null
    }

    fun suppressionReason(context: Context): String? {
        if (isTheaterModeOn(context)) return "Theater mode"
        if (isZenBlockingAlarms(context)) return "Do not disturb"
        if (isAlarmStreamMuted(context)) return "Alarm volume muted"
        return null
    }

    private fun isTheaterModeOn(context: Context): Boolean {
        val resolver = context.contentResolver
        val keys = listOf(
            "theater_mode_on",
            "theater_mode",
            "settings_system/watch_theater_mode",
            "setting_theatre_mode",
        )
        for (key in keys) {
            val value = runCatching {
                Settings.Global.getInt(resolver, key, 0)
            }.getOrElse {
                runCatching { Settings.System.getInt(resolver, key, 0) }.getOrElse { 0 }
            }
            if (value == 1) return true
        }
        return false
    }

    private fun isZenBlockingAlarms(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        when (nm.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_NONE -> return true
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> return false
            NotificationManager.INTERRUPTION_FILTER_ALL -> return false
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> {
                val policy = nm.notificationPolicy ?: return false
                val alarmsAllowed = policy.priorityCategories and
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS != 0
                return !alarmsAllowed
            }
        }

        val zen = Settings.Global.getInt(
            context.contentResolver,
            "zen_mode",
            ZEN_MODE_OFF,
        )
        return when (zen) {
            ZEN_MODE_OFF,
            ZEN_MODE_ALARMS,
            -> false
            ZEN_MODE_NO_INTERRUPTIONS -> true
            ZEN_MODE_IMPORTANT_INTERRUPTIONS -> {
                val policy = nm.notificationPolicy
                policy == null || policy.priorityCategories and
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS == 0
            }
            else -> false
        }
    }

    private fun isAlarmStreamMuted(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return am.isStreamMute(android.media.AudioManager.STREAM_ALARM)
        }
        return am.getStreamVolume(android.media.AudioManager.STREAM_ALARM) == 0
    }
}
