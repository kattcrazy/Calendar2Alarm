package kattcrazy.calendartoalarm.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme as WearColorScheme

object Calendar2AlarmColorScheme {
    private val dark: ColorScheme = darkColorScheme(
        primary = Color(0xFFACC7FF),
        onPrimary = Color(0xFF002F67),
        primaryContainer = Color(0xFF004492),
        onPrimaryContainer = Color(0xFFD7E2FF),
        secondary = Color(0xFFBEC6DC),
        onSecondary = Color(0xFF283041),
        secondaryContainer = Color(0xFF3F4759),
        onSecondaryContainer = Color(0xFFDAE2F9),
        tertiary = Color(0xFFDEBCDF),
        onTertiary = Color(0xFF402844),
        tertiaryContainer = Color(0xFF573E5B),
        onTertiaryContainer = Color(0xFFFBD7FC),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF1B1B1F),
        onBackground = Color(0xFFE3E2E6),
        surface = Color(0xFF1B1B1F),
        onSurface = Color(0xFFE3E2E6),
        surfaceVariant = Color(0xFF44474E),
        onSurfaceVariant = Color(0xFFC4C6D0),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474E),
        inverseSurface = Color(0xFFE3E2E6),
        inverseOnSurface = Color(0xFF2F3033),
        inversePrimary = Color(0xFF235CB1),
    )

    val wearDark: WearColorScheme = dark
        .copy(background = Color.Black, surface = Color.Black)
        .toWearColorScheme()

    val syncSuccessContainer = Color(0xFF6BAA80)
    val syncSuccessOn = Color(0xFFE8F5EB)
}

private fun ColorScheme.toWearColorScheme(): WearColorScheme =
    WearColorScheme(
        primary = primary,
        primaryDim = primaryContainer,
        primaryContainer = primaryContainer,
        onPrimary = onPrimary,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        secondaryDim = secondaryContainer,
        secondaryContainer = secondaryContainer,
        onSecondary = onSecondary,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        tertiaryDim = tertiaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiary = onTertiary,
        onTertiaryContainer = onTertiaryContainer,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceVariant,
        surfaceContainerHigh = surfaceVariant,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        background = background,
        onBackground = onBackground,
        error = error,
        errorDim = errorContainer,
        errorContainer = errorContainer,
        onError = onError,
        onErrorContainer = onErrorContainer,
    )
