package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.data.local.WalletType
import com.example.data.repository.FinanceRepository
import com.example.data.repository.ParsedCsvRow
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = false)
data class FintekBackupPayload(
    val version: Int = 1,
    val app: String = "Fintek",
    val exportedAt: Long,
    val wallets: List<WalletBackupDto>,
    val categories: List<CategoryBackupDto>,
    val transactions: List<TransactionBackupDto>
)

@JsonClass(generateAdapter = false)
data class WalletBackupDto(
    val id: Long,
    val name: String,
    val type: String,
    val balance: Long,
    val accentKey: String,
    val iconKey: String,
    val createdAt: Long
)

@JsonClass(generateAdapter = false)
data class CategoryBackupDto(
    val id: Long,
    val name: String,
    val type: String,
    val iconKey: String,
    val colorHex: Long,
    val isDefault: Boolean
)

@JsonClass(generateAdapter = false)
data class TransactionBackupDto(
    val id: Long,
    val type: String,
    val amount: Long,
    val walletId: Long,
    val targetWalletId: Long? = null,
    val categoryId: Long,
    val title: String,
    val note: String,
    val timestamp: Long,
    val createdAt: Long
)

class BackupManager(
    private val context: Context,
    private val repository: FinanceRepository
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(FintekBackupPayload::class.java).indent("  ")

    suspend fun exportJson(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val (wallets, categories, transactions) = repository.getSnapshot()
            val payload = FintekBackupPayload(
                exportedAt = System.currentTimeMillis(),
                wallets = wallets.map {
                    WalletBackupDto(it.id, it.name, it.type.name, it.balance, it.accentKey, it.iconKey, it.createdAt)
                },
                categories = categories.map {
                    CategoryBackupDto(it.id, it.name, it.type.name, it.iconKey, it.colorHex, it.isDefault)
                },
                transactions = transactions.map {
                    TransactionBackupDto(
                        it.id, it.type.name, it.amount, it.walletId, it.targetWalletId,
                        it.categoryId, it.title, it.note, it.timestamp, it.createdAt
                    )
                }
            )
            val json = payloadAdapter.toJson(payload)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Tidak dapat membuka berkas tujuan.")
            "Cadangan JSON berhasil diekspor (${wallets.size} dompet, ${transactions.size} transaksi)."
        }
    }

    suspend fun importJson(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Tidak dapat membaca berkas JSON.")

            val payload = payloadAdapter.fromJson(content)
                ?: error("Format berkas JSON Fintek tidak valid.")

            val wallets = payload.wallets.map {
                WalletEntity(
                    id = it.id,
                    name = it.name,
                    type = runCatching { WalletType.valueOf(it.type) }.getOrDefault(WalletType.TUNAI),
                    balance = it.balance,
                    accentKey = it.accentKey,
                    iconKey = it.iconKey,
                    createdAt = it.createdAt
                )
            }
            val categories = payload.categories.map {
                CategoryEntity(
                    id = it.id,
                    name = it.name,
                    type = runCatching { TransactionType.valueOf(it.type) }.getOrDefault(TransactionType.EXPENSE),
                    iconKey = it.iconKey,
                    colorHex = it.colorHex,
                    isDefault = it.isDefault
                )
            }
            val transactions = payload.transactions.map {
                TransactionEntity(
                    id = it.id,
                    type = runCatching { TransactionType.valueOf(it.type) }.getOrDefault(TransactionType.EXPENSE),
                    amount = it.amount,
                    walletId = it.walletId,
                    targetWalletId = it.targetWalletId,
                    categoryId = it.categoryId,
                    title = it.title,
                    note = it.note,
                    timestamp = it.timestamp,
                    createdAt = it.createdAt
                )
            }

            repository.restoreSnapshot(wallets, categories, transactions)
            "Berhasil memulihkan ${wallets.size} dompet dan ${transactions.size} transaksi dari JSON."
        }
    }

    suspend fun exportCsv(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val (wallets, categories, transactions) = repository.getSnapshot()
            val walletMap = wallets.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val timeFmt = SimpleDateFormat("HH:mm", Locale.US)

            val sb = StringBuilder()
            sb.append('\uFEFF') // UTF-8 BOM untuk Excel
            sb.appendLine("Tanggal,Waktu,Tipe,Judul,Kategori,Dompet,Dompet Tujuan,Nominal,Catatan")

            for (tx in transactions) {
                val date = Date(tx.timestamp)
                val tipeStr = when (tx.type) {
                    TransactionType.INCOME -> "PEMASUKAN"
                    TransactionType.EXPENSE -> "PENGELUARAN"
                    TransactionType.TRANSFER -> "TRANSFER"
                }
                val catName = if (tx.type == TransactionType.TRANSFER) {
                    "Transfer Antar Dompet"
                } else {
                    categoryMap[tx.categoryId]?.name ?: "Lainnya"
                }
                val walletName = walletMap[tx.walletId]?.name ?: "Dompet"
                val targetName = tx.targetWalletId?.let { walletMap[it]?.name } ?: ""

                sb.appendLine(
                    listOf(
                        dateFmt.format(date),
                        timeFmt.format(date),
                        tipeStr,
                        escapeCsv(tx.title),
                        escapeCsv(catName),
                        escapeCsv(walletName),
                        escapeCsv(targetName),
                        tx.amount.toString(),
                        escapeCsv(tx.note)
                    ).joinToString(",")
                )
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            } ?: error("Tidak dapat menulis berkas CSV.")

            "Laporan CSV berhasil diekspor (${transactions.size} transaksi)."
        }
    }

    suspend fun importCsv(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val lines = context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readLines()
            } ?: error("Tidak dapat membaca berkas CSV.")

            val nonEmptyLines = lines.map { it.trimStart('\uFEFF') }.filter { it.isNotBlank() }
            if (nonEmptyLines.size <= 1) {
                error("Berkas CSV kosong atau tidak memiliki baris transaksi.")
            }

            val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val dateOnlyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsedRows = mutableListOf<ParsedCsvRow>()

            for (line in nonEmptyLines.drop(1)) {
                val cols = parseCsvLine(line)
                if (cols.size < 8) continue

                val dateStr = cols[0].trim()
                val timeStr = cols[1].trim().ifEmpty { "12:00" }
                val timestamp = runCatching { dateTimeFmt.parse("$dateStr $timeStr")?.time }.getOrNull()
                    ?: runCatching { dateOnlyFmt.parse(dateStr)?.time }.getOrNull()
                    ?: System.currentTimeMillis()

                val typeRaw = cols[2].trim().uppercase(Locale.US)
                val type = when {
                    typeRaw.contains("MASUK") || typeRaw == "INCOME" -> TransactionType.INCOME
                    typeRaw.contains("TRANSFER") -> TransactionType.TRANSFER
                    else -> TransactionType.EXPENSE
                }

                val title = cols[3].trim().ifEmpty { "Transaksi Impor" }
                val category = cols[4].trim().ifEmpty { "Lainnya" }
                val walletName = cols[5].trim().ifEmpty { "Dompet Tunai" }
                val targetWalletName = cols[6].trim().ifBlank { null }
                val amount = cols[7].trim().replace(".", "").replace(",", "").toLongOrNull() ?: 0L
                val note = cols.getOrNull(8)?.trim() ?: ""

                if (amount > 0L) {
                    parsedRows.add(
                        ParsedCsvRow(
                            timestamp = timestamp,
                            type = type,
                            title = title,
                            categoryName = category,
                            walletName = walletName,
                            targetWalletName = targetWalletName,
                            amount = amount,
                            note = note
                        )
                    )
                }
            }

            if (parsedRows.isEmpty()) {
                error("Tidak ada baris transaksi valid ditemukan di CSV.")
            }

            val count = repository.importParsedCsvTransactions(parsedRows)
            "Berhasil mengimpor $count transaksi dari berkas CSV."
        }
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\n", " ").replace("\r", " ")
        return if (clean.contains(",") || clean.contains("\"")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
