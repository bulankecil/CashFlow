package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.local.CategoryEntity
import com.example.data.local.FintekDatabase
import com.example.data.local.ThemeMode
import com.example.data.local.ThemePreferences
import com.example.data.local.TransactionDetail
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.data.local.WalletType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FintekTab(val label: String) {
    DASHBOARD("Beranda"),
    TRANSACTIONS("Transaksi"),
    ANALYTICS("Analitik"),
    WALLETS("Dompet"),
    SETTINGS("Pengaturan")
}

enum class TransactionFilterType(val label: String) {
    ALL("Semua"),
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
    TRANSFER("Transfer")
}

data class DashboardSummary(
    val totalBalance: Long = 0L,
    val monthIncome: Long = 0L,
    val monthExpense: Long = 0L,
    val monthNet: Long = 0L,
    val savingsRatePercent: Int = 0,
    val sparklineValues: List<Float> = emptyList()
)

data class FintekUiState(
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val allTransactions: List<TransactionDetail> = emptyList(),
    val filteredTransactions: List<TransactionDetail> = emptyList(),
    val recentTransactions: List<TransactionDetail> = emptyList(),
    val dashboardSummary: DashboardSummary = DashboardSummary(),
    val analytics: AnalyticsUiState = AnalyticsUiState(),
    val searchQuery: String = "",
    val filterType: TransactionFilterType = TransactionFilterType.ALL,
    val selectedWalletFilterId: Long? = null,
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.MONTH,
    val isLoading: Boolean = true
)

class FintekViewModel(application: Application) : AndroidViewModel(application) {

    private val themePrefs = ThemePreferences(application)
    private val dao = FintekDatabase.getInstance(application).fintekDao()
    private val repository = FinanceRepository(dao, themePrefs)
    private val backupManager = BackupManager(application, repository)

    val themeMode: StateFlow<ThemeMode> = themePrefs.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    private val _selectedTab = MutableStateFlow(FintekTab.DASHBOARD)
    val selectedTab: StateFlow<FintekTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow(TransactionFilterType.ALL)
    private val _selectedWalletFilterId = MutableStateFlow<Long?>(null)
    private val _selectedPeriod = MutableStateFlow(AnalyticsPeriod.MONTH)
    private val _isInitialized = MutableStateFlow(false)

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val uiState: StateFlow<FintekUiState> = combine(
        repository.wallets,
        repository.categories,
        repository.transactionDetails,
        combine(_searchQuery, _filterType, _selectedWalletFilterId, _selectedPeriod, _isInitialized) {
            query, typeFilter, walletFilter, period, initialized ->
            FilterParams(query, typeFilter, walletFilter, period, initialized)
        }
    ) { wallets, categories, details, params ->
        val totalBalance = wallets.sumOf { it.balance }
        val monthAnalytics = AnalyticsCalculator.compute(details, AnalyticsPeriod.MONTH)
        val selectedAnalytics = if (params.period == AnalyticsPeriod.MONTH) {
            monthAnalytics
        } else {
            AnalyticsCalculator.compute(details, params.period)
        }

        val filtered = details.filter { item ->
            val matchesQuery = params.query.isBlank() ||
                item.transaction.title.contains(params.query, ignoreCase = true) ||
                item.transaction.note.contains(params.query, ignoreCase = true) ||
                item.categoryName.contains(params.query, ignoreCase = true) ||
                item.walletName.contains(params.query, ignoreCase = true)

            val matchesType = when (params.typeFilter) {
                TransactionFilterType.ALL -> true
                TransactionFilterType.INCOME -> item.transaction.type == TransactionType.INCOME
                TransactionFilterType.EXPENSE -> item.transaction.type == TransactionType.EXPENSE
                TransactionFilterType.TRANSFER -> item.transaction.type == TransactionType.TRANSFER
            }

            val matchesWallet = params.walletFilterId == null ||
                item.transaction.walletId == params.walletFilterId ||
                item.transaction.targetWalletId == params.walletFilterId

            matchesQuery && matchesType && matchesWallet
        }

        FintekUiState(
            wallets = wallets,
            categories = categories,
            allTransactions = details,
            filteredTransactions = filtered,
            recentTransactions = details.take(6),
            dashboardSummary = DashboardSummary(
                totalBalance = totalBalance,
                monthIncome = monthAnalytics.totalIncome,
                monthExpense = monthAnalytics.totalExpense,
                monthNet = monthAnalytics.netCashFlow,
                savingsRatePercent = monthAnalytics.savingsRatePercent,
                sparklineValues = monthAnalytics.sparklineValues
            ),
            analytics = selectedAnalytics,
            searchQuery = params.query,
            filterType = params.typeFilter,
            selectedWalletFilterId = params.walletFilterId,
            selectedPeriod = params.period,
            isLoading = !params.initialized
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FintekUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialSeedIfNeeded()
            _isInitialized.value = true
        }
    }

