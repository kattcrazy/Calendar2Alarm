package kattcrazy.calendartoalarm

import android.content.Context
import android.text.format.DateFormat
import java.util.Calendar

object AlarmFormatters {
    fun isEventToday(beginMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
        val begin = Calendar.getInstance().apply { timeInMillis = beginMs }
        val now = Calendar.getInstance().apply { timeInMillis = nowMs }
        return begin.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            begin.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    }

    fun formatEventDate(beginMs: Long): String =
        DateFormat.format("EEE d MMM", beginMs).toString()

    fun formatEventTime(context: Context, beginMs: Long, endMs: Long, allDay: Boolean): String {
        if (allDay) return context.getString(R.string.alarm_all_day)
        val start = DateFormat.getTimeFormat(context).format(beginMs)
        val end = DateFormat.getTimeFormat(context).format(endMs)
        return if (endMs > beginMs && end != start) "$start - $end" else start
    }

    fun formatTimeRange(context: Context, beginMs: Long, endMs: Long, allDay: Boolean): String {
        if (allDay) {
            return DateFormat.format("EEE d MMM", beginMs).toString()
        }
        val date = DateFormat.format("EEE d MMM · ", beginMs).toString()
        val start = DateFormat.getTimeFormat(context).format(beginMs)
        val end = DateFormat.getTimeFormat(context).format(endMs)
        return "$date$start - $end"
    }

    fun formatNextAlarm(context: Context, beginMs: Long, title: String): String {
        val time = DateFormat.getTimeFormat(context).format(beginMs)
        return "$title @ $time"
    }

    fun plainDescription(raw: String): String {
        if (raw.isBlank()) return ""
        return android.text.Html.fromHtml(raw, android.text.Html.FROM_HTML_MODE_COMPACT)
            .toString()
            .trim()
    }
}

fun stableRequestCode(eventId: Long, beginMs: Long, reminderMinutes: Int): Int {
    var result = eventId.hashCode()
    result = 31 * result + beginMs.hashCode()
    result = 31 * result + reminderMinutes
    return result
}

fun todayAt(hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
