package kattcrazy.calendar2alarm

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import androidx.wear.provider.WearableCalendarContract

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String = "",
    /** True when the watch provided a real calendar title (not a hash fallback). */
    val hasRealName: Boolean = false,
)

data class CalendarEventSample(
    val title: String,
    val beginMs: Long,
    val endMs: Long,
    val allDay: Boolean,
)

object CalendarRepository {
    private const val TAG = "Calendar2Alarm"
    private const val HORIZON_DAYS = 7
    /** Match Google Calendar on Wear (~4 weeks) when discovering calendar list. */
    private const val CALENDAR_LIST_DAYS = 28
    private const val MS_PER_DAY = 24L * 60 * 60 * 1000L

    private fun rollingHorizonEnd(now: Long = System.currentTimeMillis()): Long =
        now + HORIZON_DAYS * MS_PER_DAY

    /** Events with alert reminders starting within the next 7×24 hours (rolling, not calendar week). */
    fun countEventsWithRemindersInHorizon(context: Context): Int {
        if (!SetupStatus.hasCalendarPermission(context)) return 0
        val calendarIds = AppPreferences.selectedCalendarIds(context)
        if (calendarIds.isEmpty()) return 0

        val now = System.currentTimeMillis()
        val horizon = rollingHorizonEnd(now)
        val reminders = loadReminders(context)
        val eventIds = mutableSetOf<Long>()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.CALENDAR_ID,
        )
        context.contentResolver.query(
            instancesUri(now, horizon),
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val eventIdIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val calIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
            while (cursor.moveToNext()) {
                val calId = cursor.getLong(calIdx)
                if (calId !in calendarIds) continue
                val eventId = cursor.getLong(eventIdIdx)
                if (reminders[eventId].isNullOrEmpty()) continue
                val begin = cursor.getLong(beginIdx)
                if (begin < now || begin > horizon) continue
                eventIds.add(eventId)
            }
        }
        return eventIds.size
    }

    fun listVisibleCalendars(context: Context, viewer: ViewerApp? = null): List<CalendarInfo> {
        if (!SetupStatus.hasCalendarPermission(context)) {
            Log.w(TAG, "listVisibleCalendars: no READ_CALENDAR permission")
            return emptyList()
        }
        val now = System.currentTimeMillis()
        val horizon = now + CALENDAR_LIST_DAYS * 24L * 60 * 60 * 1000L
        val byId = linkedMapOf<Long, CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.OWNER_ACCOUNT,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
        )
        context.contentResolver.query(
            instancesUri(now, horizon),
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val calIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
            val nameIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val ownerIdx = cursor.getColumnIndex(CalendarContract.Instances.OWNER_ACCOUNT)
            val accountIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            val typeIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
            while (cursor.moveToNext()) {
                val calId = cursor.getLong(calIdx)
                val accountType = cursor.getString(typeIdx) ?: inferAccountType(cursor.getString(ownerIdx))
                if (viewer != null && !matchesViewer(accountType, viewer)) continue
                val ownerAccount = cursor.getString(ownerIdx)
                val resolved = resolveCalendarDisplayName(
                    calDisplayName = cursor.getString(nameIdx),
                    ownerAccount = ownerAccount,
                )
                val accountName = readableAccountLabel(
                    accountName = cursor.getString(accountIdx),
                    ownerAccount = ownerAccount,
                )
                val info = CalendarInfo(
                    id = calId,
                    displayName = resolved.name,
                    accountName = accountName,
                    accountType = accountType,
                    hasRealName = resolved.hasRealName,
                )
                val existing = byId[calId]
                if (existing == null || isBetterCalendarName(info, existing)) {
                    byId[calId] = info
                }
            }
        } ?: Log.e(TAG, "listVisibleCalendars: wearable provider returned null cursor")
        enrichFromCalendarsTable(context, byId)
        disambiguateDuplicateNames(byId)
        Log.i(TAG, "listVisibleCalendars count=${byId.size}")
        byId.values.forEach { Log.d(TAG, "  cal id=${it.id} name=${it.displayName} real=${it.hasRealName}") }
        return byId.values.sortedWith(
            compareBy<CalendarInfo> { !it.hasRealName }
                .thenBy { if (it.hasRealName) it.displayName.lowercase() else it.id.toString() },
        )
    }

    fun sampleUpcomingEvents(context: Context, calendarId: Long, limit: Int = 5): List<CalendarEventSample> {
        if (!SetupStatus.hasCalendarPermission(context)) return emptyList()
        val now = System.currentTimeMillis()
        val horizon = now + CALENDAR_LIST_DAYS * 24L * 60 * 60 * 1000L
        val results = mutableListOf<CalendarEventSample>()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.ALL_DAY,
        )
        context.contentResolver.query(
            instancesUri(now, horizon),
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
            val calIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
            val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            while (cursor.moveToNext()) {
                if (cursor.getLong(calIdx) != calendarId) continue
                val begin = cursor.getLong(beginIdx)
                if (begin < now) continue
                results.add(
                    CalendarEventSample(
                        title = cursor.getString(titleIdx)?.trim().orEmpty().ifEmpty { "Event" },
                        beginMs = begin,
                        endMs = cursor.getLong(endIdx),
                        allDay = allDayIdx >= 0 && cursor.getInt(allDayIdx) == 1,
                    ),
                )
            }
        }
        return results
            .sortedBy { it.beginMs }
            .distinctBy { "${it.title}|${it.beginMs}" }
            .take(limit)
    }

    fun loadUpcomingAlarms(context: Context): List<AlarmPayload> {
        if (!SetupStatus.hasCalendarPermission(context)) return emptyList()
        val calendarIds = AppPreferences.selectedCalendarIds(context)
        if (calendarIds.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val alarmHorizon = rollingHorizonEnd(now)
        val reminders = loadReminders(context)
        val maxReminderMs = reminders.values.flatten().maxOrNull()?.coerceAtLeast(0)?.times(60_000L) ?: 0L
        val instanceHorizon = alarmHorizon + maxReminderMs
        val results = mutableListOf<AlarmPayload>()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.ALL_DAY,
        )
        context.contentResolver.query(
            instancesUri(now, instanceHorizon),
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val eventIdIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
            val calIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
            val descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
            val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            while (cursor.moveToNext()) {
                val calId = cursor.getLong(calIdx)
                if (calId !in calendarIds) continue
                val eventId = cursor.getLong(eventIdIdx)
                val begin = cursor.getLong(beginIdx)
                val end = cursor.getLong(endIdx)
                val title = cursor.getString(titleIdx) ?: "Event"
                val description = if (descIdx >= 0) cursor.getString(descIdx) ?: "" else ""
                val allDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false
                val reminderMinutes = reminders[eventId].orEmpty()
                if (reminderMinutes.isEmpty()) continue
                val minutesToSchedule = if (AppPreferences.closestReminderOnly(context)) {
                    listOf(reminderMinutes.min())
                } else {
                    reminderMinutes
                }
                for (minutes in minutesToSchedule) {
                    val alarmTime = begin - minutes * 60_000L
                    if (alarmTime <= now) continue
                    if (alarmTime > alarmHorizon) continue
                    results.add(
                        AlarmPayload(
                            eventId = eventId,
                            title = title,
                            description = description,
                            beginMs = begin,
                            endMs = end,
                            allDay = allDay,
                            reminderMinutes = minutes,
                            requestCode = stableRequestCode(eventId, begin, minutes),
                        ),
                    )
                }
            }
        }
        Log.i(TAG, "Loaded ${results.size} alarms")
        return results.distinctBy { it.requestCode }
    }

    private fun instancesUri(beginMs: Long, endMs: Long) =
        WearableCalendarContract.Instances.CONTENT_URI.buildUpon().apply {
            ContentUris.appendId(this, beginMs)
            ContentUris.appendId(this, endMs)
        }.build()

    private fun loadReminders(context: Context): Map<Long, List<Int>> {
        val map = mutableMapOf<Long, MutableList<Int>>()
        context.contentResolver.query(
            WearableCalendarContract.Reminders.CONTENT_URI,
            arrayOf(
                CalendarContract.Reminders.EVENT_ID,
                CalendarContract.Reminders.MINUTES,
                CalendarContract.Reminders.METHOD,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val eventIdx = cursor.getColumnIndex(CalendarContract.Reminders.EVENT_ID)
            val minIdx = cursor.getColumnIndex(CalendarContract.Reminders.MINUTES)
            val methodIdx = cursor.getColumnIndex(CalendarContract.Reminders.METHOD)
            while (cursor.moveToNext()) {
                if (methodIdx >= 0 && cursor.getInt(methodIdx) != CalendarContract.Reminders.METHOD_ALERT) continue
                val eventId = cursor.getLong(eventIdx)
                map.getOrPut(eventId) { mutableListOf() }.add(cursor.getInt(minIdx))
            }
        }
        return map
    }

    private data class ResolvedCalendarName(val name: String, val hasRealName: Boolean)

    private fun resolveCalendarDisplayName(
        calDisplayName: String?,
        ownerAccount: String?,
    ): ResolvedCalendarName {
        calDisplayName?.trim()?.takeIf { it.isNotEmpty() && !isWeakDisplayName(it) }?.let {
            return ResolvedCalendarName(it, hasRealName = true)
        }
        if (ownerAccount.isNullOrBlank()) {
            return ResolvedCalendarName("", hasRealName = false)
        }
        val at = ownerAccount.indexOf('@')
        if (at <= 0) return ResolvedCalendarName(ownerAccount, hasRealName = true)
        val local = ownerAccount.substring(0, at)
        val domain = ownerAccount.substring(at + 1)
        return when {
            domain.equals("group.calendar.google.com", ignoreCase = true) ||
                isHexHash(local) ->
                ResolvedCalendarName("", hasRealName = false)
            domain.equals("gmail.com", ignoreCase = true) ||
                domain.equals("googlemail.com", ignoreCase = true) ->
                ResolvedCalendarName(local.replaceFirstChar { it.titlecase() }, hasRealName = true)
            else -> ResolvedCalendarName(local.replaceFirstChar { it.titlecase() }, hasRealName = true)
        }
    }

    private fun isWeakDisplayName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.length <= 4) return true
        return trimmed.equals("gcal", ignoreCase = true) ||
            trimmed.equals("cal", ignoreCase = true)
    }

    /** Best-effort names from the system calendars table (often empty on Wear). */
    private fun enrichFromCalendarsTable(context: Context, byId: MutableMap<Long, CalendarInfo>) {
        if (byId.isEmpty()) return
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
        )
        runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE}=1",
                null,
                null,
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                while (cursor.moveToNext()) {
                    val calId = cursor.getLong(idIdx)
                    val existing = byId[calId] ?: continue
                    val name = cursor.getString(nameIdx)?.trim().orEmpty()
                    if (name.isEmpty() || isWeakDisplayName(name)) continue
                    val account = readableAccountLabel(cursor.getString(accountIdx), existing.accountName)
                    byId[calId] = existing.copy(
                        displayName = name,
                        accountName = account.ifBlank { existing.accountName },
                        hasRealName = true,
                    )
                }
            }
        }.onFailure { Log.w(TAG, "Calendars table enrichment failed", it) }
    }

    private fun disambiguateDuplicateNames(byId: MutableMap<Long, CalendarInfo>) {
        val named = byId.values.filter { it.hasRealName }
        val counts = named.groupingBy { it.displayName.lowercase() }.eachCount()
        for ((id, info) in byId.entries.toList()) {
            if (!info.hasRealName) continue
            if (counts.getValue(info.displayName.lowercase()) > 1) {
                byId[id] = info.copy(displayName = "${info.displayName} #${info.id}")
            }
        }
    }

    private fun readableAccountLabel(accountName: String?, ownerAccount: String?): String {
        val candidate = accountName?.trim()?.takeIf { it.isNotEmpty() } ?: ownerAccount?.trim().orEmpty()
        if (candidate.isEmpty() || !candidate.contains('@')) return ""
        if (candidate.contains("@group.calendar.google.com", ignoreCase = true)) return ""
        val local = candidate.substringBefore('@')
        if (isHexHash(local)) return ""
        return candidate
    }

    private fun isHexHash(value: String): Boolean =
        value.length >= 16 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

    private fun isBetterCalendarName(candidate: CalendarInfo, existing: CalendarInfo): Boolean {
        if (!existing.hasRealName && candidate.hasRealName) return true
        if (existing.hasRealName && !candidate.hasRealName) return false
        return candidate.displayName.length > existing.displayName.length
    }

    private fun matchesViewer(accountType: String, viewer: ViewerApp): Boolean = when (viewer) {
        ViewerApp.GOOGLE -> accountType.contains("google", ignoreCase = true) || accountType.isBlank()
        ViewerApp.SAMSUNG -> accountType.contains("samsung", ignoreCase = true) ||
            accountType.contains("osp", ignoreCase = true) ||
            accountType.equals("LOCAL", ignoreCase = true)
    }

    private fun inferAccountType(ownerAccount: String?): String =
        when {
            ownerAccount.isNullOrBlank() -> ""
            ownerAccount.contains("google", ignoreCase = true) ||
                ownerAccount.contains("@group.calendar.google.com") -> "com.google"
            else -> ""
        }
}
