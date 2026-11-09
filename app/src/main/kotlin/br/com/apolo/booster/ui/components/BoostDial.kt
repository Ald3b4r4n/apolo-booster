package br.com.apolo.booster.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.apolo.booster.audio.GainMath
import br.com.apolo.booster.ui.theme.Ember
import br.com.apolo.booster.ui.theme.Gold
import br.com.apolo.booster.ui.theme.TextMuted
import br.com.apolo.booster.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BoostDial(
    percent: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val fraction = (percent - GainMath.MIN_PERCENT).toFloat() /
        (GainMath.MAX_PERCENT - GainMath.MIN_PERCENT).toFloat()
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420),
        label = "dial-fraction",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.085f
            val inset = stroke
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val startAngle = 135f
            val fullSweep = 270f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = startAngle,
                sweepAngle = fullSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (animated > 0.001f) {
                drawArc(
                    color = Gold.copy(alpha = 0.16f),
                    startAngle = startAngle,
                    sweepAngle = fullSweep * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 1.7f, cap = StrokeCap.Round),
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Ember, Gold, Gold, Ember),
                        center = center,
                    ),
                    startAngle = startAngle,
                    sweepAngle = fullSweep * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            val tickCount = 27
            val outerRadius = size.minDimension / 2f - stroke * 0.42f
            val innerRadius = outerRadius - stroke * 0.42f
            for (index in 0..tickCount) {
                val angle = Math.toRadians((startAngle + fullSweep * index / tickCount).toDouble())
                val dx = cos(angle).toFloat()
                val dy = sin(angle).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(center.x + innerRadius * dx, center.y + innerRadius * dy),
                    end = Offset(center.x + outerRadius * dx, center.y + outerRadius * dy),
                    strokeWidth = 1.4f,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$percent%",
                fontSize = 62.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = br.com.apolo.booster.ui.theme.ApoloDisplayFont,
                color = if (enabled) TextPrimary else TextMuted,
            )
            Text(
                text = "BOOST",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 5.sp,
                color = TextMuted,
            )
            Text(
                text = "~+${GainMath.percentToDb(percent).roundToInt()} dB",
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) Gold else TextMuted,
            )
        }
    }
}
