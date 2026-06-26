package kattcrazy.calendar2alarm

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

private enum class PulseKind { S, M, L }

private enum class PauseKind { SHORT, MED, GROUP, CYCLE }

private data class VibPulse(val kind: PulseKind)

private data class VibPause(val kind: PauseKind)

private fun pulseDuration(kind: PulseKind): Long = when (kind) {
    PulseKind.S -> 22L
    PulseKind.M -> 115L
    PulseKind.L -> 310L
}

private fun pulseAmplitude(kind: PulseKind): Int = when (kind) {
    PulseKind.S -> 36
    PulseKind.M -> 44
    PulseKind.L -> 50
}

private fun pauseDuration(kind: PauseKind): Long = when (kind) {
    PauseKind.SHORT -> 26L
    PauseKind.MED -> 72L
    PauseKind.GROUP -> 125L
    PauseKind.CYCLE -> 165L
}

private fun vibPattern(vararg steps: Any): Pair<LongArray, IntArray> {
    val timings = mutableListOf(0L)
    val amplitudes = mutableListOf(0)
    for (step in steps) {
        when (step) {
            is VibPulse -> {
                timings.add(pulseDuration(step.kind))
                amplitudes.add(pulseAmplitude(step.kind))
            }
            is VibPause -> {
                timings.add(pauseDuration(step.kind))
                amplitudes.add(0)
            }
        }
    }
    return timings.toLongArray() to amplitudes.toIntArray()
}

enum class VibrationPattern(
    val key: String,
    val displayName: String,
    private val timings: LongArray,
    private val amplitudes: IntArray,
) {
    // S S S , S S S
    BASIC_CALL(
        "basic_call",
        "Basic call",
        vibPattern(
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.GROUP),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.CYCLE),
        ),
    ),
    // L
    SIMPLE(
        "simple",
        "Simple",
        vibPattern(VibPulse(PulseKind.L), VibPause(PauseKind.CYCLE)),
    ),
    // M L , M L
    ZIG_ZIG(
        "zig_zig",
        "Zig-zig",
        vibPattern(
            VibPulse(PulseKind.M), VibPause(PauseKind.MED),
            VibPulse(PulseKind.L), VibPause(PauseKind.GROUP),
            VibPulse(PulseKind.M), VibPause(PauseKind.MED),
            VibPulse(PulseKind.L), VibPause(PauseKind.CYCLE),
        ),
    ),
    // M L , M L , M L
    ZIG_ZIG_ZIG(
        "zig_zig_zig",
        "Zig-zig-zig",
        vibPattern(
            VibPulse(PulseKind.M), VibPause(PauseKind.MED),
            VibPulse(PulseKind.L), VibPause(PauseKind.GROUP),
            VibPulse(PulseKind.M), VibPause(PauseKind.MED),
            VibPulse(PulseKind.L), VibPause(PauseKind.GROUP),
            VibPulse(PulseKind.M), VibPause(PauseKind.MED),
            VibPulse(PulseKind.L), VibPause(PauseKind.CYCLE),
        ),
    ),
    // S S L
    TAP(
        "tap",
        "Tap",
        vibPattern(
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.MED),
            VibPulse(PulseKind.L), VibPause(PauseKind.CYCLE),
        ),
    ),
    // S S S L
    KNOCK(
        "knock",
        "Knock",
        vibPattern(
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.MED),
            VibPulse(PulseKind.L), VibPause(PauseKind.CYCLE),
        ),
    ),
    // S S , S S
    HEARTBEAT(
        "heartbeat",
        "Heartbeat",
        vibPattern(
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.GROUP),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.CYCLE),
        ),
    ),
    // S , S S , S , S S
    BOUNCE(
        "bounce",
        "Bounce",
        vibPattern(
            VibPulse(PulseKind.S), VibPause(PauseKind.GROUP),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.GROUP),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.CYCLE),
        ),
    ),
    // rapid all-short wobble
    DUBSTEP(
        "dubstep",
        "Dubstep",
        vibPattern(
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.MED),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.CYCLE),
        ),
    ),
    // triplet gallop, all short
    GALLOP(
        "gallop",
        "Gallop",
        vibPattern(
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.GROUP),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.SHORT),
            VibPulse(PulseKind.S), VibPause(PauseKind.CYCLE),
        ),
    ),
    OFF(
        "off",
        "Off",
        longArrayOf(0),
        intArrayOf(0),
    ),
    ;

    constructor(
        key: String,
        displayName: String,
        pattern: Pair<LongArray, IntArray>,
    ) : this(key, displayName, pattern.first, pattern.second)

    fun play(context: Context, loop: Boolean = false) {
        if (this == OFF) return
        val vibrator = vibrator(context) ?: return
        val effect = VibrationEffect.createWaveform(timings, amplitudes, if (loop) 0 else -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(
                effect,
                VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build(),
            )
        } else {
            vibrator.vibrate(effect)
        }
    }

    fun stop(context: Context) {
        vibrator(context)?.cancel()
    }

    companion object {
        private val LEGACY_KEYS = mapOf(
            "basic" to BASIC_CALL,
            "ticktock" to TAP,
            "waltz" to ZIG_ZIG_ZIG,
            "zig" to ZIG_ZIG_ZIG,
            "ripple" to BOUNCE,
            "telephone" to SIMPLE,
            "siren" to DUBSTEP,
            "spinning" to GALLOP,
            "offbeat" to GALLOP,
        )

        fun fromKey(key: String?): VibrationPattern =
            LEGACY_KEYS[key] ?: entries.firstOrNull { it.key == key } ?: BASIC_CALL

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
