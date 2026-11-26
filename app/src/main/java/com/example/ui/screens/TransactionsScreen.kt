package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionDetail
import com.example.data.local.TransactionType
import com.example.ui.components.DailyLedgerCard
import com.example.ui.components.EmptyFinanceState
import com.example.ui.components.buildDailyLedgers
import com.example.ui.theme.LocalFintekColors
import com.example.ui.theme.MoneySmallStyle
import com.example.ui.viewmodel.FintekUiState
import com.example.ui.viewmodel.TransactionFilterType
import com.example.util.formatCompactRupiah


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
