package kattcrazy.calendar2alarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
        if (hasFullScreenIntentPermissionGrant(context)) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return nm.canUseFullScreenIntent()
    }

    /** Play-approved alarm apps are often granted FSI at install; Wear builds may not reflect that in canUseFullScreenIntent(). */
    private fun hasFullScreenIntentPermissionGrant(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FULL_SCREEN_INTENT) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val permissions = packageInfo.requestedPermissions ?: return false
        val flags = packageInfo.requestedPermissionsFlags ?: return false
        val index = permissions.indexOf(Manifest.permission.USE_FULL_SCREEN_INTENT)
        if (index < 0) return false
        return (flags[index] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
    }

    fun hasAllPermissions(context: Context): Boolean = isReady(context)

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
