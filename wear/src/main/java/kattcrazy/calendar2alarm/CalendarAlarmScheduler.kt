package kattcrazy.calendar2alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object ScheduledAlarmStore {
    private const val PREFS = "scheduled_alarms"
    private const val KEY_CODES = "codes"
    private const val KEY_NEXT = "next_alarm"

    fun save(context: Context, payloads: List<AlarmPayload>) {
        val arr = JSONArray()
        payloads.forEach { p ->
            arr.put(
                JSONObject()
                    .put("rc", p.requestCode)
                    .put("title", p.title)
                    .put("begin", p.beginMs)
                    .put("eventId", p.eventId)
                    .put("end", p.endMs)
                    .put("desc", p.description)
                    .put("allDay", p.allDay)
                    .put("remMin", p.reminderMinutes),
            )
        }
        val next = payloads.minByOrNull { it.beginMs - it.reminderMinutes * 60_000L }
        prefs(context).edit()
            .putString(KEY_CODES, arr.toString())
            .putString(
                KEY_NEXT,
                next?.let { "${it.title}|${it.beginMs - it.reminderMinutes * 60_000L}" },
            )
            .apply()
    }

    fun loadPayload(context: Context, requestCode: Int): AlarmPayload? {
        val arr = JSONArray(prefs(context).getString(KEY_CODES, "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getInt("rc") == requestCode) {
                return AlarmPayload(
                    eventId = o.getLong("eventId"),
                    title = o.getString("title"),
                    description = o.optString("desc", ""),
                    beginMs = o.getLong("begin"),
                    endMs = o.getLong("end"),
                    allDay = o.optBoolean("allDay", false),
                    reminderMinutes = o.optInt("remMin", 0),
                    requestCode = requestCode,
                )
            }
        }
        return null
    }

    fun nextAlarmSummary(context: Context): String? {
        val raw = prefs(context).getString(KEY_NEXT, null) ?: return null
        val parts = raw.split("|", limit = 2)
        if (parts.size != 2) return null
        val begin = parts[1].toLongOrNull() ?: return null
        return AlarmFormatters.formatNextAlarm(context, begin, parts[0])
    }

    fun allRequestCodes(context: Context): Set<Int> {
        val arr = JSONArray(prefs(context).getString(KEY_CODES, "[]"))
        return buildSet {
            for (i in 0 until arr.length()) add(arr.getJSONObject(i).getInt("rc"))
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

object CalendarAlarmScheduler {
    private const val TAG = "Calendar2Alarm"

    fun resyncAsync(context: Context, onComplete: ((SyncResult) -> Unit)? = null) {
        Thread {
            val result = runCatching { resync(context.applicationContext) }
                .getOrElse { SyncResult.Error(it.message ?: "Sync failed") }
            onComplete?.let { callback ->
                Handler(Looper.getMainLooper()).post { callback(result) }
            }
        }.start()
    }

    fun resync(context: Context): SyncResult {
        if (!AppPreferences.isEnabled(context)) {
            cancelAll(context)
            return SyncResult.NotEnabled
        }
        if (!SetupStatus.isReady(context)) {
            val reason = SetupStatus.notReadyReason(context) ?: "Not ready"
            Log.w(TAG, "Not ready for scheduling: $reason")
            return SyncResult.Error(reason)
        }

        return try {
            val appContext = context.applicationContext
            val payloads = CalendarRepository.loadUpcomingAlarms(appContext)
            val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val existing = ScheduledAlarmStore.allRequestCodes(appContext)
            val newCodes = payloads.map { it.requestCode }.toSet()

            for (code in existing - newCodes) {
                am.cancel(buildPendingIntent(appContext, code, snooze = false))
            }

            for (payload in payloads) {
                val trigger = payload.beginMs - payload.reminderMinutes * 60_000L
                val pi = buildPendingIntent(appContext, payload.requestCode, snooze = false)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }

            ScheduledAlarmStore.save(appContext, payloads)
            AppPreferences.setLastResync(appContext, System.currentTimeMillis(), payloads.size)
            Log.i(TAG, "Scheduled ${payloads.size} alarms")
            SyncResult.Success(payloads.size)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }

    fun scheduleSnooze(context: Context, payload: AlarmPayload) {
        val minutes = AlarmPreferences.snoozeMinutes(context)
        val trigger = System.currentTimeMillis() + minutes * 60_000L
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, payload.requestCode, snooze = true, payload = payload)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ScheduledAlarmStore.allRequestCodes(context).forEach { code ->
            am.cancel(buildPendingIntent(context, code, snooze = false))
        }
        ScheduledAlarmStore.save(context, emptyList())
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        snooze: Boolean,
        payload: AlarmPayload? = null,
    ): PendingIntent {
        val cls = if (snooze) SnoozeReceiver::class.java else CalendarAlarmReceiver::class.java
        val intent = Intent(context, cls).apply {
            putExtra(AlarmExtras.REQUEST_CODE, requestCode)
            payload?.let { AlarmExtras.write(this, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

object AlarmExtras {
    const val REQUEST_CODE = "request_code"
    const val EVENT_ID = "event_id"
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val BEGIN = "begin"
    const val END = "end"
    const val ALL_DAY = "all_day"
    const val REM_MIN = "rem_min"
    const val IS_TEST = "is_test"

    fun write(intent: Intent, payload: AlarmPayload) {
        intent.putExtra(EVENT_ID, payload.eventId)
        intent.putExtra(TITLE, payload.title)
        intent.putExtra(DESCRIPTION, payload.description)
        intent.putExtra(BEGIN, payload.beginMs)
        intent.putExtra(END, payload.endMs)
        intent.putExtra(ALL_DAY, payload.allDay)
        intent.putExtra(REM_MIN, payload.reminderMinutes)
        intent.putExtra(IS_TEST, payload.isTest)
        intent.putExtra(REQUEST_CODE, payload.requestCode)
    }

    fun read(intent: Intent, context: Context): AlarmPayload? {
        if (intent.hasExtra(TITLE)) {
            return AlarmPayload(
                eventId = intent.getLongExtra(EVENT_ID, -1L),
                title = intent.getStringExtra(TITLE) ?: "Event",
                description = intent.getStringExtra(DESCRIPTION) ?: "",
                beginMs = intent.getLongExtra(BEGIN, 0L),
                endMs = intent.getLongExtra(END, 0L),
                allDay = intent.getBooleanExtra(ALL_DAY, false),
                reminderMinutes = intent.getIntExtra(REM_MIN, 0),
                requestCode = intent.getIntExtra(REQUEST_CODE, 0),
                isTest = intent.getBooleanExtra(IS_TEST, false),
            )
        }
        val code = intent.getIntExtra(REQUEST_CODE, -1)
        if (code == -1) return null
        return ScheduledAlarmStore.loadPayload(context, code)
    }
}
