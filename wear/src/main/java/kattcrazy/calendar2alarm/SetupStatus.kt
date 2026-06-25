package kattcrazy.calendar2alarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object SetupStatus {
    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun canScheduleExactAlarms(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return nm.canUseFullScreenIntent()
    }

    fun hasAllPermissions(context: Context): Boolean =
        hasCalendarPermission(context) &&
            hasNotificationPermission(context) &&
            canScheduleExactAlarms(context) &&
            canUseFullScreenIntent(context)

    fun isReady(context: Context): Boolean =
        hasCalendarPermission(context) &&
            hasNotificationPermission(context) &&
            canScheduleExactAlarms(context)

    fun notReadyReason(context: Context): String? = when {
        !hasCalendarPermission(context) -> "Calendar permission missing"
        !hasNotificationPermission(context) -> "Notifications disabled"
        !canScheduleExactAlarms(context) -> "Exact alarms not allowed"
        else -> null
    }

    fun hasConnectedCalendarAndApp(context: Context): Boolean {
        if (!hasCalendarPermission(context)) return false
        val viewer = AppPreferences.viewerApp(context)
        if (!viewer.isInstalled(context)) return false
        val visible = CalendarRepository.listVisibleCalendars(context)
        if (visible.isEmpty()) return false
        val selected = AppPreferences.selectedCalendarIds(context)
        return visible.any { it.id in selected }
    }
}
