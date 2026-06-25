package kattcrazy.calendar2alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CalendarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val payload = AlarmExtras.read(intent, context) ?: return
        if (AlarmSuppression.shouldSuppressAlarm(context)) return
        AlarmLauncher.start(context, payload)
    }
}

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val payload = AlarmExtras.read(intent, context) ?: return
        if (AlarmSuppression.shouldSuppressAlarm(context)) return
        AlarmLauncher.start(context, payload)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CalendarAlarmScheduler.resyncAsync(context)
    }
}

class TimezoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CalendarAlarmScheduler.resyncAsync(context)
    }
}
