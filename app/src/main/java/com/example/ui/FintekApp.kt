package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.ThemeMode
import com.example.data.local.TransactionDetail
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.ui.components.CategoryManagerSheet
import com.example.ui.components.TransactionBottomSheet
import com.example.ui.components.WalletEditorDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.WalletsScreen
import com.example.ui.theme.LocalFintekColors
import com.example.ui.viewmodel.FintekTab
import com.example.ui.viewmodel.FintekViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FintekApp(
    viewModel: FintekViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = selectedTab != FintekTab.DASHBOARD) {
        viewModel.selectTab(FintekTab.DASHBOARD)
    }

    // Modal states for transaction, wallet, and category CRUD
    var showTransactionSheet by remember { mutableStateOf(false) }
    var sheetInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<TransactionDetail?>(null) }

    var showWalletDialog by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<WalletEntity?>(null) }

    var showCategoryManagerSheet by remember { mutableStateOf(false) }
    var categoryInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }

    // SAF launchers (zero storage permission)
    val dateStamp = remember {
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportJson(it) }
    }
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importJson(it) }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportCsv(it) }
    }
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importCsv(it) }
    }

    val openNewTx = { type: TransactionType ->
        editingTransaction = null
        sheetInitialType = type
        showTransactionSheet = true
    }

    val openEditTx = { detail: TransactionDetail ->
        editingTransaction = detail
        sheetInitialType = detail.transaction.type
        showTransactionSheet = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = if (useNavigationRail) 16.dp else 92.dp)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            header = {
                                Spacer(modifier = Modifier.height(8.dp))
                                FloatingActionButton(
                                    onClick = { openNewTx(TransactionType.EXPENSE) },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("rail_add_transaction")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = stringResource(R.string.cd_add_transaction)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .navigationBarsPadding()
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            FintekTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 840.dp)
                        ) {
                            when (selectedTab) {
                                FintekTab.DASHBOARD -> DashboardScreen(
                                    uiState = uiState,
                                    isDarkTheme = isDarkTheme,
                                    onToggleTheme = { viewModel.toggleLightDark(isDarkTheme) },
                                    onOpenNewTransaction = openNewTx,
                                    onEditTransaction = openEditTx,
                                    onOpenAddWallet = {
                                        editingWallet = null
                                        showWalletDialog = true
                                    },
                                    onOpenWalletTab = { viewModel.selectTab(FintekTab.WALLETS) },
                                    onOpenTransactionsTab = { viewModel.selectTab(FintekTab.TRANSACTIONS) },
                                    onOpenAnalyticsTab = { viewModel.selectTab(FintekTab.ANALYTICS) }
                                )

                                FintekTab.TRANSACTIONS -> TransactionsScreen(
                                    uiState = uiState,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onFilterTypeChange = viewModel::setFilterType,
                                    onWalletFilterChange = viewModel::setWalletFilter,
                                    onEditTransaction = openEditTx,
                                    onAddTransactionClick = { openNewTx(TransactionType.EXPENSE) }
                                )

                                FintekTab.ANALYTICS -> AnalyticsScreen(
                                    analytics = uiState.analytics,
                                    onSelectPeriod = viewModel::setAnalyticsPeriod
                                )

                                FintekTab.WALLETS -> WalletsScreen(
                                    uiState = uiState,
                                    onAddWalletClick = {
                                        editingWallet = null
                                        showWalletDialog = true
                                    },
                                    onEditWalletClick = { wallet ->
                                        editingWallet = wallet
                                        showWalletDialog = true
                                    },
                                    onDeleteWalletClick = viewModel::deleteWallet
                                )

                                FintekTab.SETTINGS -> {
                                    val expenseCount = uiState.categories.count { it.type == TransactionType.EXPENSE }
                                    val incomeCount = uiState.categories.count { it.type == TransactionType.INCOME }
                                    SettingsScreen(
                                        currentThemeMode = themeMode,
                                        walletCount = uiState.wallets.size,
                                        transactionCount = uiState.allTransactions.size,
                                        expenseCategoryCount = expenseCount,
                                        incomeCategoryCount = incomeCount,
                                        onSelectThemeMode = viewModel::setThemeMode,
                                        onManageCategoriesClick = {
                                            categoryInitialType = TransactionType.EXPENSE
                                            showCategoryManagerSheet = true
                                        },
                                        onExportJsonClick = {
                                            exportJsonLauncher.launch("fintek_cadangan_$dateStamp.json")
                                        },
                                        onImportJsonClick = {
                                            importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                        },
                                        onExportCsvClick = {
                                            exportCsvLauncher.launch("fintek_transaksi_$dateStamp.csv")
                                        },
                                        onImportCsvClick = {
                                            importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                                        },
                                        onResetSeedData = viewModel::resetToSeedData,
                                        onClearAllData = viewModel::clearAllData
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating + Catat FAB above floating pill bar (compact view)
                if (!useNavigationRail && selectedTab != FintekTab.SETTINGS) {
                    ExtendedFloatingActionButton(
                        onClick = { openNewTx(TransactionType.EXPENSE) },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.cd_add_transaction)
                            )
                        },
                        text = { Text(stringResource(R.string.action_record_transaction)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 16.dp, bottom = 94.dp)
                            .testTag("fab_add_transaction")
                    )
                }

                // Floating Pill Footer Navigation Bar (compact view)
                if (!useNavigationRail) {
                    FloatingBottomNavBar(
                        selectedTab = selectedTab,
                        onSelectTab = viewModel::selectTab,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    if (showTransactionSheet) {
        TransactionBottomSheet(
            wallets = uiState.wallets,
            categories = uiState.categories,
            editingDetail = editingTransaction,
            initialType = sheetInitialType,
            onDismiss = { showTransactionSheet = false },
            onSave = viewModel::saveTransaction,
            onDelete = viewModel::deleteTransaction,
            onRequestAddWallet = {
                editingWallet = null
                showWalletDialog = true
            },
            onRequestManageCategories = { currentType ->
                categoryInitialType = if (currentType == TransactionType.INCOME) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }
                showCategoryManagerSheet = true
            }
        )
    }

    if (showCategoryManagerSheet) {
        CategoryManagerSheet(
            categories = uiState.categories,
            allTransactions = uiState.allTransactions,
            initialType = categoryInitialType,
            onDismiss = { showCategoryManagerSheet = false },
            onSaveCategory = viewModel::saveCategory,
            onDeleteCategory = viewModel::deleteCategory
        )
    }

    if (showWalletDialog) {
        WalletEditorDialog(
            editingWallet = editingWallet,
            onDismiss = { showWalletDialog = false },
            onSave = viewModel::saveWallet
        )
    }
}

@Composable
private fun FloatingBottomNavBar(
    selectedTab: FintekTab,
    onSelectTab: (FintekTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val fintekColors = LocalFintekColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .testTag("floating_bottom_nav_bar"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 10.dp,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, fintekColors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FintekTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val shortLabel = if (tab == FintekTab.SETTINGS) "Setelan" else tab.label
                val contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tabIcon(tab, selected),
                        contentDescription = tab.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary else contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = shortLabel,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.primary else contentColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun tabIcon(tab: FintekTab, selected: Boolean): ImageVector = when (tab) {
    FintekTab.DASHBOARD -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    FintekTab.TRANSACTIONS -> if (selected) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong
    FintekTab.ANALYTICS -> if (selected) Icons.Rounded.BarChart else Icons.Outlined.BarChart
    FintekTab.WALLETS -> if (selected) Icons.Rounded.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
    FintekTab.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
}
