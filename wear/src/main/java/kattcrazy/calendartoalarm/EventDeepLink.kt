package kattcrazy.calendartoalarm

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast

object EventDeepLink {
    fun openEvent(context: Context, payload: AlarmPayload): Boolean {
        if (payload.isTest || payload.eventId < 0) return false
        val viewer = AppPreferences.viewerApp(context)
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, payload.eventId)
        val extras = fun(intent: Intent) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, payload.beginMs)
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, payload.endMs)
            intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, payload.allDay)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val targeted = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            extras(this)
            setPackage(viewer.packageName)
        }
        if (targeted.resolveActivity(context.packageManager) != null) {
            context.startActivity(targeted)
            return true
        }
        val generic = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            extras(this)
        }
        if (generic.resolveActivity(context.packageManager) != null) {
            context.startActivity(generic)
            return true
        }
        if (viewer.isInstalled(context)) {
            SetupIntents.openCalendarApp(context, viewer)
            return true
        }
        Toast.makeText(context, R.string.open_event_failed, Toast.LENGTH_SHORT).show()
        return false
    }
}
