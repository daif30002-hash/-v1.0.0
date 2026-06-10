package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.VentilationViewModel.VentilationMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Canvas line chart plotting Target Temperature/Humidity against flock age.
 */
@Composable
fun ClimateCurveChart(
    currentAge: Int,
    currentValue: Float,
    targetValue: Float,
    isHumidity: Boolean = false,
    isArabic: Boolean = true,
    isFahrenheit: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Reference chart points (flock age vs target temp under typical guide)
    val points = if (isHumidity) {
        listOf(
            Offset(1f, 65f),
            Offset(3f, 65f),
            Offset(7f, 62.5f),
            Offset(14f, 60f),
            Offset(21f, 57.5f),
            Offset(28f, 57.5f),
            Offset(35f, 60f),
            Offset(42f, 60f),
            Offset(49f, 60f),
            Offset(56f, 60f)
        )
    } else {
        listOf(
            Offset(1f, 33.5f),
            Offset(3f, 32.5f),
            Offset(7f, 30.5f),
            Offset(14f, 28.5f),
            Offset(21f, 26.5f),
            Offset(28f, 24.5f),
            Offset(35f, 22.5f),
            Offset(42f, 20.5f),
            Offset(49f, 20.0f),
            Offset(56f, 20.0f)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val labelSuite = if (isArabic) {
        if (isHumidity) "الرطوبة المستهدفة (%)" else "الحرارة المستهدفة (°س)"
    } else {
        if (isHumidity) "Target Humidity (%)" else "Target Temperature (°C)"
    }

    val ageLabel = if (isArabic) "العمر باليوم" else "Flock Age (Days)"

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val paddingLeft = 110f
        val paddingRight = 40f
        val paddingTop = 60f
        val paddingBottom = 80f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Min/Max bounds for axis mapping
        val minX = 1f
        val maxX = 56f
        val minY = if (isHumidity) 30f else 15f
        val maxY = if (isHumidity) 95f else 40f

        fun toChartX(x: Float): Float = paddingLeft + ((x - minX) / (maxX - minX)) * chartWidth
        fun toChartY(y: Float): Float = paddingTop + chartHeight - ((y - minY) / (maxY - minY)) * chartHeight

        // Draw background grid lines & Y Axis values
        val gridLinesCount = 5
        for (i in 0..gridLinesCount) {
            val ratio = i.toFloat() / gridLinesCount
            val yVal = minY + ratio * (maxY - minY)
            val cy = toChartY(yVal)

            // Draw line
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(paddingLeft, cy),
                end = Offset(width - paddingRight, cy),
                strokeWidth = 1.dp.toPx()
            )

            // Value text
            val formattedValue = if (!isHumidity && isFahrenheit) {
                String.format("%.0f°F", yVal * 1.8f + 32f)
            } else {
                String.format("%.0f%s", yVal, if (isHumidity) "%" else "°C")
            }

            drawContext.canvas.nativeCanvas.apply {
                val tp = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                drawText(formattedValue, paddingLeft - 15f, cy + 10f, tp)
            }
        }

        // Draw X axis label days
        val dayTicks = listOf(1, 7, 14, 21, 28, 35, 42, 49, 56)
        for (day in dayTicks) {
            val cx = toChartX(day.toFloat())
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(cx, paddingTop),
                end = Offset(cx, paddingTop + chartHeight),
                strokeWidth = 1.dp.toPx()
            )

            drawContext.canvas.nativeCanvas.apply {
                val tp = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 26f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(day.toString(), cx, paddingTop + chartHeight + 35f, tp)
            }
        }

        // Y and X axes lines
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + chartHeight),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(paddingLeft, paddingTop + chartHeight),
            end = Offset(width - paddingRight, paddingTop + chartHeight),
            strokeWidth = 2.dp.toPx()
        )

        // Draw Curve Line and Gradient Fill below curve
        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { idx, point ->
            val cx = toChartX(point.x)
            val cy = toChartY(point.y)
            if (idx == 0) {
                path.moveTo(cx, cy)
                fillPath.moveTo(cx, paddingTop + chartHeight)
                fillPath.lineTo(cx, cy)
            } else {
                path.lineTo(cx, cy)
                fillPath.lineTo(cx, cy)
            }
        }
        fillPath.lineTo(toChartX(maxX), paddingTop + chartHeight)
        fillPath.close()

        // Draw Fill with Brush
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )

        // Draw main line curve
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Mark flock current state
        val currentX = toChartX(currentAge.toFloat())
        val targetY = toChartY(targetValue)
        val actualY = toChartY(currentValue)

        // Guide vertical line at current age
        drawLine(
            color = secondaryColor.copy(alpha = 0.6f),
            start = Offset(currentX, paddingTop),
            end = Offset(currentX, paddingTop + chartHeight),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
        )

        // Circle at Target point
        drawCircle(
            color = tertiaryColor,
            radius = 6.dp.toPx(),
            center = Offset(currentX, targetY)
        )

        // Circle at Actual point
        drawCircle(
            color = primaryColor,
            radius = 8.dp.toPx(),
            center = Offset(currentX, actualY)
        )

        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(currentX, actualY)
        )

        // Draw Legend Labels
        drawContext.canvas.nativeCanvas.apply {
            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 32f
                textAlign = if (isArabic) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT
                isAntiAlias = true
            }
            val textX = if (isArabic) width - paddingRight else paddingLeft + 30f
            drawText("$labelSuite - $ageLabel", textX, paddingTop - 25f, labelPaint)
        }
    }
}

