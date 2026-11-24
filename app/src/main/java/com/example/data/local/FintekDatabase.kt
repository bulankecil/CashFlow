package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Calendar

class FintekTypeConverters {
    @TypeConverter
    fun fromWalletType(value: WalletType): String = value.name

    @TypeConverter
    fun toWalletType(value: String): WalletType = WalletType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}

@Database(
    entities = [WalletEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(FintekTypeConverters::class)
abstract class FintekDatabase : RoomDatabase() {

    abstract fun fintekDao(): FintekDao

    companion object {
        @Volatile
        private var INSTANCE: FintekDatabase? = null

        fun getInstance(context: Context): FintekDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FintekDatabase::class.java,
                    "fintek_offline.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

object SeedDataFactory {

    fun defaultCategories(): List<CategoryEntity> = listOf(
        // Pemasukan (IDs 1..4)
        CategoryEntity(id = 1, name = "Gaji", type = TransactionType.INCOME, iconKey = "salary", colorHex = 0xFF059669),
        CategoryEntity(id = 2, name = "Bonus & Freelance", type = TransactionType.INCOME, iconKey = "bonus", colorHex = 0xFF0D9488),
        CategoryEntity(id = 3, name = "Hasil Investasi", type = TransactionType.INCOME, iconKey = "investment", colorHex = 0xFF0284C7),
        CategoryEntity(id = 4, name = "Lainnya", type = TransactionType.INCOME, iconKey = "cash", colorHex = 0xFF10B981),
        // Pengeluaran (IDs 5..13)
        CategoryEntity(id = 5, name = "Makan & Minum", type = TransactionType.EXPENSE, iconKey = "food", colorHex = 0xFFEF4444),
        CategoryEntity(id = 6, name = "Kopi & Jajan", type = TransactionType.EXPENSE, iconKey = "coffee", colorHex = 0xFFD97706),
        CategoryEntity(id = 7, name = "Transportasi", type = TransactionType.EXPENSE, iconKey = "transport", colorHex = 0xFF2563EB),
        CategoryEntity(id = 8, name = "Belanja Kebutuhan", type = TransactionType.EXPENSE, iconKey = "shopping", colorHex = 0xFFDB2777),
        CategoryEntity(id = 9, name = "Tagihan & Listrik", type = TransactionType.EXPENSE, iconKey = "bills", colorHex = 0xFF7C3AED),
        CategoryEntity(id = 10, name = "Kesehatan", type = TransactionType.EXPENSE, iconKey = "health", colorHex = 0xFF0D9488),
        CategoryEntity(id = 11, name = "Hiburan", type = TransactionType.EXPENSE, iconKey = "entertainment", colorHex = 0xFFF59E0B),
        CategoryEntity(id = 12, name = "Pendidikan", type = TransactionType.EXPENSE, iconKey = "education", colorHex = 0xFF4F46E5),
        CategoryEntity(id = 13, name = "Lainnya", type = TransactionType.EXPENSE, iconKey = "cash", colorHex = 0xFF64748B)
    )

    fun defaultWallets(now: Long = System.currentTimeMillis()): List<WalletEntity> = listOf(
        WalletEntity(
            id = 1,
            name = "BCA Utama",
            type = WalletType.BANK,
            balance = 12_450_000L,
            accentKey = "navy",
            iconKey = "bank",
            createdAt = now - 60_000_000L
        ),
        WalletEntity(
            id = 2,
            name = "GoPay",
            type = WalletType.EWALLET,
            balance = 1_680_000L,
            accentKey = "sky",
            iconKey = "ewallet",
            createdAt = now - 50_000_000L
        ),
        WalletEntity(
            id = 3,
            name = "Dompet Tunai",
            type = WalletType.TUNAI,
            balance = 640_000L,
            accentKey = "emerald",
            iconKey = "wallet",
            createdAt = now - 40_000_000L
        ),
        WalletEntity(
            id = 4,
            name = "Tabungan Darurat",
            type = WalletType.TABUNGAN,
            balance = 24_500_000L,
            accentKey = "teal",
            iconKey = "savings",
            createdAt = now - 30_000_000L
        )
    )

    fun sampleTransactions(now: Long = System.currentTimeMillis()): List<TransactionEntity> {
        fun daysAgo(days: Int, hour: Int, minute: Int = 15): Long {
            return Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -days)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        return listOf(
            // Hari ini
            TransactionEntity(
                id = 1,
                type = TransactionType.EXPENSE,
                amount = 48_000L,
                walletId = 2,
                categoryId = 6,
                title = "Kopi Susu & Croissant",
                note = "Sarapan sebelum kerja",
                timestamp = daysAgo(0, 8, 30)
            ),
            TransactionEntity(
                id = 2,
                type = TransactionType.EXPENSE,
                amount = 85_000L,
                walletId = 3,
                categoryId = 5,
                title = "Makan Siang Padang",
                note = "Makan siang bersama tim",
                timestamp = daysAgo(0, 12, 20)
            ),
            // Kemarin
            TransactionEntity(
                id = 3,
                type = TransactionType.EXPENSE,
                amount = 145_000L,
                walletId = 2,
                categoryId = 7,
                title = "Isi Bensin & Parkir",
                note = "Pertamax mingguan",
                timestamp = daysAgo(1, 18, 10)
            ),
            TransactionEntity(
                id = 4,
                type = TransactionType.TRANSFER,
                amount = 1_000_000L,
                walletId = 1,
                targetWalletId = 2,
                categoryId = 0,
                title = "Top Up GoPay",
                note = "Anggaran transport & jajan",
                timestamp = daysAgo(1, 9, 45)
            ),
            // Minggu ini
            TransactionEntity(
                id = 5,
                type = TransactionType.EXPENSE,
                amount = 420_000L,
                walletId = 1,
                categoryId = 9,
                title = "Tagihan Listrik & Internet",
                note = "Token PLN + Wi-Fi rumah",
                timestamp = daysAgo(3, 14, 0)
            ),
            TransactionEntity(
                id = 6,
                type = TransactionType.INCOME,
                amount = 1_850_000L,
                walletId = 1,
                categoryId = 2,
                title = "Desain UI Landing Page",
                note = "Proyek freelance akhir pekan",
                timestamp = daysAgo(4, 16, 40)
            ),
            TransactionEntity(
                id = 7,
                type = TransactionType.EXPENSE,
                amount = 365_000L,
                walletId = 1,
                categoryId = 8,
                title = "Belanja Bulanan Minimarket",
                note = "Kebutuhan dapur & sabun",
                timestamp = daysAgo(5, 19, 15)
            ),
            TransactionEntity(
                id = 8,
                type = TransactionType.EXPENSE,
                amount = 165_000L,
                walletId = 2,
                categoryId = 11,
                title = "Tiket Bioskop & Camilan",
                note = "Akhir pekan",
                timestamp = daysAgo(6, 20, 0)
            ),
            // Awal bulan ini
            TransactionEntity(
                id = 9,
                type = TransactionType.INCOME,
                amount = 9_500_000L,
                walletId = 1,
                categoryId = 1,
                title = "Gaji Bulanan",
                note = "Pembayaran gaji bulan berjalan",
                timestamp = daysAgo(12, 9, 0)
            ),
            TransactionEntity(
                id = 10,
                type = TransactionType.TRANSFER,
                amount = 2_500_000L,
                walletId = 1,
                targetWalletId = 4,
                categoryId = 0,
                title = "Sisihkan Tabungan Darurat",
                note = "Auto-saving rutin bulanan",
                timestamp = daysAgo(12, 10, 0)
            ),
            TransactionEntity(
                id = 11,
                type = TransactionType.EXPENSE,
                amount = 290_000L,
                walletId = 1,
                categoryId = 10,
                title = "Vitamin & Cek Kesehatan",
                note = "Apotek keluarga",
                timestamp = daysAgo(15, 11, 30)
            ),
            TransactionEntity(
                id = 12,
                type = TransactionType.EXPENSE,
                amount = 540_000L,
                walletId = 1,
                categoryId = 12,
                title = "Langganan Kursus Online",
                note = "Materi pengembangan diri",
                timestamp = daysAgo(18, 15, 10)
            ),
            // 1 bulan lalu (untuk analitik 3 bulan & tahunan)
            TransactionEntity(
                id = 13,
                type = TransactionType.INCOME,
                amount = 9_500_000L,
                walletId = 1,
                categoryId = 1,
                title = "Gaji Bulan Lalu",
                note = "Pembayaran gaji",
                timestamp = daysAgo(42, 9, 0)
            ),
            TransactionEntity(
                id = 14,
                type = TransactionType.EXPENSE,
                amount = 3_450_000L,
                walletId = 1,
                categoryId = 8,
                title = "Kebutuhan Rumah & Belanja",
                note = "Rekap belanja bulanan",
                timestamp = daysAgo(38, 13, 20)
            ),
            TransactionEntity(
                id = 15,
                type = TransactionType.EXPENSE,
                amount = 1_250_000L,
                walletId = 2,
                categoryId = 5,
                title = "Kuliner & Makan Bulanan",
                note = "Makan di luar & pesan antar",
                timestamp = daysAgo(34, 19, 45)
            ),
            // 2 bulan lalu
            TransactionEntity(
                id = 16,
                type = TransactionType.INCOME,
                amount = 9_200_000L,
                walletId = 1,
                categoryId = 1,
                title = "Gaji Dua Bulan Lalu",
                note = "Pembayaran gaji",
                timestamp = daysAgo(72, 9, 0)
            ),
            TransactionEntity(
                id = 17,
                type = TransactionType.INCOME,
                amount = 650_000L,
                walletId = 4,
                categoryId = 3,
                title = "Imbal Hasil Reksa Dana",
                note = "Dividen pasar uang",
                timestamp = daysAgo(68, 10, 30)
            ),
            TransactionEntity(
                id = 18,
                type = TransactionType.EXPENSE,
                amount = 4_100_000L,
                walletId = 1,
                categoryId = 9,
                title = "Cicilan & Utilitas Bulanan",
                note = "Sewa, listrik, dan air",
                timestamp = daysAgo(64, 11, 0)
            )
        )
    }
}
