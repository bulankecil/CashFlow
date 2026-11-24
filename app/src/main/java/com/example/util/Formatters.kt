package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.local.WalletType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val IndonesianLocale = Locale.Builder().setLanguage("in").setRegion("ID").build()

fun formatRupiah(amount: Long, showPlusSign: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(IndonesianLocale)
    val absValue = formatter.format(abs(amount))
    return when {
        amount < 0 -> "-Rp $absValue"
        showPlusSign && amount > 0 -> "+Rp $absValue"
        else -> "Rp $absValue"
    }
}

fun formatSignedRupiah(amount: Long, isIncome: Boolean): String {
    val formatter = NumberFormat.getNumberInstance(IndonesianLocale)
    val absValue = formatter.format(abs(amount))
    return if (isIncome) "+Rp $absValue" else "-Rp $absValue"
}

fun formatCompactRupiah(amount: Long): String {
    val absVal = abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        absVal >= 1_000_000_000L -> {
            val v = absVal / 1_000_000_000.0
            "${sign}Rp ${String.format(IndonesianLocale, "%.1f", v)} M"
        }
        absVal >= 1_000_000L -> {
            val v = absVal / 1_000_000.0
            val formatted = if (absVal % 1_000_000L == 0L) {
                (absVal / 1_000_000L).toString()
            } else {
                String.format(IndonesianLocale, "%.1f", v)
            }
            "${sign}Rp $formatted jt"
        }
        absVal >= 1_000L -> {
            val v = absVal / 1_000L
            "${sign}Rp $v rb"
        }
        else -> "${sign}Rp $absVal"
    }
}

fun formatDateIndo(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", IndonesianLocale)
    return sdf.format(Date(timestamp))
}

fun formatFullDateIndo(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", IndonesianLocale)
    return sdf.format(Date(timestamp))
}

fun formatDateKey(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date(timestamp))
}

fun formatTimeIndo(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", IndonesianLocale)
    return sdf.format(Date(timestamp))
}

fun relativeDayBadge(timestamp: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val calTarget = Calendar.getInstance().apply { timeInMillis = timestamp }
    val calToday = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val calYesterday = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }

    val sameYearDay = { a: Calendar, b: Calendar ->
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    return when {
        sameYearDay(calTarget, calToday) -> "Hari Ini"
        sameYearDay(calTarget, calYesterday) -> "Kemarin"
        else -> SimpleDateFormat("EEEE", IndonesianLocale).format(Date(timestamp))
    }
}

fun formatDayHeader(timestamp: Long): String {
    return "${relativeDayBadge(timestamp)} • ${formatDateIndo(timestamp)}"
}

fun walletTypeLabel(type: WalletType): String = when (type) {
    WalletType.TUNAI -> "Tunai"
    WalletType.BANK -> "Rekening Bank"
    WalletType.EWALLET -> "E-Wallet"
    WalletType.TABUNGAN -> "Tabungan"
}

fun iconForKey(key: String): ImageVector = when (key) {
    "wallet" -> Icons.Rounded.AccountBalanceWallet
    "bank" -> Icons.Rounded.AccountBalance
    "ewallet" -> Icons.Rounded.Smartphone
    "savings" -> Icons.Rounded.Savings
    "salary", "work" -> Icons.Rounded.Work
    "bonus", "gift" -> Icons.Rounded.CardGiftcard
    "investment", "trending" -> Icons.Rounded.TrendingUp
    "food" -> Icons.Rounded.Restaurant
    "coffee" -> Icons.Rounded.LocalCafe
    "transport" -> Icons.Rounded.DirectionsCar
    "shopping" -> Icons.Rounded.ShoppingBag
    "bills" -> Icons.Rounded.ReceiptLong
    "electricity" -> Icons.Rounded.Bolt
    "health" -> Icons.Rounded.Favorite
    "entertainment" -> Icons.Rounded.Movie
    "education" -> Icons.Rounded.School
    "home" -> Icons.Rounded.Home
    "phone" -> Icons.Rounded.PhoneAndroid
    "transfer" -> Icons.Rounded.SwapHoriz
    "cash" -> Icons.Rounded.Payments
    else -> Icons.Rounded.AttachMoney
}

fun accentColorForKey(key: String): Color = when (key) {
    "emerald" -> Color(0xFF059669)
    "navy" -> Color(0xFF1E3A8A)
    "sky" -> Color(0xFF0284C7)
    "amber" -> Color(0xFFD97706)
    "teal" -> Color(0xFF0D9488)
    "indigo" -> Color(0xFF4F46E5)
    "rose" -> Color(0xFFE11D48)
    else -> Color(0xFF059669)
}

val AvailableWalletAccents = listOf(
    "emerald" to "Zamrud",
    "navy" to "Biru Navy",
    "sky" to "Biru Langit",
    "teal" to "Teal",
    "indigo" to "Indigo",
    "amber" to "Emas"
)

val AvailableCategoryIcons = listOf(
    "food" to "Makanan",
    "coffee" to "Kopi",
    "transport" to "Transportasi",
    "shopping" to "Belanja",
    "bills" to "Tagihan",
    "electricity" to "Listrik",
    "health" to "Kesehatan",
    "entertainment" to "Hiburan",
    "education" to "Edukasi",
    "home" to "Rumah",
    "phone" to "Pulsa/Data",
    "salary" to "Gaji",
    "bonus" to "Hadiah",
    "investment" to "Investasi",
    "cash" to "Lainnya"
)

val AvailableCategoryColors = listOf(
    0xFF059669L,
    0xFF0D9488L,
    0xFF0284C7L,
    0xFF2563EBL,
    0xFF4F46E5L,
    0xFF7C3AEDL,
    0xFFDB2777L,
    0xFFEF4444L,
    0xFFD97706L,
    0xFF64748BL
)
