package kattcrazy.calendar2alarm

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.wear.provider.WearableCalendarContract

class Calendar2AlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CalendarObserver.registerIfPermitted(this)
    }
}

object CalendarObserver {
    private var registered = false
    private var pending = false

    fun registerIfPermitted(context: android.content.Context) {
        if (registered || !SetupStatus.hasCalendarPermission(context)) return
        registered = true
        val appContext = context.applicationContext
        appContext.contentResolver.registerContentObserver(
            WearableCalendarContract.CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    if (pending) return
                    pending = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        pending = false
                        CalendarAlarmScheduler.resyncAsync(appContext)
                    }, 2500L)
                }
            },
        )
    }
}
