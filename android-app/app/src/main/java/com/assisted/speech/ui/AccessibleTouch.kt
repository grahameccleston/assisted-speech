package com.assisted.speech.ui

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assisted.speech.data.Formality
import com.assisted.speech.data.SpeakerGender
import kotlinx.coroutines.delay

enum class Handedness(val label: String) {
    BOTTOM("Bottom Reach"),
    RIGHT("Right Hand"),
    LEFT("Left Hand")
}

data class MotorAccessSettings(
    val debounceMs: Long = 350L,
    val minHoldMs: Long = 70L,
    val handedness: Handedness = Handedness.BOTTOM,
    val autoSpeakComplete: Boolean = true,
    val speakerGender: SpeakerGender = SpeakerGender.NEUTRAL,
    val formality: Formality = Formality.POLITE,
    val speechRate: Float = 0.85f,
    val autoScanEnabled: Boolean = false,
    val scanIntervalMs: Long = 2200L
)

/**
 * Global tremor debounce tracker across all buttons on screen so an involuntary
 * double-bounce or grazing a neighbor button immediately after a tap is ignored.
 */
object TremorDebounceGate {
    private var lastAcceptedTapUptimeMs: Long = 0L

    fun tryAcquire(debounceMs: Long): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - lastAcceptedTapUptimeMs < debounceMs) {
            return false
        }
        lastAcceptedTapUptimeMs = now
        return true
    }
}

/**
 * Motor-accessible button designed specifically for users with hand tremors,
 * accidental brush touches, unsteady release gestures, and 1-switch scanning:
 * 1. Ignores accidental micro-brushes shorter than [settings.minHoldMs].
 * 2. Renders a visual hold-progress bar when [settings.minHoldMs] >= 120ms.
 * 3. Tolerates generous finger jitter/tremor (up to 48px outside bounds) without cancelling.
 * 4. Enforces a global [settings.debounceMs] lockout after activation to block double-bounces.
 * 5. Supports high-visibility [isScanHighlighted] focus ring for 1-switch / auto-scan users.
 */
@Composable
fun AccessibleCardButton(
    onClick: () -> Unit,
    settings: MotorAccessSettings,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isScanHighlighted: Boolean = false,
    backgroundColor: Color = CardDark,
    pressedColor: Color = Color(0xFF28374D),
    borderColor: Color = BorderDark,
    borderWidth: Dp = 2.5.dp,
    cornerRadius: Dp = 16.dp,
    accessibilityLabel: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentSettings by rememberUpdatedState(settings)
    val currentEnabled by rememberUpdatedState(enabled)

    LaunchedEffect(isPressed, currentSettings.minHoldMs) {
        if (isPressed && currentSettings.minHoldMs >= 120L) {
            val start = SystemClock.uptimeMillis()
            while (isPressed) {
                val elapsed = SystemClock.uptimeMillis() - start
                holdProgress = (elapsed.toFloat() / currentSettings.minHoldMs).coerceIn(0f, 1f)
                delay(20L)
            }
        } else {
            holdProgress = 0f
        }
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.96f
            isScanHighlighted -> 1.02f
            else -> 1.0f
        },
        label = "PressScale"
    )

    val activeBorderColor = when {
        isScanHighlighted -> Color(0xFFFACC15) // High-visibility gold scan ring
        isPressed -> Color(0xFF38BDF8)
        else -> borderColor
    }
    val activeBorderWidth = when {
        isScanHighlighted -> 4.dp
        else -> borderWidth
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .onSizeChanged { boxSize = it }
            .clip(shape)
            .background(if (isPressed) pressedColor else backgroundColor, shape)
            .border(BorderStroke(activeBorderWidth, activeBorderColor), shape)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (accessibilityLabel != null) {
                    contentDescription = accessibilityLabel
                }
                onClick {
                    if (currentEnabled) {
                        currentOnClick()
                        true
                    } else {
                        false
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!currentEnabled) return@awaitEachGesture

                    val downTime = SystemClock.uptimeMillis()
                    isPressed = true

                    val slopMarginPx = 48.dp.toPx()
                    var stayedWithinGenerousBounds = true

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            break
                        }
                        val pos = change.position
                        if (boxSize.width > 0 && boxSize.height > 0) {
                            if (pos.x < -slopMarginPx ||
                                pos.x > boxSize.width + slopMarginPx ||
                                pos.y < -slopMarginPx ||
                                pos.y > boxSize.height + slopMarginPx
                            ) {
                                stayedWithinGenerousBounds = false
                                isPressed = false
                                break
                            }
                        }
                        change.consume()
                    }

                    val holdDuration = SystemClock.uptimeMillis() - downTime
                    val wasPressed = isPressed
                    isPressed = false

                    if (wasPressed &&
                        stayedWithinGenerousBounds &&
                        holdDuration >= currentSettings.minHoldMs &&
                        TremorDebounceGate.tryAcquire(currentSettings.debounceMs)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnClick()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()

        // Visual hold-threshold progress indicator at bottom of tile
        if (isPressed && currentSettings.minHoldMs >= 120L) {
            LinearProgressIndicator(
                progress = { holdProgress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(5.dp),
                color = if (holdProgress >= 1f) AccentGreen else AccentCyan,
                trackColor = Color.Transparent
            )
        }
    }
}

/**
 * Requires holding continuously for 3 seconds to open or exit Caregiver Mode,
 * preventing accidental tremor taps from opening settings or deleting vocabulary.
 */
@Composable
fun CaregiverHoldLockButton(
    isUnlocked: Boolean,
    onUnlockToggle: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMs: Long = 2600L
) {
    val haptic = LocalHapticFeedback.current
    var isHolding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val currentOnToggle by rememberUpdatedState(onUnlockToggle)

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val start = SystemClock.uptimeMillis()
            while (isHolding) {
                val elapsed = SystemClock.uptimeMillis() - start
                progress = (elapsed.toFloat() / holdDurationMs).coerceIn(0f, 1f)
                if (elapsed >= holdDurationMs) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isHolding = false
                    progress = 0f
                    currentOnToggle()
                    break
                }
                delay(30L)
            }
        } else {
            progress = 0f
        }
    }

    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.5.dp,
                if (isUnlocked) AccentAmber else BorderDark,
                RoundedCornerShape(10.dp)
            )
            .pointerInput(isUnlocked) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (isUnlocked) {
                        // Exiting Caregiver mode is immediate on 1 tap
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnToggle()
                        return@awaitEachGesture
                    }
                    isHolding = true
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) {
                            break
                        }
                    }
                    isHolding = false
                }
            },
        color = if (isUnlocked) Color(0xFF451A03) else Color(0xFF1E293B)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isUnlocked) "🔓 Exit Caregiver" else if (isHolding) "⏳ Keep Holding…" else "🔒 Caregiver (Hold 3s)",
                color = if (isUnlocked) Color(0xFFFDE68A) else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (isHolding && !isUnlocked) {
                Spacer(modifier = Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.width(96.dp).height(4.dp),
                    color = AccentAmber,
                    trackColor = BorderDark
                )
            }
        }
    }
}
