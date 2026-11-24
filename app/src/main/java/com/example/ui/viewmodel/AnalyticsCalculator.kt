package com.example.ui.viewmodel

import com.example.data.local.TransactionDetail
import com.example.data.local.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AnalyticsPeriod(val label: String) {
    WEEK("7 Hari"),
    MONTH("Bulan Ini"),
    THREE_MONTHS("3 Bulan"),
    YEAR("Tahun Ini")
}

data class CashFlowBarPoint(
    val label: String,
    val income: Long,
    val expense: Long
)

data class CategoryShare(
    val categoryId: Long,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val amount: Long,
    val percentage: Float,
    val transactionCount: Int
)

data class AnalyticsUiState(
    val period: AnalyticsPeriod = AnalyticsPeriod.MONTH,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netCashFlow: Long = 0L,
    val savingsRatePercent: Int = 0,
    val barPoints: List<CashFlowBarPoint> = emptyList(),
    val expenseShares: List<CategoryShare> = emptyList(),
    val incomeShares: List<CategoryShare> = emptyList(),
    val sparklineValues: List<Float> = emptyList()
)

object AnalyticsCalculator {

    private val indoLocale = Locale.Builder().setLanguage("in").setRegion("ID").build()

    fun compute(
        details: List<TransactionDetail>,
        period: AnalyticsPeriod,
        nowMillis: Long = System.currentTimeMillis()
    ): AnalyticsUiState {
        val startMillis = periodStartMillis(period, nowMillis)
        val filtered = details.filter { it.transaction.timestamp in startMillis..nowMillis }

        val totalIncome = filtered
            .filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }

