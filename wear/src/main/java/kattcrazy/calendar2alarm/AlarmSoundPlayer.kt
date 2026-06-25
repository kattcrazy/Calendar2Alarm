package kattcrazy.calendar2alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.PowerManager

class AlarmSoundPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    fun start() {
        val uri = resolveSoundUri()
        if (uri != null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        }
        AlarmPreferences.vibrationPattern(context).play(context, loop = true)
        acquireWakeLock()
    }

    fun stop() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        AlarmPreferences.vibrationPattern(context).stop(context)
        releaseWakeLock()
    }

    fun preview(seconds: Int = 3) {
        val uri = resolveSoundUri() ?: return
        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(context, uri)
            prepare()
            start()
        }
        android.os.Handler(context.mainLooper).postDelayed({
            runCatching { player.stop() }
            runCatching { player.release() }
        }, seconds * 1000L)
    }

    private fun resolveSoundUri(): Uri? {
        val saved = AlarmPreferences.soundUri(context)
        if (saved == null) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        if (saved == SILENT_URI) return null
        return Uri.parse(saved)
    }

    private fun acquireWakeLock() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Calendar2Alarm:AlarmWakeLock",
        ).apply { acquire(10 * 60 * 1000L) }
    }

    private fun releaseWakeLock() {
        runCatching { wakeLock?.release() }
        wakeLock = null
    }

    companion object {
        const val SILENT_URI = "silent://"
    }
}

object AlarmSoundOptions {
    fun load(context: Context): List<AlarmSoundOption> {
        val options = mutableListOf<AlarmSoundOption>()
        options.add(AlarmSoundOption("Default alarm", null))
        options.add(AlarmSoundOption("Silent", Uri.parse(AlarmSoundPlayer.SILENT_URI)))

        val manager = RingtoneManager(context).apply {
            setType(RingtoneManager.TYPE_ALARM)
        }
        manager.cursor?.use { cursor ->
            var count = 0
            while (cursor.moveToNext() && count < 8) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = manager.getRingtoneUri(cursor.position)
                options.add(AlarmSoundOption(title, uri))
                count++
            }
        }
        return options
    }
}
