package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WalletType {
    TUNAI,
    BANK,
    EWALLET,
    TABUNGAN
}

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: WalletType,
    val balance: Long,
    val accentKey: String,
    val iconKey: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val iconKey: String,
    val colorHex: Long,
    val isDefault: Boolean = true
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val walletId: Long,
    val targetWalletId: Long? = null,
    val categoryId: Long,
    val title: String,
    val note: String = "",
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)

data class TransactionDetail(
    val transaction: TransactionEntity,
    val walletName: String,
    val targetWalletName: String?,
    val categoryName: String,
    val categoryIconKey: String,
    val categoryColorHex: Long
)
