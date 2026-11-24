package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalFintekColors
import com.example.ui.theme.MoneyMediumStyle
import com.example.ui.theme.MoneySmallStyle
import com.example.ui.viewmodel.CashFlowBarPoint
import com.example.ui.viewmodel.CategoryShare
import com.example.util.formatCompactRupiah
import com.example.util.formatRupiah
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun MiniBalanceSparkline(
    values: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val safeValues = if (values.size >= 2) values else listOf(0f, 1f, 0.7f, 1.4f, 1.9f)
    Canvas(modifier = modifier) {
        val minVal = safeValues.minOrNull() ?: 0f
        val maxVal = safeValues.maxOrNull() ?: 1f
        val range = max(1f, maxVal - minVal)
        val stepX = size.width / (safeValues.size - 1).coerceAtLeast(1)

        val points = safeValues.mapIndexed { index, v ->
            val x = index * stepX
            val normalized = (v - minVal) / range
            val y = size.height - (normalized * size.height * 0.78f) - size.height * 0.11f
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f
                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.28f),
                    lineColor.copy(alpha = 0.0f)
                )
            )
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val last = points.last()
        drawCircle(
            color = lineColor.copy(alpha = 0.28f),
            radius = 6.dp.toPx(),
            center = last
        )
        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = last
        )
    }
}

@Composable
fun CashFlowGroupedBarChart(
    points: List<CashFlowBarPoint>,
    modifier: Modifier = Modifier
) {
    val fintekColors = LocalFintekColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    var selectedIndex by remember(points) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "barChartProgress"
    )

    val maxValue = remember(points) {
        points.maxOfOrNull { max(it.income, it.expense) }?.coerceAtLeast(100_000L) ?: 100_000L
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected point readout pill
        val selected = points.getOrNull(selectedIndex)
        if (selected != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = fintekColors.subtleSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selected.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "+${formatCompactRupiah(selected.income)}",
                            style = MoneySmallStyle,
                            color = fintekColors.income
                        )
                        Text(
                            text = "-${formatCompactRupiah(selected.expense)}",
                            style = MoneySmallStyle,
                            color = fintekColors.expense
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada data arus kas pada periode ini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = labelColor
                )
            }
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-axis compact labels
            Column(
                modifier = Modifier
                    .width(58.dp)
                    .height(164.dp)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCompactRupiah(maxValue),
                    style = MoneySmallStyle,
                    color = labelColor,
                    maxLines = 1
                )
                Text(
                    text = formatCompactRupiah(maxValue / 2),
                    style = MoneySmallStyle,
                    color = labelColor,
                    maxLines = 1
                )
                Text(
                    text = "Rp 0",
                    style = MoneySmallStyle,
                    color = labelColor,
                    maxLines = 1
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(164.dp)
                        .testTag("cash_flow_bar_canvas")
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                if (points.isNotEmpty()) {
                                    val slotWidth = size.width / points.size.toFloat()
                                    val tapped = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                                    selectedIndex = tapped
                                }
                            }
                        }
                ) {
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    listOf(0f, 0.5f, 1f).forEach { fraction ->
                        val y = size.height * fraction
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (fraction < 1f) dashEffect else null
                        )
                    }

                    val groupWidth = size.width / points.size.toFloat()
                    val barWidth = (groupWidth * 0.28f).coerceIn(6.dp.toPx(), 18.dp.toPx())
                    val innerGap = 3.dp.toPx()

                    points.forEachIndexed { index, point ->
                        val centerX = index * groupWidth + groupWidth / 2f
                        val isSelected = index == selectedIndex

                        if (isSelected) {
                            drawRoundRect(
                                color = fintekColors.income.copy(alpha = 0.08f),
                                topLeft = Offset(index * groupWidth + 2.dp.toPx(), 0f),
                                size = Size(groupWidth - 4.dp.toPx(), size.height),
                                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                            )
                        }

                        val incomeHeight =
                            (point.income.toFloat() / maxValue.toFloat()) * size.height * animProgress
                        val expenseHeight =
                            (point.expense.toFloat() / maxValue.toFloat()) * size.height * animProgress

                        if (point.income > 0L) {
                            val h = max(4.dp.toPx(), incomeHeight)
                            drawRoundRect(
                                color = if (isSelected) fintekColors.income else fintekColors.income.copy(alpha = 0.72f),
                                topLeft = Offset(centerX - barWidth - innerGap / 2f, size.height - h),
                                size = Size(barWidth, h),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                        }

                        if (point.expense > 0L) {
                            val h = max(4.dp.toPx(), expenseHeight)
                            drawRoundRect(
                                color = if (isSelected) fintekColors.expense else fintekColors.expense.copy(alpha = 0.72f),
                                topLeft = Offset(centerX + innerGap / 2f, size.height - h),
                                size = Size(barWidth, h),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    points.forEachIndexed { index, point ->
                        val isSelected = index == selectedIndex
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else labelColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedIndex = index }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = fintekColors.income, label = "Pemasukan")
            Spacer(modifier = Modifier.width(20.dp))
            LegendDot(color = fintekColors.expense, label = "Pengeluaran")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryDonutChart(
    shares: List<CategoryShare>,
    totalAmount: Long,
    centerTitle: String,
    selectedCategoryId: Long?,
    onSelectCategory: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val sweepFraction by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "donutSweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedShare = shares.firstOrNull { it.categoryId == selectedCategoryId }

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("category_donut_canvas")
                .pointerInput(shares, selectedCategoryId) {
                    detectTapGestures { offset ->
                        if (shares.isEmpty()) return@detectTapGestures
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = hypot(dx, dy)
                        val radius = minOf(size.width, size.height) / 2f
                        if (dist < radius * 0.45f) {
                            onSelectCategory(null)
                            return@detectTapGestures
                        }
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (angle < 0f) angle += 360f

                        var runningAngle = 0f
                        for (share in shares) {
                            val slice = share.percentage * 360f
                            if (angle in runningAngle..(runningAngle + slice)) {
                                val nextId = if (selectedCategoryId == share.categoryId) null else share.categoryId
                                onSelectCategory(nextId)
                                break
                            }
                            runningAngle += slice
                        }
                    }
                }
        ) {
            val strokeWidth = 28.dp.toPx()
            val inset = strokeWidth / 2f + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            if (shares.isEmpty() || totalAmount <= 0L) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                var startAngle = -90f
                val gapDeg = if (shares.size > 1) 2.5f else 0f
                shares.forEach { share ->
                    val rawSweep = share.percentage * 360f * sweepFraction
                    val sweep = (rawSweep - gapDeg).coerceAtLeast(1.5f)
                    val isDimmed = selectedCategoryId != null && selectedCategoryId != share.categoryId
                    val sliceColor = Color(share.colorHex).copy(alpha = if (isDimmed) 0.28f else 1f)
                    val activeStroke = if (selectedCategoryId == share.categoryId) strokeWidth + 5.dp.toPx() else strokeWidth

                    drawArc(
                        color = sliceColor,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = activeStroke, cap = StrokeCap.Butt)
                    )
                    startAngle += rawSweep
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = selectedShare?.name ?: centerTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatCompactRupiah(selectedShare?.amount ?: totalAmount),
                style = MoneyMediumStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (selectedShare != null) {
                Text(
                    text = "${(selectedShare.percentage * 100).toInt()}% • ${formatRupiah(selectedShare.amount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
