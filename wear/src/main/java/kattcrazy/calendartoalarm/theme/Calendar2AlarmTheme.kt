package kattcrazy.calendartoalarm.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun Calendar2AlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Calendar2AlarmColorScheme.wearDark,
        content = content,
    )
}
