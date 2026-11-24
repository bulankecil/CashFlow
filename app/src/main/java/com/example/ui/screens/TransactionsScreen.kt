package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionDetail
import com.example.data.local.TransactionType
import com.example.ui.components.EmptyFinanceState
import com.example.ui.theme.LocalFintekColors
import com.example.ui.theme.MoneyMediumStyle
import com.example.ui.theme.MoneySmallStyle
import com.example.ui.viewmodel.FintekUiState
import com.example.ui.viewmodel.TransactionFilterType
import com.example.util.formatCompactRupiah
import com.example.util.formatDateKey
import com.example.util.formatFullDateIndo
import com.example.util.formatRupiah
import com.example.util.formatSignedRupiah
import com.example.util.formatTimeIndo
import com.example.util.iconForKey
import com.example.util.relativeDayBadge

data class DailyLedgerGroup(
    val dateKey: String,
    val dateTitle: String,
    val dayBadge: String,
    val items: List<TransactionDetail>,
    val totalIncome: Long,
    val totalExpense: Long,
    val netAmount: Long
)

fun buildDailyLedgers(details: List<TransactionDetail>): List<DailyLedgerGroup> {
    if (details.isEmpty()) return emptyList()
    return details
        .sortedByDescending { it.transaction.timestamp }
        .groupBy { formatDateKey(it.transaction.timestamp) }
        .map { (dateKey, dayItems) ->
            val repTimestamp = dayItems.first().transaction.timestamp
            val income = dayItems
                .filter { it.transaction.type == TransactionType.INCOME }
                .sumOf { it.transaction.amount }
            val expense = dayItems
                .filter { it.transaction.type == TransactionType.EXPENSE }
                .sumOf { it.transaction.amount }

            DailyLedgerGroup(
                dateKey = dateKey,
                dateTitle = formatFullDateIndo(repTimestamp),
                dayBadge = relativeDayBadge(repTimestamp),
                items = dayItems,
                totalIncome = income,
                totalExpense = expense,
                netAmount = income - expense
            )
        }
}

@Composable
fun TransactionsScreen(
    uiState: FintekUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterTypeChange: (TransactionFilterType) -> Unit,
    onWalletFilterChange: (Long?) -> Unit,
    onEditTransaction: (TransactionDetail) -> Unit,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fintekColors = LocalFintekColors.current
    val dailyLedgers = remember(uiState.filteredTransactions) {
        buildDailyLedgers(uiState.filteredTransactions)
    }

    var expandedDateKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasInitializedDefaultExpand by remember { mutableStateOf(false) }

    // Buka otomatis kartu tanggal terbaru saat pertama kali dimuat atau saat pencarian aktif
    LaunchedEffect(dailyLedgers, uiState.searchQuery) {
        val newestKey = dailyLedgers.firstOrNull()?.dateKey
        if (uiState.searchQuery.isNotBlank()) {
            expandedDateKeys = dailyLedgers.map { it.dateKey }.toSet()
        } else if (!hasInitializedDefaultExpand && newestKey != null) {
            expandedDateKeys = setOf(newestKey)
            hasInitializedDefaultExpand = true
        } else if (newestKey != null && expandedDateKeys.none { key -> dailyLedgers.any { it.dateKey == key } }) {
            expandedDateKeys = setOf(newestKey)
        }
    }

    val allExpanded = dailyLedgers.isNotEmpty() && dailyLedgers.all { it.dateKey in expandedDateKeys }

    val filteredIncome = remember(uiState.filteredTransactions) {
        uiState.filteredTransactions
            .filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
    }
    val filteredExpense = remember(uiState.filteredTransactions) {
        uiState.filteredTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Buku Kas Harian",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Ledger transaksi dikelompokkan per tanggal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (dailyLedgers.size > 1) {
                    TextButton(
                        onClick = {
                            expandedDateKeys = if (allExpanded) {
                                emptySet()
                            } else {
                                dailyLedgers.map { it.dateKey }.toSet()
                            }
                        },
                        modifier = Modifier.testTag("ledger_toggle_all_button")
                    ) {
                        Icon(
                            imageVector = if (allExpanded) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (allExpanded) "Lipat Semua" else "Buka Semua")
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Cari judul, kategori, atau dompet...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_search_input")
            )
        }

        // Type Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionFilterType.entries.forEach { typeFilter ->
                    FilterChip(
                        selected = uiState.filterType == typeFilter,
                        onClick = { onFilterTypeChange(typeFilter) },
                        label = { Text(typeFilter.label) },
                        modifier = Modifier.testTag("filter_type_${typeFilter.name.lowercase()}")
                    )
                }
            }
        }

        // Wallet Filter Chips
        if (uiState.wallets.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedWalletFilterId == null,
                        onClick = { onWalletFilterChange(null) },
                        label = { Text("Semua Dompet") }
                    )
                    uiState.wallets.forEach { wallet ->
                        FilterChip(
                            selected = uiState.selectedWalletFilterId == wallet.id,
                            onClick = {
                                val next = if (uiState.selectedWalletFilterId == wallet.id) null else wallet.id
                                onWalletFilterChange(next)
                            },
                            label = { Text(wallet.name) }
                        )
                    }
                }
            }
        }

        // Filtered Totals Strip
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, fintekColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${dailyLedgers.size} Tanggal • ${uiState.filteredTransactions.size} Transaksi",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Sesuai filter aktif",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Masuk",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCompactRupiah(filteredIncome),
                                style = MoneySmallStyle.copy(fontWeight = FontWeight.Bold),
                                color = fintekColors.income
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Keluar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCompactRupiah(filteredExpense),
                                style = MoneySmallStyle.copy(fontWeight = FontWeight.Bold),
                                color = fintekColors.expense
                            )
                        }
                    }
                }
            }
        }

        if (dailyLedgers.isEmpty()) {
            item {
                EmptyFinanceState(
                    title = "Tidak Ada Transaksi Ditemukan",
                    subtitle = "Ubah kata kunci atau filter di atas, atau catat transaksi baru sekarang.",
                    actionLabel = "Catat Transaksi",
                    onActionClick = onAddTransactionClick
                )
            }
        } else {
            items(dailyLedgers, key = { it.dateKey }) { ledger ->
                val isExpanded = ledger.dateKey in expandedDateKeys
                DailyLedgerCard(
                    ledger = ledger,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedDateKeys = if (isExpanded) {
                            expandedDateKeys - ledger.dateKey
                        } else {
                            expandedDateKeys + ledger.dateKey
                        }
                    },
                    onEditTransaction = onEditTransaction
                )
            }
        }

        item { Spacer(modifier = Modifier.height(132.dp)) }
    }
}

