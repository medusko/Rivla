package com.medusko.rivla.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun EvVehicleVectorIcon(
    color: Color = Color(0xFFE89D24),
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f

        val chassisPath = Path().apply {
            moveTo(w * 0.08f, h * 0.68f)
            lineTo(w * 0.08f, h * 0.50f)
            lineTo(w * 0.24f, h * 0.44f)
            lineTo(w * 0.36f, h * 0.26f)
            lineTo(w * 0.66f, h * 0.26f)
            lineTo(w * 0.80f, h * 0.44f)
            lineTo(w * 0.92f, h * 0.50f)
            lineTo(w * 0.92f, h * 0.68f)
        }
        drawPath(
            path = chassisPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Deck line between wheels
        drawLine(
            color = color,
            start = Offset(w * 0.38f, h * 0.68f),
            end = Offset(w * 0.62f, h * 0.68f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Wheels
        drawCircle(
            color = color,
            radius = w * 0.13f,
            center = Offset(w * 0.25f, h * 0.68f),
            style = Stroke(width = stroke)
        )
        drawCircle(
            color = color,
            radius = w * 0.13f,
            center = Offset(w * 0.75f, h * 0.68f),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun PowerwallVectorIcon(
    color: Color = Color(0xFF00C853),
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f

        // Battery pack enclosure
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.22f, h * 0.16f),
            size = Size(w * 0.56f, h * 0.74f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = stroke)
        )

        // Top terminal cap
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.38f, h * 0.06f),
            size = Size(w * 0.24f, h * 0.10f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )

        // 3 horizontal SoE bars
        for (i in 0..2) {
            val y = h * (0.72f - i * 0.18f)
            drawLine(
                color = color,
                start = Offset(w * 0.34f, y),
                end = Offset(w * 0.66f, y),
                strokeWidth = stroke * 0.9f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun StatusColorDot(
    color: Color,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        drawCircle(color = color)
    }
}
