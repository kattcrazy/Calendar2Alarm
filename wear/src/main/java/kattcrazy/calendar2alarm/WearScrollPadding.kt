package kattcrazy.calendar2alarm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

private val DefaultListBottomSpacer = 20.dp
private val DefaultListTopSpacer = 16.dp

fun TransformingLazyColumnScope.topScrollSpacer(
    transformationSpec: TransformationSpec,
    height: Dp = DefaultListTopSpacer,
) {
    item(key = "top_scroll_spacer") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .transformedHeight(this, transformationSpec),
        )
    }
}

fun TransformingLazyColumnScope.bottomScrollSpacer(
    transformationSpec: TransformationSpec,
    height: Dp = DefaultListBottomSpacer,
) {
    item(key = "bottom_scroll_spacer") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .transformedHeight(this, transformationSpec),
        )
    }
}
