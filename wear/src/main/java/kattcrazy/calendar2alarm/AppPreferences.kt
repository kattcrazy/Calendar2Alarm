package kattcrazy.calendar2alarm

import android.content.Context

object AppPreferences {
    private const val PREFS = "calendar2alarm_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_CLOSEST_REMINDER_ONLY = "closest_reminder_only"
    private const val KEY_SELECTED_CALENDAR_IDS = "selected_calendar_ids"
    private const val KEY_VIEWER_APP = "viewer_app"
    private const val KEY_LAST_RESYNC = "last_resync"
    private const val KEY_SCHEDULED_COUNT = "scheduled_count"

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun closestReminderOnly(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CLOSEST_REMINDER_ONLY, true)

    fun setClosestReminderOnly(context: Context, closestOnly: Boolean) {
        prefs(context).edit().putBoolean(KEY_CLOSEST_REMINDER_ONLY, closestOnly).apply()
    }

    fun selectedCalendarIds(context: Context): Set<Long> {
        val stored = prefs(context).getStringSet(KEY_SELECTED_CALENDAR_IDS, null)
        if (stored != null && stored.isNotEmpty()) {
            return stored.mapNotNull { it.toLongOrNull() }.toSet()
        }
        return CalendarRepository.listVisibleCalendars(context)
            .map { it.id }
            .toSet()
    }

    fun setSelectedCalendarIds(context: Context, ids: Set<Long>) {
        val editor = prefs(context).edit()
        if (ids.isEmpty()) {
            editor.remove(KEY_SELECTED_CALENDAR_IDS)
        } else {
            editor.putStringSet(KEY_SELECTED_CALENDAR_IDS, ids.map { it.toString() }.toSet())
        }
        editor.apply()
    }

    fun toggleCalendarId(context: Context, calendarId: Long, selected: Boolean): Set<Long> {
        val current = selectedCalendarIds(context)
        val updated = if (selected) current + calendarId else current - calendarId
        setSelectedCalendarIds(context, updated)
        return updated
    }

    fun applyViewerApp(context: Context, app: ViewerApp) {
        setViewerApp(context, app)
    }

    fun viewerApp(context: Context): ViewerApp =
        ViewerApp.fromKey(prefs(context).getString(KEY_VIEWER_APP, ViewerApp.GOOGLE.key))

    fun setViewerApp(context: Context, app: ViewerApp) {
        prefs(context).edit().putString(KEY_VIEWER_APP, app.key).apply()
    }

    fun setLastResync(context: Context, timeMs: Long, count: Int) {
        prefs(context).edit()
            .putLong(KEY_LAST_RESYNC, timeMs)
            .putInt(KEY_SCHEDULED_COUNT, count)
            .apply()
    }

    fun lastResyncMs(context: Context): Long = prefs(context).getLong(KEY_LAST_RESYNC, 0L)

    fun scheduledCount(context: Context): Int = prefs(context).getInt(KEY_SCHEDULED_COUNT, 0)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

