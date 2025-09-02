package com.yumzy.userapp

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.animation.core.*

@Composable
fun YLogoLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    color: Color = Color.Red
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsing scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing), // FIXED
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Alpha animation for breathing effect
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing), // FIXED
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.size(size * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        // Outer rotating ring
        Canvas(
            modifier = Modifier
                .size(size * 1.3f)
                .graphicsLayer {
                    rotationZ = rotation
                }
        ) {
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = this.size.minDimension / 2,
                style = Stroke(width = 3.dp.toPx())
            )

            // Animated arc
            drawArc(
                color = color.copy(alpha = 0.8f),
                startAngle = -90f,
                sweepAngle = 120f,
                useCenter = false,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // Y Logo with animations
        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        ) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val strokeWidth = 6.dp.toPx()

            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2

            val ySize = canvasWidth * 0.4f

            // Draw Y shape
            val path = Path().apply {
                moveTo(centerX - ySize / 2, centerY - ySize / 2)
                lineTo(centerX, centerY)

                moveTo(centerX + ySize / 2, centerY - ySize / 2)
                lineTo(centerX, centerY)

                moveTo(centerX, centerY)
                lineTo(centerX, centerY + ySize / 2)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Inner pulsing dot
        Canvas(
            modifier = Modifier
                .size(12.dp)
                .graphicsLayer {
                    scaleX = scale * 0.8f
                    scaleY = scale * 0.8f
                    this.alpha = alpha * 0.7f
                }
        ) {
            drawCircle(
                color = color,
                radius = this.size.minDimension / 4
            )
        }
    }
}
