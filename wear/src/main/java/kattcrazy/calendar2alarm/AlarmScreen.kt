package kattcrazy.calendar2alarm

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

private const val SWIPE_DISMISS_THRESHOLD_DP = 72f
private const val EXPAND_PHASE_FRACTION = 0.35f
private const val VANISH_START_FRACTION = 0.3f
private const val MAX_RING_SCALE = 3f
private val ButtonOuterBase = 76.dp
private val ButtonInnerSize = 62.dp
private val ButtonMidRingSize = (ButtonInnerSize + ButtonOuterBase) / 2
private val ButtonIconSize = 30.dp
private val ButtonHitSize = ButtonOuterBase
private val PulseGrow = 12.dp
private val RingOverlaySize = ButtonOuterBase + PulseGrow * 2
private val ButtonGap = 36.dp - ButtonHitSize / 2

@Composable
fun AlarmScreen(
    payload: AlarmPayload,
    viewerApp: ViewerApp,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val context = LocalContext.current
    val timeLine = remember(payload.beginMs, payload.endMs, payload.allDay) {
        AlarmFormatters.formatEventTime(context, payload.beginMs, payload.endMs, payload.allDay)
    }
    val description = remember(payload.description) {
        AlarmFormatters.plainDescription(payload.description)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(CalendarAppColors.alarmBackgroundGradient(viewerApp, size))
            },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 42.dp, bottom = 92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = timeLine,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = payload.title,
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SwipeableAlarmButtons(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            onDismiss = onDismiss,
            onSnooze = onSnooze,
        )
    }
}

@Composable
private fun SwipeableAlarmButtons(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularAlarmButton(
            icon = Icons.Filled.Close,
            contentDescription = stringResource(R.string.alarm_dismiss),
            onClick = onDismiss,
            innerColor = Color.White,
            innerAlphaBase = 0.38f,
            showMidRing = true,
            onSwipeDismiss = onDismiss,
        )
        CircularAlarmButton(
            icon = Icons.Filled.Snooze,
            contentDescription = stringResource(R.string.alarm_snooze),
            onClick = onSnooze,
            innerColor = Color(0xFF2A2A2A),
            innerAlphaBase = 0.18f,
        )
    }
}

@Composable
private fun CircularAlarmButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    innerColor: Color,
    innerAlphaBase: Float,
    showMidRing: Boolean = false,
    onSwipeDismiss: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val thresholdPx = with(density) { SWIPE_DISMISS_THRESHOLD_DP.dp.toPx() }
    val innerRadiusPx = with(density) { (ButtonInnerSize / 2).toPx() }
    val outerRadiusPx = with(density) { (ButtonOuterBase / 2).toPx() }
    val pulseGrowPx = with(density) { PulseGrow.toPx() }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var gesturePressed by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val clickablePressed by interactionSource.collectIsPressedAsState()
    val pressed = gesturePressed || clickablePressed
    val pressBoost by animateFloatAsState(
        targetValue = if (pressed) 0.18f else 0f,
        animationSpec = tween(120),
        label = "pressBoost",
    )
    val swipeProgress = if (onSwipeDismiss != null) {
        (hypot(dragX.toDouble(), dragY.toDouble()) / thresholdPx).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val interactionProgress = maxOf(swipeProgress, pressBoost)
    val isInteracting = interactionProgress > 0.01f

    val ringScale = if (interactionProgress <= EXPAND_PHASE_FRACTION) {
        1f + (interactionProgress / EXPAND_PHASE_FRACTION) * (MAX_RING_SCALE - 1f)
    } else {
        MAX_RING_SCALE
    }
    val innerHoleFraction = if (interactionProgress <= VANISH_START_FRACTION) {
        0f
    } else {
        ((interactionProgress - VANISH_START_FRACTION) / (1f - VANISH_START_FRACTION))
            .coerceIn(0f, 1f)
    }
    val interactionAlpha = 0.14f + 0.1f * interactionProgress.coerceAtMost(VANISH_START_FRACTION)
    val innerAlpha = innerAlphaBase + 0.06f * interactionProgress.coerceAtMost(EXPAND_PHASE_FRACTION)

    Box(
        modifier = Modifier
            .requiredSize(ButtonHitSize)
            .graphicsLayer { clip = false }
            .then(
                if (onSwipeDismiss != null) {
                    Modifier.pointerInput(onClick, onSwipeDismiss, thresholdPx) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            gesturePressed = true
                            dragX = 0f
                            dragY = 0f
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    val dist = hypot(dragX.toDouble(), dragY.toDouble())
                                    when {
                                        dist >= thresholdPx -> onSwipeDismiss()
                                        dist < slop -> onClick()
                                    }
                                    gesturePressed = false
                                    dragX = 0f
                                    dragY = 0f
                                    break
                                }
                                if (change.positionChanged()) {
                                    val delta = change.position - change.previousPosition
                                    dragX += delta.x
                                    dragY += delta.y
                                    change.consume()
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(RingOverlaySize)
                .align(Alignment.Center)
                .graphicsLayer { clip = false },
            contentAlignment = Alignment.Center,
        ) {
            if (!isInteracting) {
                PulsingOuterRings(
                    innerRadiusPx = innerRadiusPx,
                    outerRadiusPx = outerRadiusPx,
                    pulseGrowPx = pulseGrowPx,
                )
            }
            if (isInteracting) {
                InteractionOuterRing(
                    outerRadiusPx = outerRadiusPx,
                    ringScale = ringScale,
                    innerHoleFraction = innerHoleFraction,
                    alpha = interactionAlpha,
                )
            }
        }
        if (showMidRing) {
            Box(
                modifier = Modifier
                    .size(ButtonMidRingSize)
                    .border(
                        width = 3.dp,
                        color = Color.White.copy(alpha = 0.7f),
                        shape = CircleShape,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(ButtonInnerSize)
                .clip(CircleShape)
                .background(innerColor.copy(alpha = innerAlpha))
                .then(
                    if (onSwipeDismiss == null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(ButtonIconSize),
            )
        }
    }
}

@Composable
private fun PulsingOuterRings(
    innerRadiusPx: Float,
    outerRadiusPx: Float,
    pulseGrowPx: Float,
) {
    val transition = rememberInfiniteTransition(label = "ringPulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0),
        ),
        label = "pulse",
    )
    val bandOuterRadius = innerRadiusPx +
        (outerRadiusPx - innerRadiusPx + pulseGrowPx) * progress
    val alpha = progress * (1f - progress) * 0.5f
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (alpha <= 0f || bandOuterRadius <= innerRadiusPx + 0.5f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = (bandOuterRadius - innerRadiusPx).coerceAtLeast(1f)
        val strokeRadius = innerRadiusPx + strokeWidth / 2f
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = strokeRadius,
            center = center,
            style = Stroke(width = strokeWidth),
        )
    }
}

@Composable
private fun InteractionOuterRing(
    outerRadiusPx: Float,
    ringScale: Float,
    innerHoleFraction: Float,
    alpha: Float,
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = ringScale
                scaleY = ringScale
                compositingStrategy = CompositingStrategy.Offscreen
            },
    ) {
        if (alpha <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = outerRadiusPx,
            center = center,
        )
        if (innerHoleFraction > 0f) {
            drawCircle(
                color = Color.Transparent,
                radius = outerRadiusPx * innerHoleFraction.coerceIn(0f, 0.98f),
                center = center,
                blendMode = BlendMode.Clear,
            )
        }
    }
}