    fun selectTab(tab: FintekTab) {
        _selectedTab.value = tab
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePrefs.setThemeMode(mode)
        }
    }

    fun toggleLightDark(isCurrentlyDark: Boolean) {
        val next = if (isCurrentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(next)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: TransactionFilterType) {
        _filterType.value = type
    }

    fun setWalletFilter(walletId: Long?) {
        _selectedWalletFilterId.value = walletId
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        _selectedPeriod.value = period
    }

    fun saveTransaction(
        existingId: Long = 0L,
        type: TransactionType,
        amount: Long,
        walletId: Long,
        targetWalletId: Long?,
        categoryId: Long,
        title: String,
        note: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            val cleanTitle = title.trim().ifEmpty {
                when (type) {
                    TransactionType.INCOME -> "Pemasukan"
                    TransactionType.EXPENSE -> "Pengeluaran"
                    TransactionType.TRANSFER -> "Transfer Antar Dompet"
                }
            }
            repository.saveTransaction(
                TransactionEntity(
                    id = existingId,
                    type = type,
                    amount = amount,
                    walletId = walletId,
                    targetWalletId = if (type == TransactionType.TRANSFER) targetWalletId else null,
                    categoryId = if (type == TransactionType.TRANSFER) 0L else categoryId,
                    title = cleanTitle,
                    note = note.trim(),
                    timestamp = timestamp
                )
            )
            _snackbarEvents.emit(
                if (existingId == 0L) "Transaksi berhasil dicatat." else "Transaksi berhasil diperbarui."
            )
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            _snackbarEvents.emit("Transaksi dihapus dan saldo disesuaikan.")
        }
    }

    fun saveWallet(
        existingId: Long = 0L,
        name: String,
        type: WalletType,
        balance: Long,
        accentKey: String
    ) {
        viewModelScope.launch {
            val iconKey = when (type) {
                WalletType.TUNAI -> "wallet"
                WalletType.BANK -> "bank"
                WalletType.EWALLET -> "ewallet"
                WalletType.TABUNGAN -> "savings"
            }
            if (existingId == 0L) {
                repository.addWallet(
                    WalletEntity(
                        name = name.trim(),
                        type = type,
                        balance = balance,
                        accentKey = accentKey,
                        iconKey = iconKey
                    )
                )
                _snackbarEvents.emit("Dompet \"$name\" ditambahkan.")
            } else {
                val current = dao.getWalletById(existingId)
                repository.updateWallet(
                    WalletEntity(
                        id = existingId,
                        name = name.trim(),
                        type = type,
                        balance = balance,
                        accentKey = accentKey,
                        iconKey = iconKey,
                        createdAt = current?.createdAt ?: System.currentTimeMillis()
                    )
                )
                _snackbarEvents.emit("Dompet \"$name\" diperbarui.")
            }
        }
    }

    fun deleteWallet(walletId: Long) {
        viewModelScope.launch {
            val deleted = repository.deleteWallet(walletId)
            if (deleted) {
                if (_selectedWalletFilterId.value == walletId) {
                    _selectedWalletFilterId.value = null
                }
                _snackbarEvents.emit("Dompet berhasil dihapus.")
            } else {
                _snackbarEvents.emit("Dompet masih memiliki riwayat transaksi.")
            }
        }
    }

    fun saveCategory(
        existingId: Long = 0L,
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            repository.saveCategory(
                CategoryEntity(
                    id = existingId,
                    name = name,
                    type = type,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    isDefault = false
                )
            ).onSuccess { _snackbarEvents.emit(it) }
                .onFailure { _snackbarEvents.emit(it.message ?: "Gagal menyimpan kategori.") }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            repository.deleteCategoryReassigningToFallback(categoryId)
                .onSuccess { _snackbarEvents.emit(it) }
                .onFailure { _snackbarEvents.emit(it.message ?: "Gagal menghapus kategori.") }
        }
    }

    fun resetToSeedData() {
        viewModelScope.launch {
            repository.resetToSeedData()
            _selectedWalletFilterId.value = null
            _snackbarEvents.emit("Data contoh Fintek berhasil dimuat ulang.")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllDataKeepCategories()
            _selectedWalletFilterId.value = null
            _snackbarEvents.emit("Semua dompet dan transaksi telah dihapus.")
        }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportJson(uri)
                .onSuccess { _snackbarEvents.emit(it) }
                .onFailure { _snackbarEvents.emit("Gagal ekspor JSON: ${it.message}") }
        }
    }

    fun importJson(uri: Uri) {
        viewModelScope.launch {
            backupManager.importJson(uri)
                .onSuccess { _snackbarEvents.emit(it) }
                .onFailure { _snackbarEvents.emit("Gagal impor JSON: ${it.message}") }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportCsv(uri)
                .onSuccess { _snackbarEvents.emit(it) }
                .onFailure { _snackbarEvents.emit("Gagal ekspor CSV: ${it.message}") }
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            backupManager.importCsv(uri)
                .onSuccess { _snackbarEvents.emit(it) }
                .onFailure { _snackbarEvents.emit("Gagal impor CSV: ${it.message}") }
        }
    }

    private data class FilterParams(
        val query: String,
        val typeFilter: TransactionFilterType,
        val walletFilterId: Long?,
        val period: AnalyticsPeriod,
        val initialized: Boolean
    )
}
