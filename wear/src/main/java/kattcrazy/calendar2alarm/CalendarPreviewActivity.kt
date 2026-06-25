package kattcrazy.calendar2alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
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
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kattcrazy.calendar2alarm.theme.Calendar2AlarmTheme

class CalendarPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val calendarId = intent.getLongExtra(EXTRA_CALENDAR_ID, -1L)
        if (calendarId < 0L) {
            finish()
            return
        }
        setContent {
            Calendar2AlarmTheme {
                CalendarPreviewScreen(calendarId = calendarId)
            }
        }
    }

    companion object {
        const val EXTRA_CALENDAR_ID = "calendar_id"

        fun intent(context: Context, calendarId: Long): Intent =
            Intent(context, CalendarPreviewActivity::class.java).apply {
                putExtra(EXTRA_CALENDAR_ID, calendarId)
            }
    }
}

@androidx.compose.runtime.Composable
private fun CalendarPreviewScreen(calendarId: Long) {
    val context = LocalContext.current
    val events = remember(calendarId) {
        CalendarRepository.sampleUpcomingEvents(context, calendarId, limit = 6)
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
                        Text(stringResource(R.string.calendar_unnamed, calendarId))
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.calendar_preview_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .transformedHeight(this, spec),
                    )
                }

                if (events.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.calendar_preview_empty),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .transformedHeight(this, spec),
                        )
                    }
                } else {
                    items(events, key = { "${it.title}|${it.beginMs}" }) { event ->
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .transformedHeight(this, spec),
                        )
                        Text(
                            text = AlarmFormatters.formatTimeRange(
                                context,
                                event.beginMs,
                                event.endMs,
                                event.allDay,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 10.dp)
                                .transformedHeight(this, spec),
                        )
                    }
                }

                bottomScrollSpacer(spec)
            }
        }
    }
}
