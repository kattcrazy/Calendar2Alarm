package kattcrazy.calendar2alarm

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kattcrazy.calendar2alarm.theme.Calendar2AlarmTheme

class MainActivity : ComponentActivity() {
    private val refreshState = mutableIntStateOf(0)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        refreshState.intValue++
        CalendarObserver.registerIfPermitted(this)
        if (AppPreferences.isEnabled(this)) CalendarAlarmScheduler.resyncAsync(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            Calendar2AlarmTheme {
                MainScreen(
                    refreshTick = refreshState.intValue,
                    onCalendarApp = {
                        startActivity(Intent(this, CalendarAppActivity::class.java))
                    },
                    onCalendars = {
                        startActivity(Intent(this, CalendarsActivity::class.java))
                    },
                    onAlarmSettings = {
                        startActivity(Intent(this, AlarmSettingsActivity::class.java))
                    },
                    onToggleEnabled = { enabled ->
                        AppPreferences.setEnabled(this, enabled)
                        if (enabled) CalendarAlarmScheduler.resyncAsync(this)
                        else CalendarAlarmScheduler.cancelAll(this)
                        refreshState.intValue++
                    },
                    onToggleClosestReminder = { closestOnly ->
                        AppPreferences.setClosestReminderOnly(this, closestOnly)
                        if (AppPreferences.isEnabled(this)) {
                            CalendarAlarmScheduler.resyncAsync(this)
                        }
                        refreshState.intValue++
                    },
                    onRequestCalendar = {
                        requestPermissionsLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR))
                    },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        } else {
                            SetupIntents.openNotificationSettings(this)
                        }
                    },
                    onTestAlarm = { TestAlarmLauncher.show(this) },
                    onOpenFullScreenIntent = {
                        if (!SetupIntents.openFullScreenIntentSettings(this)) {
                            Toast.makeText(this, R.string.fsi_settings_failed, Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CalendarObserver.registerIfPermitted(this)
        refreshState.intValue++
    }
}

@Composable
private fun MainScreen(
    refreshTick: Int,
    onCalendarApp: () -> Unit,
    onCalendars: () -> Unit,
    onAlarmSettings: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleClosestReminder: (Boolean) -> Unit,
    onRequestCalendar: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenFullScreenIntent: () -> Unit,
    onTestAlarm: () -> Unit,
) {
    val context = LocalContext.current
    val isDebug = remember {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    val hasCalendar = remember(refreshTick) { SetupStatus.hasCalendarPermission(context) }
    val hasNotif = remember(refreshTick) { SetupStatus.hasNotificationPermission(context) }
    val hasExact = remember(refreshTick) { SetupStatus.canScheduleExactAlarms(context) }
    val hasFsi = remember(refreshTick) { SetupStatus.canUseFullScreenIntent(context) }
    val allPermissions = remember(refreshTick) { SetupStatus.hasAllPermissions(context) }
    val enabled = remember(refreshTick) { AppPreferences.isEnabled(context) }
    val closestReminderOnly = remember(refreshTick) { AppPreferences.closestReminderOnly(context) }
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    val privacyPolicyLabel = stringResource(R.string.privacy_policy)

    AppScaffold {
        val listState = rememberTransformingLazyColumnState()
        val transformationSpec = rememberTransformationSpec()
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(
                contentPadding = contentPadding,
                state = listState,
            ) {
                topScrollSpacer(transformationSpec)

                if (!allPermissions) {
                    item {
                        Text(
                            text = stringResource(R.string.permissions_required_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .transformedHeight(this, transformationSpec),
                        )
                    }
                    permissionsSection(
                        context = context,
                        spec = transformationSpec,
                        hasCalendar = hasCalendar,
                        hasNotif = hasNotif,
                        hasExact = hasExact,
                        onRequestCalendar = onRequestCalendar,
                        onRequestNotifications = onRequestNotifications,
                    )
                } else {
                    item {
                        Button(
                            onClick = { onToggleEnabled(!enabled) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            colors = selectionButtonColors(enabled),
                        ) {
                            Text(
                                if (enabled) stringResource(R.string.enable_button) else stringResource(R.string.disable_button),
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = { onToggleClosestReminder(!closestReminderOnly) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            colors = selectionButtonColors(closestReminderOnly),
                        ) {
                            Text(
                                if (closestReminderOnly) {
                                    stringResource(R.string.closest_reminder_on)
                                } else {
                                    stringResource(R.string.closest_reminder_off)
                                },
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = onCalendarApp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) { Text(stringResource(R.string.calendar_app)) }
                    }
                    item {
                        Button(
                            onClick = onCalendars,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) { Text(stringResource(R.string.calendars)) }
                    }
                    item {
                        Button(
                            onClick = onAlarmSettings,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) { Text(stringResource(R.string.alarm_settings)) }
                    }
                    item {
                        PermissionStatusLine(
                            label = stringResource(R.string.status_fsi),
                            granted = hasFsi,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .transformedHeight(this, transformationSpec),
                        )
                    }
                    if (!hasFsi) {
                        item {
                            Text(
                                text = stringResource(R.string.fsi_optional_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .transformedHeight(this, transformationSpec),
                            )
                        }
                        item {
                            Button(
                                onClick = onOpenFullScreenIntent,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, transformationSpec),
                                transformation = SurfaceTransformation(transformationSpec),
                            ) { Text(stringResource(R.string.grant_fsi)) }
                        }
                    }
                }

                if (isDebug) {
                    item {
                        Button(
                            onClick = onTestAlarm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) { Text(stringResource(R.string.test_alarm_button)) }
                    }
                }

                item {
                    CreditFooter(
                        Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    )
                }
                item {
                    Text(
                        text = privacyPolicyLabel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp)
                            .clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl)),
                                )
                            }
                            .transformedHeight(this, transformationSpec),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                bottomScrollSpacer(transformationSpec)
            }
        }
    }
}

private fun TransformingLazyColumnScope.permissionsSection(
    context: android.content.Context,
    spec: TransformationSpec,
    hasCalendar: Boolean,
    hasNotif: Boolean,
    hasExact: Boolean,
    onRequestCalendar: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    item {
        ListHeader(
            modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
            transformation = SurfaceTransformation(spec),
        ) {
            Text(stringResource(R.string.permissions))
        }
    }
    if (!hasCalendar) {
        item {
            Button(
                onClick = onRequestCalendar,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
            ) { Text(stringResource(R.string.grant_calendar)) }
        }
    }
    if (!hasNotif) {
        item {
            Button(
                onClick = onRequestNotifications,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
            ) { Text(stringResource(R.string.grant_notifications)) }
        }
    }
    if (!hasExact) {
        item {
            Button(
                onClick = { SetupIntents.openExactAlarmSettings(context) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
            ) { Text(stringResource(R.string.grant_exact_alarms)) }
        }
    }
}

@Composable
private fun PermissionStatusLine(
    label: String,
    granted: Boolean,
    modifier: Modifier = Modifier,
) {
    val value = stringResource(if (granted) R.string.status_yes else R.string.status_no)
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
fun CreditFooter(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.credit_by),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(top = 8.dp),
    )
    Text(
        text = stringResource(R.string.credit_repo),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}
