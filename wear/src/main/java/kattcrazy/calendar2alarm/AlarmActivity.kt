package kattcrazy.calendar2alarm

import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import kattcrazy.calendar2alarm.theme.Calendar2AlarmTheme

class AlarmActivity : ComponentActivity() {
    private var payload: AlarmPayload? = null
    private var screenWakeLock: PowerManager.WakeLock? = null
    private var dismissed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        keepScreenOn()
        acquireScreenWakeLock()

        if (AlarmSuppression.shouldSuppressAlarm(this)) {
            finish()
            return
        }

        val alarmPayload = AlarmExtras.read(intent, this) ?: run {
            finish()
            return
        }
        payload = alarmPayload

        onBackPressedDispatcher.addCallback(this) {
            handleDismiss()
        }

        setContent {
            Calendar2AlarmTheme {
                AlarmScreen(
                    payload = alarmPayload,
                    viewerApp = AppPreferences.viewerApp(this),
                    onDismiss = { handleDismiss() },
                    onSnooze = { handleSnooze(alarmPayload) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AlarmSuppression.shouldSuppressAlarm(this)) {
            handleDismiss(openEvent = false)
            return
        }
        keepScreenOn()
        acquireScreenWakeLock()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        handleDismiss(openEvent = false)
    }

    private fun keepScreenOn() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )
        window.decorView.keepScreenOn = true
    }

    private fun acquireScreenWakeLock() {
        if (screenWakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        screenWakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Calendar2Alarm:AlarmScreen",
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseScreenWakeLock() {
        screenWakeLock?.let { lock ->
            if (lock.isHeld) lock.release()
        }
        screenWakeLock = null
    }

    private fun handleDismiss(openEvent: Boolean = true) {
        if (dismissed) return
        dismissed = true
        val current = payload
        AlarmForegroundService.stop(this)
        if (openEvent && current != null) {
            EventDeepLink.openEvent(this, current)
        }
        finish()
    }

    private fun handleSnooze(payload: AlarmPayload) {
        if (dismissed) return
        dismissed = true
        AlarmForegroundService.stop(this)
        CalendarAlarmScheduler.scheduleSnooze(this, payload)
        finish()
    }

    override fun onDestroy() {
        releaseScreenWakeLock()
        AlarmForegroundService.stop(this)
        super.onDestroy()
    }
}
