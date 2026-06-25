package kattcrazy.calendar2alarm

data class AlarmPayload(
    val eventId: Long,
    val title: String,
    val description: String,
    val beginMs: Long,
    val endMs: Long,
    val allDay: Boolean,
    val reminderMinutes: Int,
    val requestCode: Int,
    val isTest: Boolean = false,
) {
    fun toSummaryTime(context: android.content.Context): String =
        AlarmFormatters.formatTimeRange(context, beginMs, endMs, allDay)
}
