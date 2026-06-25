package kattcrazy.calendar2alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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

class CalendarAppActivity : ComponentActivity() {
    private val refreshState = androidx.compose.runtime.mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Calendar2AlarmTheme {
                CalendarAppScreen(refreshTick = refreshState.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState.intValue++
    }
}

@androidx.compose.runtime.Composable
private fun CalendarAppScreen(refreshTick: Int) {
    val context = LocalContext.current
    val hasCalendar = remember(refreshTick) { SetupStatus.hasCalendarPermission(context) }
    var currentViewer by remember(refreshTick) { mutableStateOf(AppPreferences.viewerApp(context)) }
    val installedApps = remember(refreshTick) {
        ViewerApp.entries.filter { it.isInstalled(context) }
    }
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()

    AppScaffold {
        ScreenScaffold(scrollState = listState) { padding ->
            TransformingLazyColumn(contentPadding = padding, state = listState) {
                topScrollSpacer(spec)

                item {
                    ListHeader(
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    ) {
                        Text(stringResource(R.string.calendar_app))
                    }
                }

                if (!hasCalendar) {
                    item {
                        Text(
                            stringResource(R.string.calendars_need_permission),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).transformedHeight(this, spec),
                        )
                    }
                } else if (installedApps.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_calendar_apps_installed),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .transformedHeight(this, spec),
                        )
                    }
                } else {
                    items(installedApps, key = { it.key }) { option ->
                        val isSelected = currentViewer == option
                        val icon = remember(option.packageName) {
                            context.packageManager.getApplicationIcon(option.packageName)
                        }
                        Button(
                            onClick = {
                                AppPreferences.applyViewerApp(context, option)
                                currentViewer = option
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, spec),
                            transformation = SurfaceTransformation(spec),
                            colors = selectionButtonColors(isSelected),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                AppIcon(
                                    icon = icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .padding(end = 8.dp),
                                )
                                Text(
                                    text = stringResource(option.labelRes),
                                    textAlign = TextAlign.Start,
                                )
                            }
                        }
                    }
                }

                bottomScrollSpacer(spec)
            }
        }
    }
}