@Composable
private fun DailyLedgerCard(
    ledger: DailyLedgerGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditTransaction: (TransactionDetail) -> Unit
) {
    val fintekColors = LocalFintekColors.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "ledgerChevron"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, fintekColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_card_${ledger.dateKey}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Ledger Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(fintekColors.subtleSurface)
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("ledger_header_${ledger.dateKey}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = ledger.dateTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (ledger.dayBadge == "Hari Ini") {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Text(
                                text = ledger.dayBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ledger.dayBadge == "Hari Ini") {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${ledger.items.size} mutasi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Lipat ledger" else "Buka ledger",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Daily summary row: Masuk | Keluar | Selisih Bersih
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LedgerHeaderMetric(
                        label = "Masuk",
                        value = formatSignedRupiah(ledger.totalIncome, isIncome = true),
                        valueColor = fintekColors.income,
                        modifier = Modifier.weight(1f)
                    )
                    LedgerHeaderMetric(
                        label = "Keluar",
                        value = formatSignedRupiah(ledger.totalExpense, isIncome = false),
                        valueColor = fintekColors.expense,
                        modifier = Modifier.weight(1f)
                    )
                    LedgerHeaderMetric(
                        label = "Bersih",
                        value = formatRupiah(ledger.netAmount, showPlusSign = true),
                        valueColor = if (ledger.netAmount >= 0L) fintekColors.income else fintekColors.expense,
                        modifier = Modifier.weight(1f),
                        alignEnd = true
                    )
                }
            }

            // Expandable Ledger Rows
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = fintekColors.cardBorder.copy(alpha = 0.7f))
                    ledger.items.forEachIndexed { index, detail ->
                        LedgerTransactionRow(
                            detail = detail,
                            onClick = { onEditTransaction(detail) }
                        )
                        if (index < ledger.items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.8.dp,
                                color = fintekColors.cardBorder.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerHeaderMetric(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MoneySmallStyle.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LedgerTransactionRow(
    detail: TransactionDetail,
    onClick: () -> Unit
) {
    val tx = detail.transaction
    val fintekColors = LocalFintekColors.current
    val categoryColor = Color(detail.categoryColorHex)

    val amountColor = when (tx.type) {
        TransactionType.INCOME -> fintekColors.income
        TransactionType.EXPENSE -> fintekColors.expense
        TransactionType.TRANSFER -> fintekColors.transfer
    }

    val formattedAmount = when (tx.type) {
        TransactionType.INCOME -> formatSignedRupiah(tx.amount, isIncome = true)
        TransactionType.EXPENSE -> formatSignedRupiah(tx.amount, isIncome = false)
        TransactionType.TRANSFER -> formatRupiah(tx.amount)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag("transaction_item_${tx.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(categoryColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForKey(detail.categoryIconKey),
                contentDescription = detail.categoryName,
                tint = categoryColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = detail.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (tx.type == TransactionType.TRANSFER && detail.targetWalletName != null) {
                    Text(
                        text = detail.walletName,
                        style = MaterialTheme.typography.labelSmall,
                        color = fintekColors.transfer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = fintekColors.transfer,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = detail.targetWalletName,
                        style = MaterialTheme.typography.labelSmall,
                        color = fintekColors.transfer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Text(
                        text = detail.walletName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formattedAmount,
                style = MoneyMediumStyle.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
                maxLines = 1
            )
            Text(
                text = formatTimeIndo(tx.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
