package kattcrazy.calendar2alarm

import android.content.Context
import android.net.Uri

object AlarmPreferences {
    private const val PREFS = "alarm_prefs"
    private const val KEY_SNOOZE = "snooze_minutes"
    private const val KEY_SOUND = "sound_uri"
    private const val KEY_VIBRATION = "vibration"

    fun snoozeMinutes(context: Context): Int =
        prefs(context).getInt(KEY_SNOOZE, 10)

    fun setSnoozeMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_SNOOZE, minutes).apply()
    }

    fun soundUri(context: Context): String? =
        prefs(context).getString(KEY_SOUND, null)

    fun setSoundUri(context: Context, uri: String?) {
        prefs(context).edit().putString(KEY_SOUND, uri).apply()
    }

    fun vibrationPattern(context: Context): VibrationPattern =
        VibrationPattern.fromKey(prefs(context).getString(KEY_VIBRATION, VibrationPattern.BASIC_CALL.key))

    fun setVibrationPattern(context: Context, pattern: VibrationPattern) {
        prefs(context).edit().putString(KEY_VIBRATION, pattern.key).apply()
    }

    fun summary(context: Context): String {
        val snooze = snoozeMinutes(context)
        val vib = vibrationPattern(context).displayName
        return "$snooze min · $vib"
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

val SNOOZE_PRESETS = listOf(3, 5, 10, 15, 20, 30)

data class AlarmSoundOption(val label: String, val uri: Uri?)
