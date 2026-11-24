package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FintekDao {

    @Query("SELECT * FROM wallets ORDER BY createdAt ASC, id ASC")
    fun observeWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets ORDER BY createdAt ASC, id ASC")
    suspend fun getWalletsOnce(): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id LIMIT 1")
    suspend fun getWalletById(id: Long): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallets(wallets: List<WalletEntity>): List<Long>

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Query("UPDATE wallets SET balance = balance + :delta WHERE id = :walletId")
    suspend fun adjustWalletBalance(walletId: Long, delta: Long)

    @Query("DELETE FROM wallets WHERE id = :walletId")
    suspend fun deleteWalletById(walletId: Long)

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE type = :type AND LOWER(name) = 'lainnya' LIMIT 1")
    suspend fun getFallbackCategory(type: TransactionType): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: Long): Int

    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignTransactionsCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    @Transaction
    suspend fun deleteCategoryAndReassign(categoryId: Long): Int {
        val target = getCategoryById(categoryId) ?: return -1
        if (target.name.equals("Lainnya", ignoreCase = true)) {
            return -2
        }
        val fallback = getFallbackCategory(target.type)
        val fallbackId = fallback?.id ?: insertCategory(
            CategoryEntity(
                name = "Lainnya",
                type = target.type,
                iconKey = "cash",
                colorHex = 0xFF64748B,
                isDefault = true
            )
        )
        val movedCount = countTransactionsForCategory(target.id)
        if (movedCount > 0) {
            reassignTransactionsCategory(oldCategoryId = target.id, newCategoryId = fallbackId)
        }
        deleteCategoryById(target.id)
        return movedCount
    }

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :walletId OR targetWalletId = :walletId")
    suspend fun countTransactionsForWallet(walletId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM wallets")
    suspend fun clearWallets()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Transaction
    suspend fun insertTransactionAndAdjustBalance(tx: TransactionEntity): Long {
        val insertedId = insertTransaction(tx)
        applyBalanceDelta(tx, multiplier = 1)
        return insertedId
    }

    @Transaction
    suspend fun updateTransactionAndAdjustBalance(newTx: TransactionEntity) {
        val existing = getTransactionById(newTx.id)
        if (existing != null) {
            applyBalanceDelta(existing, multiplier = -1)
        }
        updateTransaction(newTx)
        applyBalanceDelta(newTx, multiplier = 1)
    }

    @Transaction
    suspend fun deleteTransactionAndAdjustBalance(transactionId: Long) {
        val existing = getTransactionById(transactionId) ?: return
        applyBalanceDelta(existing, multiplier = -1)
        deleteTransactionById(transactionId)
    }

    @Transaction
    suspend fun replaceAllData(
        wallets: List<WalletEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>
    ) {
        clearTransactions()
        clearWallets()
        clearCategories()
        insertWallets(wallets)
        insertCategories(categories)
        insertTransactions(transactions)
    }

    private suspend fun applyBalanceDelta(tx: TransactionEntity, multiplier: Long) {
        val signedAmount = tx.amount * multiplier
        when (tx.type) {
            TransactionType.INCOME -> {
                adjustWalletBalance(tx.walletId, signedAmount)
            }
            TransactionType.EXPENSE -> {
                adjustWalletBalance(tx.walletId, -signedAmount)
            }
            TransactionType.TRANSFER -> {
                adjustWalletBalance(tx.walletId, -signedAmount)
                tx.targetWalletId?.let { targetId ->
                    adjustWalletBalance(targetId, signedAmount)
                }
            }
        }
    }
}
