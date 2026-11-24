package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.CashFlowGroupedBarChart
import com.example.ui.components.CategoryDonutChart
import com.example.ui.theme.LocalFintekColors
import com.example.ui.theme.MoneyMediumStyle
import com.example.ui.viewmodel.AnalyticsPeriod
import com.example.ui.viewmodel.AnalyticsUiState
import com.example.ui.viewmodel.CategoryShare
import com.example.util.formatCompactRupiah
import com.example.util.formatRupiah
import com.example.util.iconForKey

@Composable
fun AnalyticsScreen(
    analytics: AnalyticsUiState,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val fintekColors = LocalFintekColors.current
    var showExpenseBreakdown by remember { mutableStateOf(true) }
    var selectedCategoryId by remember(showExpenseBreakdown, analytics.period) {
        mutableStateOf<Long?>(null)
    }

    val activeShares = if (showExpenseBreakdown) analytics.expenseShares else analytics.incomeShares
    val activeTotal = if (showExpenseBreakdown) analytics.totalExpense else analytics.totalIncome

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Analitik & Laporan",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pantau arus kas dan komposisi pengeluaran Anda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Period Selector Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = analytics.period == period,
                        onClick = { onSelectPeriod(period) },
                        label = { Text(period.label) },
                        modifier = Modifier.testTag("analytics_period_${period.name.lowercase()}")
                    )
                }
            }
        }

        // KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsKpiCard(
                    title = "Total Pemasukan",
                    amount = formatRupiah(analytics.totalIncome),
                    compact = formatCompactRupiah(analytics.totalIncome),
                    accentColor = fintekColors.income,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsKpiCard(
                    title = "Total Pengeluaran",
                    amount = formatRupiah(analytics.totalExpense),
                    compact = formatCompactRupiah(analytics.totalExpense),
                    accentColor = fintekColors.expense,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, fintekColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Arus Kas Bersih (${analytics.period.label})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupiah(analytics.netCashFlow, showPlusSign = true),
                            style = MoneyMediumStyle.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize
                            ),
                            color = if (analytics.netCashFlow >= 0) fintekColors.income else fintekColors.expense
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Rasio Tabungan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${analytics.savingsRatePercent}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Grouped Cash Flow Bar Chart Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, fintekColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Grafik Arus Kas",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ketuk batang grafik untuk melihat rincian angka",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    CashFlowGroupedBarChart(points = analytics.barPoints)
                }
            }
        }

        // Category Breakdown Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, fintekColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rincian Kategori",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = fintekColors.subtleSurface
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                BreakdownTogglePill(
                                    label = "Keluar",
                                    selected = showExpenseBreakdown,
                                    onClick = { showExpenseBreakdown = true },
                                    modifier = Modifier.testTag("breakdown_expense_toggle")
                                )
                                BreakdownTogglePill(
                                    label = "Masuk",
                                    selected = !showExpenseBreakdown,
                                    onClick = { showExpenseBreakdown = false },
                                    modifier = Modifier.testTag("breakdown_income_toggle")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CategoryDonutChart(
                        shares = activeShares,
                        totalAmount = activeTotal,
                        centerTitle = if (showExpenseBreakdown) "Total Keluar" else "Total Masuk",
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategory = { selectedCategoryId = it }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (activeShares.isEmpty()) {
                        Text(
                            text = "Belum ada transaksi pada periode ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            activeShares.forEach { share ->
                                CategoryShareRow(
                                    share = share,
                                    isSelected = selectedCategoryId == share.categoryId,
                                    onClick = {
                                        selectedCategoryId = if (selectedCategoryId == share.categoryId) {
                                            null
                                        } else {
                                            share.categoryId
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(132.dp)) }
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    amount: String,
    compact: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LocalFintekColors.current.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amount,
                style = MoneyMediumStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = compact,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun BreakdownTogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryShareRow(
    share: CategoryShare,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = Color(share.colorHex)
    val fintekColors = LocalFintekColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) color.copy(alpha = 0.08f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForKey(share.iconKey),
                        contentDescription = share.name,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = share.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${share.transactionCount} transaksi • ${(share.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatRupiah(share.amount),
                    style = MoneyMediumStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { share.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = fintekColors.subtleSurface,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
