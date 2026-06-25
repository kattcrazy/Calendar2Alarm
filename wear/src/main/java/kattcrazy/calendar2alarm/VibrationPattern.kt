package kattcrazy.calendar2alarm

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class VibrationPattern(val key: String, val displayName: String, private val timings: LongArray) {
    BASIC_CALL("basic", "Basic call", longArrayOf(0, 400, 200, 600)),
    HEARTBEAT("heartbeat", "Heartbeat", longArrayOf(0, 100, 100, 100, 400)),
    TICKTOCK("ticktock", "Ticktock", longArrayOf(0, 80, 120, 80, 120, 400)),
    WALTZ("waltz", "Waltz", longArrayOf(0, 120, 80, 120, 80, 120, 400)),
    ZIG("zig", "Zig-zig-zig", longArrayOf(0, 60, 40, 60, 40, 60, 300)),
    BOUNCE("bounce", "Bounce", longArrayOf(0, 200, 150, 150, 100, 100, 400)),
    RIPPLE("ripple", "Ripple", longArrayOf(0, 80, 60, 120, 80, 160, 400)),
    TELEPHONE("telephone", "Telephone", longArrayOf(0, 800, 400)),
    SIREN("siren", "Siren", longArrayOf(0, 100, 50, 100, 50, 100, 50, 100, 200)),
    SPINNING("spinning", "Spinning", longArrayOf(0, 50, 40, 70, 30, 90, 20, 110, 300)),
    OFF_BEAT("offbeat", "Off-beat", longArrayOf(0, 120, 200, 80, 300, 60, 400)),
    OFF("off", "Off", longArrayOf(0)),
    ;

    fun play(context: Context, loop: Boolean = false) {
        if (this == OFF) return
        val vibrator = vibrator(context) ?: return
        val effect = VibrationEffect.createWaveform(timings, if (loop) 0 else -1)
        vibrator.vibrate(effect)
    }

    fun stop(context: Context) {
        vibrator(context)?.cancel()
    }

    companion object {
        fun fromKey(key: String?): VibrationPattern =
            entries.firstOrNull { it.key == key } ?: BASIC_CALL

        private fun vibrator(context: Context): Vibrator? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }
    }
}
