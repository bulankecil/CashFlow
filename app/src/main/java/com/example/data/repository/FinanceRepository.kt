package com.example.data.repository

import com.example.data.local.CategoryEntity
import com.example.data.local.FintekDao
import com.example.data.local.SeedDataFactory
import com.example.data.local.ThemePreferences
import com.example.data.local.TransactionDetail
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FinanceRepository(
    private val dao: FintekDao,
    private val themePreferences: ThemePreferences
) {
    val wallets: Flow<List<WalletEntity>> = dao.observeWallets()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()

    val transactionDetails: Flow<List<TransactionDetail>> = combine(
        transactions,
        wallets,
        categories
    ) { txList, walletList, categoryList ->
        val walletMap = walletList.associateBy { it.id }
        val categoryMap = categoryList.associateBy { it.id }

        txList.map { tx ->
            val sourceWallet = walletMap[tx.walletId]
            val targetWallet = tx.targetWalletId?.let { walletMap[it] }
            val category = categoryMap[tx.categoryId]
            val isTransfer = tx.type == TransactionType.TRANSFER

            TransactionDetail(
                transaction = tx,
                walletName = sourceWallet?.name ?: "Dompet Terhapus",
                targetWalletName = targetWallet?.name,
                categoryName = if (isTransfer) "Transfer Antar Dompet" else (category?.name ?: "Lainnya"),
                categoryIconKey = if (isTransfer) "transfer" else (category?.iconKey ?: "cash"),
                categoryColorHex = if (isTransfer) 0xFF0284C7 else (category?.colorHex ?: 0xFF059669)
            )
        }
    }

    suspend fun ensureInitialSeedIfNeeded() {
        val alreadySeeded = themePreferences.hasSeededOnce()
        val existingWallets = dao.getWalletsOnce()
        val existingCategories = dao.getCategoriesOnce()
        if (!alreadySeeded && existingWallets.isEmpty() && existingCategories.isEmpty()) {
            resetToSeedData()
            themePreferences.markSeededOnce()
        } else if (existingCategories.isEmpty()) {
            dao.insertCategories(SeedDataFactory.defaultCategories())
        } else {
            if (dao.getFallbackCategory(TransactionType.EXPENSE) == null) {
                dao.insertCategory(
                    CategoryEntity(
                        name = "Lainnya",
                        type = TransactionType.EXPENSE,
                        iconKey = "cash",
                        colorHex = 0xFF64748B,
                        isDefault = true
                    )
                )
            }
            if (dao.getFallbackCategory(TransactionType.INCOME) == null) {
                dao.insertCategory(
                    CategoryEntity(
                        name = "Lainnya",
                        type = TransactionType.INCOME,
                        iconKey = "cash",
                        colorHex = 0xFF10B981,
                        isDefault = true
                    )
                )
            }
        }
    }

    suspend fun resetToSeedData() {
        val now = System.currentTimeMillis()
        dao.replaceAllData(
            wallets = SeedDataFactory.defaultWallets(now),
            categories = SeedDataFactory.defaultCategories(),
            transactions = SeedDataFactory.sampleTransactions(now)
        )
        themePreferences.markSeededOnce()
    }

    suspend fun clearAllDataKeepCategories() {
        dao.replaceAllData(
            wallets = emptyList(),
            categories = SeedDataFactory.defaultCategories(),
            transactions = emptyList()
        )
        themePreferences.markSeededOnce()
    }

    suspend fun addWallet(wallet: WalletEntity): Long {
        return dao.insertWallet(wallet)
    }

    suspend fun updateWallet(wallet: WalletEntity) {
        dao.updateWallet(wallet)
    }

    suspend fun canDeleteWallet(walletId: Long): Boolean {
        return dao.countTransactionsForWallet(walletId) == 0
    }

    suspend fun deleteWallet(walletId: Long): Boolean {
        if (!canDeleteWallet(walletId)) return false
        dao.deleteWalletById(walletId)
        return true
    }

    suspend fun saveTransaction(tx: TransactionEntity) {
        if (tx.id == 0L) {
            dao.insertTransactionAndAdjustBalance(tx)
        } else {
            dao.updateTransactionAndAdjustBalance(tx)
        }
    }

    suspend fun deleteTransaction(transactionId: Long) {
        dao.deleteTransactionAndAdjustBalance(transactionId)
    }

    suspend fun saveCategory(category: CategoryEntity): Result<String> {
        val trimmedName = category.name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Nama kategori wajib diisi."))
        }
        val existing = dao.getCategoriesOnce()
        val duplicate = existing.any {
            it.id != category.id &&
                it.type == category.type &&
                it.name.trim().equals(trimmedName, ignoreCase = true)
        }
        if (duplicate) {
            return Result.failure(IllegalStateException("Kategori \"$trimmedName\" sudah ada."))
        }
        return if (category.id == 0L) {
            dao.insertCategory(category.copy(name = trimmedName, isDefault = false))
            Result.success("Kategori \"$trimmedName\" ditambahkan.")
        } else {
            dao.updateCategory(category.copy(name = trimmedName))
            Result.success("Kategori \"$trimmedName\" diperbarui.")
        }
    }

    suspend fun deleteCategoryReassigningToFallback(categoryId: Long): Result<String> {
        return when (val movedCount = dao.deleteCategoryAndReassign(categoryId)) {
            -1 -> Result.failure(IllegalStateException("Kategori tidak ditemukan."))
            -2 -> Result.failure(IllegalStateException("Kategori \"Lainnya\" adalah kategori penampung bawaan dan tidak dapat dihapus."))
            0 -> Result.success("Kategori berhasil dihapus.")
            else -> Result.success("Kategori dihapus; $movedCount transaksi dipindahkan ke \"Lainnya\".")
        }
    }

    suspend fun getSnapshot(): Triple<List<WalletEntity>, List<CategoryEntity>, List<TransactionEntity>> {
        return Triple(
            dao.getWalletsOnce(),
            dao.getCategoriesOnce(),
            dao.getTransactionsOnce()
        )
    }

    suspend fun restoreSnapshot(
        wallets: List<WalletEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>
    ) {
        val finalCategories = categories.ifEmpty { SeedDataFactory.defaultCategories() }
        dao.replaceAllData(wallets, finalCategories, transactions)
        themePreferences.markSeededOnce()
    }

    suspend fun importParsedCsvTransactions(
        parsedRows: List<ParsedCsvRow>
    ): Int {
        val existingWallets = dao.getWalletsOnce().associateBy { it.name.trim().lowercase() }.toMutableMap()
        val existingCategories = dao.getCategoriesOnce().associateBy {
            "${it.type.name}:${it.name.trim().lowercase()}"
        }.toMutableMap()

        var importedCount = 0
        for (row in parsedRows) {
            val sourceKey = row.walletName.trim().lowercase()
            val sourceWallet = existingWallets[sourceKey] ?: run {
                val created = WalletEntity(
                    name = row.walletName.trim().ifEmpty { "Dompet Impor" },
                    type = com.example.data.local.WalletType.TUNAI,
                    balance = 0L,
                    accentKey = "emerald",
                    iconKey = "wallet"
                )
                val newId = dao.insertWallet(created)
                created.copy(id = newId).also { existingWallets[sourceKey] = it }
            }

            val targetWallet = row.targetWalletName?.takeIf { it.isNotBlank() }?.let { targetName ->
                val targetKey = targetName.trim().lowercase()
                existingWallets[targetKey] ?: run {
                    val created = WalletEntity(
                        name = targetName.trim(),
                        type = com.example.data.local.WalletType.BANK,
                        balance = 0L,
                        accentKey = "navy",
                        iconKey = "bank"
                    )
                    val newId = dao.insertWallet(created)
                    created.copy(id = newId).also { existingWallets[targetKey] = it }
                }
            }

            val categoryId = if (row.type == TransactionType.TRANSFER) {
                0L
            } else {
                val catKey = "${row.type.name}:${row.categoryName.trim().lowercase()}"
                val category = existingCategories[catKey] ?: run {
                    val created = CategoryEntity(
                        name = row.categoryName.trim().ifEmpty { "Lainnya" },
                        type = row.type,
                        iconKey = if (row.type == TransactionType.INCOME) "cash" else "bills",
                        colorHex = if (row.type == TransactionType.INCOME) 0xFF059669 else 0xFF64748B,
                        isDefault = false
                    )
                    val newId = dao.insertCategory(created)
                    created.copy(id = newId).also { existingCategories[catKey] = it }
                }
                category.id
            }

            dao.insertTransactionAndAdjustBalance(
                TransactionEntity(
                    type = row.type,
                    amount = row.amount,
                    walletId = sourceWallet.id,
                    targetWalletId = targetWallet?.id,
                    categoryId = categoryId,
                    title = row.title,
                    note = row.note,
                    timestamp = row.timestamp
                )
            )
            importedCount++
        }
        return importedCount
    }
}

data class ParsedCsvRow(
    val timestamp: Long,
    val type: TransactionType,
    val title: String,
    val categoryName: String,
    val walletName: String,
    val targetWalletName: String?,
    val amount: Long,
    val note: String
)
