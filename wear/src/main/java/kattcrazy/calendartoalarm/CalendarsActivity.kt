package kattcrazy.calendartoalarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kattcrazy.calendartoalarm.theme.Calendar2AlarmTheme

class CalendarsActivity : ComponentActivity() {
    private val refreshState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Calendar2AlarmTheme {
                CalendarsScreen(refreshTick = refreshState.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState.intValue++
    }
}

private data class CalendarCardText(
    val primary: String,
    val secondary: String? = null,
)

@androidx.compose.runtime.Composable
private fun CalendarsScreen(refreshTick: Int) {
    val context = LocalContext.current
    val hasCalendar = remember(refreshTick) { SetupStatus.hasCalendarPermission(context) }
    val viewer = remember(refreshTick) { AppPreferences.viewerApp(context) }
    val viewerLabel = stringResource(viewer.labelRes)
    val calendars = remember(refreshTick) {
        if (hasCalendar) CalendarRepository.listVisibleCalendars(context) else emptyList()
    }
    var selectedIds by remember(refreshTick) {
        mutableStateOf(AppPreferences.selectedCalendarIds(context))
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
                        Text(stringResource(R.string.calendars))
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
                } else if (calendars.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_calendars_on_watch, viewerLabel),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).transformedHeight(this, spec),
                        )
                    }
                    if (viewer.isInstalled(context)) {
                        item {
                            Button(
                                onClick = { SetupIntents.openCalendarApp(context, viewer) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, spec),
                                transformation = SurfaceTransformation(spec),
                            ) {
                                Text(stringResource(R.string.open_calendar_app, viewerLabel))
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            stringResource(R.string.calendars_list_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .transformedHeight(this, spec),
                        )
                    }
                    items(calendars, key = { it.id }) { calendar ->
                        val isSelected = calendar.id in selectedIds
                        val cardText = remember(calendar.id, refreshTick) {
                            calendarCardText(context, calendar)
                        }

                        Button(
                            onClick = {
                                val updated = AppPreferences.toggleCalendarId(
                                    context,
                                    calendar.id,
                                    selected = !isSelected,
                                )
                                selectedIds = updated
                                if (AppPreferences.isEnabled(context)) {
                                    CalendarAlarmScheduler.resyncAsync(context)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .transformedHeight(this, spec),
                            transformation = SurfaceTransformation(spec),
                            colors = selectionButtonColors(isSelected),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = cardText.primary,
                                    textAlign = TextAlign.Start,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (!cardText.secondary.isNullOrBlank()) {
                                    Text(
                                        text = cardText.secondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Start,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }

                bottomScrollSpacer(spec)
            }
        }
    }
}

private fun calendarCardText(context: android.content.Context, calendar: CalendarInfo): CalendarCardText {
    if (calendar.hasRealName) {
        return CalendarCardText(primary = calendarListLabel(context, calendar))
    }
    val nextTitle = CalendarRepository.sampleUpcomingEvents(context, calendar.id, limit = 1)
        .firstOrNull()
        ?.title
        ?.takeIf { it.isNotBlank() }
    return CalendarCardText(
        primary = context.getString(R.string.calendar_unnamed, calendar.id),
        secondary = nextTitle,
    )
}

private fun calendarListLabel(context: android.content.Context, calendar: CalendarInfo): String {
    val account = calendar.accountName
    if (account.isBlank()) return calendar.displayName
    val emailLocal = account.substringBefore('@')
    return if (emailLocal.isNotBlank() && emailLocal != calendar.displayName) {
        "${calendar.displayName} · $emailLocal"
    } else {
        calendar.displayName
    }
}
