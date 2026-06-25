package kattcrazy.calendar2alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kattcrazy.calendar2alarm.theme.Calendar2AlarmTheme

class AlarmSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Calendar2AlarmTheme {
                AlarmSettingsScreen()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AlarmSettingsScreen() {
    val context = LocalContext.current
    var snooze by remember { mutableStateOf(AlarmPreferences.snoozeMinutes(context)) }
    var vibration by remember { mutableStateOf(AlarmPreferences.vibrationPattern(context)) }
    val sounds = remember { AlarmSoundOptions.load(context) }
    var selectedSound by remember {
        mutableStateOf(AlarmPreferences.soundUri(context))
    }
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val previewPlayer = remember { AlarmSoundPlayer(context) }

    AppScaffold {
        ScreenScaffold(scrollState = listState) { padding ->
            TransformingLazyColumn(contentPadding = padding, state = listState) {
                topScrollSpacer(spec)

                item {
                    ListHeader(
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    ) {
                        Text(stringResource(R.string.snooze_label))
                    }
                }
                items(SNOOZE_PRESETS) { minutes ->
                    val isSelected = snooze == minutes
                    Button(
                        onClick = {
                            snooze = minutes
                            AlarmPreferences.setSnoozeMinutes(context, minutes)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = selectionButtonColors(isSelected),
                    ) {
                        Text(
                            "$minutes min",
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    ListHeader(
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    ) {
                        Text(stringResource(R.string.vibration_label))
                    }
                }
                items(VibrationPattern.entries) { pattern ->
                    val isSelected = vibration == pattern
                    Button(
                        onClick = {
                            vibration = pattern
                            AlarmPreferences.setVibrationPattern(context, pattern)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = selectionButtonColors(isSelected),
                    ) {
                        Text(
                            pattern.displayName,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item {
                    Button(
                        onClick = { vibration.play(context, loop = false) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    ) { Text(stringResource(R.string.preview)) }
                }

                item {
                    ListHeader(
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    ) {
                        Text(stringResource(R.string.sound_label))
                    }
                }
                items(sounds) { option ->
                    val uriString = option.uri?.toString()
                    val isSelected = selectedSound == uriString
                    Button(
                        onClick = {
                            selectedSound = uriString
                            AlarmPreferences.setSoundUri(context, uriString)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = selectionButtonColors(isSelected),
                    ) {
                        Text(
                            option.label,
                            textAlign = TextAlign.Start,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item {
                    Button(
                        onClick = { previewPlayer.preview() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    ) { Text(stringResource(R.string.preview)) }
                }

                bottomScrollSpacer(spec)
            }
        }
    }
}
