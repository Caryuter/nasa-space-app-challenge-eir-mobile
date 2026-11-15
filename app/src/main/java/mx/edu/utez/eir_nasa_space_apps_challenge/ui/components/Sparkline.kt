package mx.edu.utez.eir_nasa_space_apps_challenge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Sparkline(
    data: List<Float>,
    color: Color = Color(0xFF0EA5E9),
    height: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 1f
        val range = if (max - min == 0f) 1f else max - min

        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height - (strokeWidth * 2)

        val path = Path()
        data.forEachIndexed { index, value ->
            val x = (index.toFloat() / (data.size - 1)) * w
            val normalized = (value - min) / range
            val y = size.height - strokeWidth - (normalized * h)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
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
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Sparkline Preview")
@Composable
fun SparklinePreview() {
    mx.edu.utez.eir_nasa_space_apps_challenge.ui.theme.EirNASASpaceAppsChallengeTheme {
        Sparkline(
            data = listOf(71f, 69f, 72f, 70f, 68f, 69f, 68f),
            color = Color(0xFF0EA5E9),
            height = 24.dp
        )
    }
}

