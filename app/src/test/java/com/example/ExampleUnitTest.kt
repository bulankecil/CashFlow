package com.example

import com.example.data.local.SeedDataFactory
import com.example.data.local.TransactionDetail
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.viewmodel.AnalyticsCalculator
import com.example.ui.viewmodel.AnalyticsPeriod
import com.example.util.formatCompactRupiah
import com.example.util.formatRupiah
import com.example.util.formatSignedRupiah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun rupiahFormatting_usesIndonesianSeparators() {
        assertEquals("Rp 1.250.000", formatRupiah(1_250_000L))
        assertEquals("+Rp 500.000", formatRupiah(500_000L, showPlusSign = true))
        assertEquals("-Rp 45.000", formatRupiah(-45_000L))
        assertEquals("+Rp 9.500.000", formatSignedRupiah(9_500_000L, isIncome = true))
        assertEquals("-Rp 85.000", formatSignedRupiah(85_000L, isIncome = false))
        assertEquals("Rp 2,5 jt", formatCompactRupiah(2_500_000L))
        assertEquals("Rp 450 rb", formatCompactRupiah(450_000L))
    }

    @Test
    fun analyticsCalculator_computesTotalsSavingsRateAndCategoryShares() {
        val now = System.currentTimeMillis()
        val details = listOf(
            TransactionDetail(
                transaction = TransactionEntity(
                    id = 1,
                    type = TransactionType.INCOME,
                    amount = 10_000_000L,
                    walletId = 1,
                    categoryId = 1,
                    title = "Gaji",
                    timestamp = now
                ),
                walletName = "BCA",
                targetWalletName = null,
                categoryName = "Gaji",
                categoryIconKey = "salary",
                categoryColorHex = 0xFF059669
            ),
            TransactionDetail(
                transaction = TransactionEntity(
                    id = 2,
                    type = TransactionType.EXPENSE,
                    amount = 2_000_000L,
                    walletId = 1,
                    categoryId = 5,
                    title = "Makan",
                    timestamp = now
                ),
                walletName = "BCA",
                targetWalletName = null,
                categoryName = "Makan & Minum",
                categoryIconKey = "food",
                categoryColorHex = 0xFFEF4444
            ),
            TransactionDetail(
                transaction = TransactionEntity(
                    id = 3,
                    type = TransactionType.EXPENSE,
                    amount = 1_000_000L,
                    walletId = 2,
                    categoryId = 7,
                    title = "Bensin",
                    timestamp = now
                ),
                walletName = "GoPay",
                targetWalletName = null,
                categoryName = "Transportasi",
                categoryIconKey = "transport",
                categoryColorHex = 0xFF2563EB
            ),
            TransactionDetail(
                transaction = TransactionEntity(
                    id = 4,
                    type = TransactionType.TRANSFER,
                    amount = 500_000L,
                    walletId = 1,
                    targetWalletId = 2,
                    categoryId = 0,
                    title = "Top Up",
                    timestamp = now
                ),
                walletName = "BCA",
                targetWalletName = "GoPay",
                categoryName = "Transfer Antar Dompet",
                categoryIconKey = "transfer",
                categoryColorHex = 0xFF0284C7
            )
        )

        val result = AnalyticsCalculator.compute(details, AnalyticsPeriod.MONTH, now)

        assertEquals(10_000_000L, result.totalIncome)
        assertEquals(3_000_000L, result.totalExpense)
        assertEquals(7_000_000L, result.netCashFlow)
        assertEquals(70, result.savingsRatePercent)
        assertEquals(2, result.expenseShares.size)
        assertEquals("Makan & Minum", result.expenseShares.first().name)
        assertEquals(4, result.barPoints.size)
    }

    @Test
    fun seedDataFactory_containsBalancedWalletsCategoriesAndTransactions() {
        val wallets = SeedDataFactory.defaultWallets()
        val categories = SeedDataFactory.defaultCategories()
        val txs = SeedDataFactory.sampleTransactions()

        assertTrue(wallets.size >= 4)
        assertTrue(categories.any { it.type == TransactionType.INCOME && it.name == "Lainnya" })
        assertTrue(categories.any { it.type == TransactionType.EXPENSE && it.name == "Lainnya" })
        assertTrue(txs.any { it.type == TransactionType.TRANSFER })
    }

    @Test
    fun buildDailyLedgers_groupsByDateAndExcludesTransferFromNet() {
        val day1 = 1_763_632_800_000L // 20 Nov 2025
        val day2 = day1 - 86_400_000L
        val details = listOf(
            TransactionDetail(
                transaction = TransactionEntity(
                    id = 1, type = TransactionType.INCOME, amount = 500_000L,
                    walletId = 1, categoryId = 1, title = "Bonus", timestamp = day1
                ),
                walletName = "BCA", targetWalletName = null,
                categoryName = "Bonus", categoryIconKey = "bonus", categoryColorHex = 0xFF059669
            ),
            TransactionDetail(
                transaction = TransactionEntity(
                    id = 2, type = TransactionType.EXPENSE, amount = 120_000L,
                    walletId = 1, categoryId = 5, title = "Makan", timestamp = day1 + 3_600_000L
                ),
                walletName = "BCA", targetWalletName = null,
                categoryName = "Makan", categoryIconKey = "food", categoryColorHex = 0xFFEF4444
            ),
            TransactionDetail(
                transaction = TransactionEntity(
                    id = 3, type = TransactionType.TRANSFER, amount = 200_000L,
                    walletId = 1, targetWalletId = 2, categoryId = 0, title = "Top Up", timestamp = day1 + 7_200_000L
                ),
                walletName = "BCA", targetWalletName = "GoPay",
                categoryName = "Transfer", categoryIconKey = "transfer", categoryColorHex = 0xFF0284C7
            ),
            TransactionDetail(
                transaction = TransactionEntity(
                    id = 4, type = TransactionType.EXPENSE, amount = 50_000L,
                    walletId = 2, categoryId = 6, title = "Kopi", timestamp = day2
                ),
                walletName = "GoPay", targetWalletName = null,
                categoryName = "Kopi", categoryIconKey = "coffee", categoryColorHex = 0xFFD97706
            )
        )

        val ledgers = com.example.ui.components.buildDailyLedgers(details)
        assertEquals(2, ledgers.size)
        assertEquals(3, ledgers[0].items.size)
        assertEquals(500_000L, ledgers[0].totalIncome)
        assertEquals(120_000L, ledgers[0].totalExpense)
        assertEquals(380_000L, ledgers[0].netAmount)
        assertEquals(-50_000L, ledgers[1].netAmount)
    }
}