/**
 * Custom live simulator layout of the house showing active/resting components, open percentages and airflow.
 */
@Composable
fun BarnLayoutVisualizer(
    ventilationMode: VentilationMode,
    sideInletsCount: Int,
    tunnelInletsCount: Int,
    totalFansCount: Int,
    activeFansCount: Int,
    sideInletOpeningPct: Float,
    tunnelInletOpeningPct: Float,
    isArabic: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Rotation duration determines animation speed based on active fans
    val infiniteTransition = rememberInfiniteTransition(label = "FanRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FanRotationAngle"
    )

    // Airflow ripple animated offset
    val airFlowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AirflowRipple"
    )

    val contentColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary // Ventilating color
    val targetInletColor = if (ventilationMode == VentilationMode.TUNNEL) Color(0xFF00ADB5) else Color(0xFF22C55E)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val marginX = 80f
        val marginY = 60f

        val houseWidth = width - marginX * 2
        val houseHeight = height - marginY * 2

        // Draw Barn main building shell layout
        drawRect(
            color = Color(0xFF1E1E30),
            topLeft = Offset(marginX, marginY),
            size = Size(houseWidth, houseHeight)
        )
        // Outer wall boundary line
        drawRect(
            color = Color(0xFF3F3F5F),
            topLeft = Offset(marginX, marginY),
            size = Size(houseWidth, houseHeight),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw Side Inlet indicators along Left Wall (Top & Bottom)
        val inletsPerSide = kotlin.math.max(4, sideInletsCount / 2)
        val insetSideOffset = houseWidth / (inletsPerSide + 1)

        val sideInletActiveColor = if (ventilationMode != VentilationMode.TUNNEL) {
            Color(0xFF22C55E).copy(alpha = 0.3f + 0.7f * (sideInletOpeningPct / 100f))
        } else {
            Color(0xFF3F3F5F) // closed in tunnel mode
        }

        for (i in 1..inletsPerSide) {
            val xPos = marginX + i * insetSideOffset
            val inletWidth = 25f
            val inletHeight = 10f

            // Topside inlet
            drawRect(
                color = sideInletActiveColor,
                topLeft = Offset(xPos - inletWidth / 2, marginY - inletHeight / 2),
                size = Size(inletWidth, inletHeight)
            )
            // Bottomside inlet
            drawRect(
                color = sideInletActiveColor,
                topLeft = Offset(xPos - inletWidth / 2, marginY + houseHeight - inletHeight / 2),
                size = Size(inletWidth, inletHeight)
            )

            // Dynamic airflow arrows for side inlets (if open and in side/trans mode)
            if (ventilationMode != VentilationMode.TUNNEL && sideInletOpeningPct > 5f) {
                // Topside flow arrow inwards
                drawLine(
                    color = Color(0xAA22C55E),
                    start = Offset(xPos, marginY),
                    end = Offset(xPos, marginY + 25f + 15f * airFlowOffset),
                    strokeWidth = 2f
                )
                // Bottomside flow arrow inwards
                drawLine(
                    color = Color(0xAA22C55E),
                    start = Offset(xPos, marginY + houseHeight),
                    end = Offset(xPos, marginY + houseHeight - 25f - 15f * airFlowOffset),
                    strokeWidth = 2f
                )
            }
        }

        // Left End Header containing Tunnel Air Inlets (Air flow from Left to Right - SKOV pattern)
        val tunnelsInletsLeftWall = kotlin.math.max(2, tunnelInletsCount)
        val tunnelInletInterval = houseHeight / (tunnelsInletsLeftWall + 1)
        val tunnelActiveColor = if (ventilationMode == VentilationMode.TUNNEL) {
            Color(0xFF00ADB5).copy(alpha = 0.3f + 0.7f * (tunnelInletOpeningPct / 100f))
        } else {
            Color(0xFF3F3F5F) // closed in minimum or transitional modes
        }

        for (i in 1..tunnelsInletsLeftWall) {
            val yPos = marginY + i * tunnelInletInterval
            val tWidth = 12f
            val tHeight = 40f

            drawRect(
                color = tunnelActiveColor,
                topLeft = Offset(marginX - tWidth / 2, yPos - tHeight / 2),
                size = Size(tWidth, tHeight)
            )

            // Dynamic airflow entering from tunnel inlet when active
            if (ventilationMode == VentilationMode.TUNNEL && tunnelInletOpeningPct > 5f) {
                drawLine(
                    color = Color(0xAA00ADB5),
                    start = Offset(marginX, yPos),
                    end = Offset(marginX + 60f + 50f * airFlowOffset, yPos),
                    strokeWidth = 2.5f
                )
            }
        }

        // Right End of layout containing Exhaust Fans (Air pulled out)
        val fanYInterval = houseHeight / (totalFansCount + 1)
        for (i in 1..totalFansCount) {
            val yPos = marginY + i * fanYInterval
            val xPos = marginX + houseWidth // right wall
            val fanRadius = 16f

            val isActive = i <= activeFansCount

            // Fan external boundary housing
            drawCircle(
                color = if (isActive) primaryColor else Color(0xFF3F3F5F),
                radius = fanRadius,
                center = Offset(xPos, yPos),
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner center hub
            drawCircle(
                color = if (isActive) primaryColor else Color(0xFF3F3F5F),
                radius = 4f,
                center = Offset(xPos, yPos)
            )

            // Rotating fan blades
            val currentAngle = if (isActive) rotationAngle else 0f
            val radAngle = (currentAngle * PI / 180f).toFloat()

            for (b in 0..2) {
                val offsetAngle = radAngle + (b * 2 * PI / 3).toFloat()
                val bladeEnd = Offset(
                    xPos + fanRadius * cos(offsetAngle),
                    yPos + fanRadius * sin(offsetAngle)
                )
                drawLine(
                    color = if (isActive) primaryColor else Color(0xFF4B4B6F),
                    start = Offset(xPos, yPos),
                    end = bladeEnd,
                    strokeWidth = 3f
                )
            }
        }

        // Draw general layout labels relative to direction
        val sideInletName = if (isArabic) "فتحات تهوية جانبية" else "Side Air Inlets"
        val tunnelInletName = if (isArabic) "فتحات نفقية" else "Tunnel Inlets"
        val exhaustFansName = if (isArabic) "مراوح سحب الهواء" else "Exhaust Fans"
        val airPatternMode = if (isArabic) {
            when (ventilationMode) {
                VentilationMode.MINIMUM -> "التهوية الدنيا (سرعة منخفضة)"
                VentilationMode.TRANSITIONAL -> "تهوية انتقالية مستمرة"
                VentilationMode.TUNNEL -> "تهوية نفقية (سرعة مستهدفة)"
            }
        } else {
            when (ventilationMode) {
                VentilationMode.MINIMUM -> "Minimum Cycling Mode"
                VentilationMode.TRANSITIONAL -> "Continuous Transitional Mode"
                VentilationMode.TUNNEL -> "Active Tunnel Mode"
            }
        }

        drawContext.canvas.nativeCanvas.apply {
            val legendPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            // Labels
            drawText(sideInletName, width / 2f, marginY - 20f, legendPaint)
            drawText(exhaustFansName, width - 110f, marginY + houseHeight + 40f, legendPaint.apply { textAlign = android.graphics.Paint.Align.RIGHT })
            drawText(tunnelInletName, marginX - 10f, marginY + houseHeight + 40f, legendPaint.apply { textAlign = android.graphics.Paint.Align.LEFT })

            // Center description of ventilation pattern
            val centerTextPaint = android.graphics.Paint().apply {
                color = if (ventilationMode == VentilationMode.TUNNEL) android.graphics.Color.CYAN else android.graphics.Color.GREEN
                textSize = 30f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(airPatternMode, marginX + houseWidth / 2f, marginY + houseHeight / 2f + 10f, centerTextPaint)
        }
    }
}
