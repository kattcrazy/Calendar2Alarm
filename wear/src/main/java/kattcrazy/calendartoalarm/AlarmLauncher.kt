package kattcrazy.calendartoalarm

import android.content.Context
import android.content.Intent

object AlarmLauncher {
    fun start(context: Context, payload: AlarmPayload) {
        val appContext = context.applicationContext
        if (AlarmSuppression.shouldSuppressAlarm(appContext)) return
        AlarmForegroundService.start(appContext, payload)
        val intent = Intent(appContext, AlarmActivity::class.java).apply {
            AlarmExtras.write(this, payload)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        appContext.startActivity(intent)
    }
}

object TestAlarmLauncher {
    fun show(context: Context) {
        val begin = todayAt(14, 0)
        val end = begin + 60 * 60 * 1000
        AlarmLauncher.start(
            context,
            AlarmPayload(
                eventId = -1L,
                title = context.getString(R.string.test_event_title),
                description = context.getString(R.string.test_event_desc),
                beginMs = begin,
                endMs = end,
                allDay = false,
                reminderMinutes = 10,
                requestCode = 999_001,
                isTest = true,
            ),
        )
    }
}
