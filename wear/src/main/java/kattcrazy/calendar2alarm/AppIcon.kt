package kattcrazy.calendar2alarm

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.graphics.drawable.toBitmap

@Composable
fun rememberDrawablePainter(drawable: Drawable): Painter {
    return remember(drawable) {
        BitmapPainter(drawable.toBitmap().asImageBitmap())
    }
}

@Composable
fun AppIcon(
    icon: Drawable,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberDrawablePainter(icon),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