        val totalExpense = filtered
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }

        val netCashFlow = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0L) {
            (((totalIncome - totalExpense).toDouble() / totalIncome.toDouble()) * 100.0)
                .toInt()
                .coerceIn(-99, 100)
        } else {
            0
        }

        val barPoints = buildBarPoints(filtered, period, nowMillis)
        val expenseShares = buildCategoryShares(filtered, TransactionType.EXPENSE, totalExpense)
        val incomeShares = buildCategoryShares(filtered, TransactionType.INCOME, totalIncome)
        val sparkline = buildSparkline(details, nowMillis)

        return AnalyticsUiState(
            period = period,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCashFlow = netCashFlow,
            savingsRatePercent = savingsRate,
            barPoints = barPoints,
            expenseShares = expenseShares,
            incomeShares = incomeShares,
            sparklineValues = sparkline
        )
    }

    fun periodStartMillis(period: AnalyticsPeriod, nowMillis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        when (period) {
            AnalyticsPeriod.WEEK -> cal.add(Calendar.DAY_OF_YEAR, -6)
            AnalyticsPeriod.MONTH -> cal.set(Calendar.DAY_OF_MONTH, 1)
            AnalyticsPeriod.THREE_MONTHS -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -2)
            }
            AnalyticsPeriod.YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun buildBarPoints(
        details: List<TransactionDetail>,
        period: AnalyticsPeriod,
        nowMillis: Long
    ): List<CashFlowBarPoint> {
        return when (period) {
            AnalyticsPeriod.WEEK -> {
                val dayFmt = SimpleDateFormat("EEE", indoLocale)
                (6 downTo 0).map { offset ->
                    val start = Calendar.getInstance().apply {
                        timeInMillis = nowMillis
                        add(Calendar.DAY_OF_YEAR, -offset)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val end = (start.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val slice = details.filter {
                        it.transaction.timestamp in start.timeInMillis until end.timeInMillis
                    }
                    CashFlowBarPoint(
                        label = dayFmt.format(Date(start.timeInMillis)).take(3),
                        income = slice.filter { it.transaction.type == TransactionType.INCOME }
                            .sumOf { it.transaction.amount },
                        expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }
                            .sumOf { it.transaction.amount }
                    )
                }
            }

            AnalyticsPeriod.MONTH -> {
                // 4 minggu dalam bulan berjalan
                val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val ranges = listOf(1..7, 8..14, 15..21, 22..maxDay)
                ranges.mapIndexed { idx, range ->
                    val slice = details.filter { item ->
                        val c = Calendar.getInstance().apply { timeInMillis = item.transaction.timestamp }
                        c.get(Calendar.DAY_OF_MONTH) in range
                    }
                    CashFlowBarPoint(
                        label = "Mg ${idx + 1}",
                        income = slice.filter { it.transaction.type == TransactionType.INCOME }
                            .sumOf { it.transaction.amount },
                        expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }
                            .sumOf { it.transaction.amount }
                    )
                }
            }

            AnalyticsPeriod.THREE_MONTHS -> {
                val monthFmt = SimpleDateFormat("MMM", indoLocale)
                (2 downTo 0).map { offset ->
                    val start = Calendar.getInstance().apply {
                        timeInMillis = nowMillis
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, -offset)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val end = (start.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1)
                    }
                    val slice = details.filter {
                        it.transaction.timestamp in start.timeInMillis until end.timeInMillis
                    }
                    CashFlowBarPoint(
                        label = monthFmt.format(Date(start.timeInMillis)),
                        income = slice.filter { it.transaction.type == TransactionType.INCOME }
                            .sumOf { it.transaction.amount },
                        expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }
                            .sumOf { it.transaction.amount }
                    )
                }
            }

            AnalyticsPeriod.YEAR -> {
                val monthFmt = SimpleDateFormat("MMM", indoLocale)
                val currentMonth = Calendar.getInstance().apply { timeInMillis = nowMillis }
                    .get(Calendar.MONTH)
                val monthsToShow = (0..currentMonth).toList()
                monthsToShow.map { monthIndex ->
                    val start = Calendar.getInstance().apply {
                        timeInMillis = nowMillis
                        set(Calendar.MONTH, monthIndex)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    val slice = details.filter {
                        it.transaction.timestamp in start.timeInMillis until end.timeInMillis
                    }
                    CashFlowBarPoint(
                        label = monthFmt.format(Date(start.timeInMillis)).take(3),
                        income = slice.filter { it.transaction.type == TransactionType.INCOME }
                            .sumOf { it.transaction.amount },
                        expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }
                            .sumOf { it.transaction.amount }
                    )
                }
            }
        }
    }

    private fun buildCategoryShares(
        details: List<TransactionDetail>,
        type: TransactionType,
        totalForType: Long
    ): List<CategoryShare> {
        if (totalForType <= 0L) return emptyList()
        return details
            .filter { it.transaction.type == type }
            .groupBy { it.transaction.categoryId }
            .map { (catId, items) ->
                val sum = items.sumOf { it.transaction.amount }
                val first = items.first()
                CategoryShare(
                    categoryId = catId,
                    name = first.categoryName,
                    iconKey = first.categoryIconKey,
                    colorHex = first.categoryColorHex,
                    amount = sum,
                    percentage = (sum.toFloat() / totalForType.toFloat()).coerceIn(0f, 1f),
                    transactionCount = items.size
                )
            }
            .sortedByDescending { it.amount }
    }

    private fun buildSparkline(details: List<TransactionDetail>, nowMillis: Long): List<Float> {
        // Arus kas kumulatif 10 hari terakhir untuk sparkline hero card
        val points = (9 downTo 0).map { offset ->
            val start = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val dayNet = details
                .filter { it.transaction.timestamp in start.timeInMillis until end.timeInMillis }
                .sumOf {
                    when (it.transaction.type) {
                        TransactionType.INCOME -> it.transaction.amount
                        TransactionType.EXPENSE -> -it.transaction.amount
                        TransactionType.TRANSFER -> 0L
                    }
                }
            dayNet.toFloat()
        }
        var running = 0f
        return points.map {
            running += it
            running
        }
    }
}
