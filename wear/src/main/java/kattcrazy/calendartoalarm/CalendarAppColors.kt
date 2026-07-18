package kattcrazy.calendartoalarm

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object CalendarAppColors {
    private val google = listOf(
        Color(0xFF7AB4FF),
        Color(0xFF4285F4),
        Color(0xFF174EA6),
    )

    private val samsung = listOf(
        Color(0xFF4DA3FF),
        Color(0xFF0072DE),
        Color(0xFF1428A0),
    )

    /** Same Google/Samsung palette as before, top-to-bottom like the stock alarm screen. */
    fun alarmBackgroundGradient(viewerApp: ViewerApp, size: Size): Brush {
        val colors = when (viewerApp) {
            ViewerApp.GOOGLE -> google
            ViewerApp.SAMSUNG -> samsung
        }
        return Brush.verticalGradient(
            colors = listOf(colors[2], colors[1], colors[0]),
            startY = 0f,
            endY = size.height,
        )
    }
}
