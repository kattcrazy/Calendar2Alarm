package kattcrazy.calendartoalarm

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log

object AlarmSuppression {
    private const val TAG = "Calendar2Alarm"

    fun shouldSuppressAlarm(context: Context): Boolean {
        val reason = suppressionReason(context)
        if (reason != null) {
            Log.i(TAG, "Alarm suppressed: $reason")
        }
        return reason != null
    }

    fun suppressionReason(context: Context): String? {
        if (isTheaterModeOn(context)) return "Theater mode"
        if (isBedtimeModeOn(context)) return "Bedtime mode"
        if (isZenBlockingAlarms(context)) return "Do not disturb"
        if (isAlarmStreamMuted(context)) return "Alarm volume muted"
        return null
    }

    private fun isTheaterModeOn(context: Context): Boolean {
        WearModeProbe.isTheaterActive(context)?.let { active ->
            Log.d(TAG, "Wear TheaterMode API active=$active")
            if (active) return true
        }

        val resolver = context.contentResolver
        val keys = listOf(
            // Samsung One UI Watch 6/7 Modes (confirmed on Galaxy Watch 7)
            "setting_theater_mode_on",
            // Stock Wear / legacy
            "theater_mode_on",
            "theater_mode",
            "watch_theater_mode",
            "settings_system/watch_theater_mode",
            "setting_theatre_mode",
            "cinema_mode",
            "cinema_mode_on",
            "mode_theater",
            "modes_theater",
            "theater_mode_enabled",
        )
        for (key in keys) {
            if (settingIsEnabled(resolver, Settings.Global::class.java, key) ||
                settingIsEnabled(resolver, Settings.Secure::class.java, key) ||
                settingIsEnabled(resolver, Settings.System::class.java, key)
            ) {
                Log.d(TAG, "Theater mode setting enabled: $key")
                return true
            }
        }
        return false
    }

    private fun isBedtimeModeOn(context: Context): Boolean {
        WearModeProbe.isBedtimeActive(context)?.let { active ->
            if (active) return true
        }
        val resolver = context.contentResolver
        val keys = listOf(
            // Samsung One UI Watch Modes
            "setting_bedtime_mode_running_state",
            // Stock Wear OS (Settings.Global.Wearable.BEDTIME_MODE, since ~Wear OS 3)
            "bedtime_mode",
        )
        for (key in keys) {
            if (settingIsEnabled(resolver, Settings.Global::class.java, key) ||
                settingIsEnabled(resolver, Settings.Secure::class.java, key) ||
                settingIsEnabled(resolver, Settings.System::class.java, key)
            ) {
                Log.d(TAG, "Bedtime mode setting enabled: $key")
                return true
            }
        }
        return false
    }

    private fun settingIsEnabled(
        resolver: android.content.ContentResolver,
        settingsClass: Class<*>,
        key: String,
    ): Boolean {
        val intValue = readSettingInt(resolver, settingsClass, key)
        if (intValue != null && intValue != 0) return true
        val stringValue = readSettingString(resolver, settingsClass, key) ?: return false
        return stringValue == "1" ||
            stringValue.equals("true", ignoreCase = true) ||
            stringValue.equals("on", ignoreCase = true)
    }

    private fun readSettingInt(
        resolver: android.content.ContentResolver,
        settingsClass: Class<*>,
        key: String,
    ): Int? {
        return runCatching {
            val method = settingsClass.getMethod(
                "getInt",
                android.content.ContentResolver::class.java,
                String::class.java,
            )
            method.invoke(null, resolver, key) as Int
        }.getOrNull()
    }

    private fun readSettingString(
        resolver: android.content.ContentResolver,
        settingsClass: Class<*>,
        key: String,
    ): String? {
        return runCatching {
            val method = settingsClass.getMethod(
                "getString",
                android.content.ContentResolver::class.java,
                String::class.java,
            )
            method.invoke(null, resolver, key) as String?
        }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
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

        val zen = Settings.Global.getInt(context.contentResolver, "zen_mode", ZEN_MODE_OFF)
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

    private const val ZEN_MODE_OFF = 0
    private const val ZEN_MODE_IMPORTANT_INTERRUPTIONS = 1
    private const val ZEN_MODE_NO_INTERRUPTIONS = 2
    private const val ZEN_MODE_ALARMS = 3
}

/**
 * Wear OS ModeManager (Watch 5+ / Modes on 6/7). Reflection avoids compile-time wear-sdk dependency.
 */
private object WearModeProbe {
    fun isTheaterActive(context: Context): Boolean? =
        modeIsActive(context, "getTheaterMode")

    fun isBedtimeActive(context: Context): Boolean? =
        modeIsActive(context, "getBedtimeMode")

    private fun modeIsActive(context: Context, getterName: String): Boolean? {
        return runCatching {
            val sdkClass = Class.forName("com.google.wear.Sdk")
            val modeManagerClass = Class.forName("com.google.wear.modes.ModeManager")
            val modeManager = obtainModeManager(context, sdkClass, modeManagerClass) ?: return null
            val mode = modeManagerClass.getMethod(getterName).invoke(modeManager) ?: return null
            mode.javaClass.getMethod("isActive").invoke(mode) as Boolean
        }.getOrElse { error ->
            Log.d("Calendar2Alarm", "Wear mode probe $getterName unavailable: ${error.message}")
            null
        }
    }

    private fun obtainModeManager(
        context: Context,
        sdkClass: Class<*>,
        modeManagerClass: Class<*>,
    ): Any? {
        return runCatching {
            sdkClass.getMethod(
                "getWearManager",
                Context::class.java,
                Class::class.java,
            ).invoke(null, context.applicationContext, modeManagerClass)
        }.getOrNull() ?: runCatching {
            sdkClass.getMethod(
                "getWearManager",
                Class::class.java,
            ).invoke(null, modeManagerClass)
        }.getOrNull()
    }
}
