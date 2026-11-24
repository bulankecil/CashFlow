package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionDetail
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.ui.theme.LocalFintekColors
import com.example.ui.theme.MoneyDisplayStyle
import com.example.ui.theme.MoneySmallStyle
import com.example.util.accentColorForKey
import com.example.util.formatDateIndo
import com.example.util.formatRupiah
import com.example.util.iconForKey
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionBottomSheet(
    wallets: List<WalletEntity>,
    categories: List<CategoryEntity>,
    editingDetail: TransactionDetail? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long,
        type: TransactionType,
        amount: Long,
        walletId: Long,
        targetWalletId: Long?,
        categoryId: Long,
        title: String,
        note: String,
        timestamp: Long
    ) -> Unit,
    onDelete: (Long) -> Unit,
    onRequestAddWallet: () -> Unit,
    onRequestManageCategories: (TransactionType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fintekColors = LocalFintekColors.current
    val existing = editingDetail?.transaction

    var selectedType by remember(existing) {
        mutableStateOf(existing?.type ?: initialType)
    }
    var amountDigits by remember(existing) {
        mutableStateOf(existing?.amount?.toString() ?: "")
    }
    var title by remember(existing) {
        mutableStateOf(existing?.title ?: "")
    }
    var note by remember(existing) {
        mutableStateOf(existing?.note ?: "")
    }
    var selectedWalletId by remember(existing, wallets) {
        mutableLongStateOf(existing?.walletId ?: wallets.firstOrNull()?.id ?: 0L)
    }
    var selectedTargetWalletId by remember(existing, wallets) {
        mutableLongStateOf(
            existing?.targetWalletId
                ?: wallets.firstOrNull { it.id != (existing?.walletId ?: wallets.firstOrNull()?.id) }?.id
                ?: wallets.firstOrNull()?.id
                ?: 0L
        )
    }

    val categoriesForType = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }
    var selectedCategoryId by remember(existing, selectedType) {
        mutableLongStateOf(
            existing?.takeIf { it.type == selectedType }?.categoryId
                ?: categoriesForType.firstOrNull()?.id
                ?: 0L
        )
    }
    LaunchedEffect(categoriesForType) {
        if (categoriesForType.isNotEmpty() && categoriesForType.none { it.id == selectedCategoryId }) {
            selectedCategoryId = categoriesForType.first().id
        }
    }
    var selectedTimestamp by remember(existing) {
        mutableLongStateOf(existing?.timestamp ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountDigits.toLongOrNull() ?: 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) "Catat Transaksi" else "Ubah Transaksi",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup"
                    )
                }
            }

            if (wallets.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                EmptyFinanceState(
                    title = "Belum Ada Dompet",
                    subtitle = "Tambahkan minimal satu dompet terlebih dahulu untuk mulai mencatat transaksi.",
                    actionLabel = "Tambah Dompet",
                    onActionClick = {
                        onDismiss()
                        onRequestAddWallet()
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                return@Column
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Type segmented selector
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = fintekColors.subtleSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        TransactionType.EXPENSE to "Pengeluaran",
                        TransactionType.INCOME to "Pemasukan",
                        TransactionType.TRANSFER to "Transfer"
                    ).forEach { (type, label) ->
                        val active = selectedType == type
                        val activeBg = when (type) {
                            TransactionType.EXPENSE -> fintekColors.expense
                            TransactionType.INCOME -> fintekColors.income
                            TransactionType.TRANSFER -> fintekColors.transfer
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) activeBg else Color.Transparent)
                                .clickable {
                                    selectedType = type
                                    errorMessage = null
                                }
                                .testTag("tx_type_${type.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount input
            OutlinedTextField(
                value = amountDigits,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() }.take(13)
                    amountDigits = filtered
                    errorMessage = null
                },
                label = { Text("Nominal (Rupiah)") },
                prefix = { Text("Rp ", style = MaterialTheme.typography.titleMedium) },
                supportingText = {
                    if (parsedAmount > 0L) {
                        Text(
                            text = formatRupiah(parsedAmount),
                            style = MoneySmallStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text("Masukkan nominal dalam angka bulat")
                    }
                },
                textStyle = MoneyDisplayStyle.copy(fontSize = 22.sp, lineHeight = 28.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick amount chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    25_000L to "+25 rb",
                    50_000L to "+50 rb",
                    100_000L to "+100 rb",
                    500_000L to "+500 rb",
                    1_000_000L to "+1 jt"
                ).forEach { (delta, label) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = fintekColors.subtleSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val next = (parsedAmount + delta).coerceAtMost(9_999_999_999_999L)
                                amountDigits = next.toString()
                                errorMessage = null
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Wallet selector
            Text(
                text = if (selectedType == TransactionType.TRANSFER) "Dari Dompet" else "Pilih Dompet",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wallets.forEach { wallet ->
                    val selected = wallet.id == selectedWalletId
                    val accent = accentColorForKey(wallet.accentKey)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) accent.copy(alpha = 0.14f) else fintekColors.subtleSurface,
                        border = if (selected) BorderStroke(1.5.dp, accent) else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedWalletId = wallet.id
                                errorMessage = null
                            }
                            .testTag("tx_wallet_chip_${wallet.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = iconForKey(wallet.iconKey),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Target Wallet for Transfer
            if (selectedType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Ke Dompet Tujuan",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wallets.forEach { wallet ->
                        val selected = wallet.id == selectedTargetWalletId
                        val accent = accentColorForKey(wallet.accentKey)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) accent.copy(alpha = 0.14f) else fintekColors.subtleSurface,
                            border = if (selected) BorderStroke(1.5.dp, accent) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedTargetWalletId = wallet.id
                                    errorMessage = null
                                }
                                .testTag("tx_target_wallet_chip_${wallet.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = iconForKey(wallet.iconKey),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = wallet.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                // Category selector
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kategori",
                        style = MaterialTheme.typography.labelLarge
                    )
                    TextButton(
                        onClick = { onRequestManageCategories(selectedType) },
                        modifier = Modifier.testTag("tx_manage_categories_button")
                    ) {
                        Text("Kelola")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoriesForType.forEach { category ->
                        val selected = category.id == selectedCategoryId
                        val catColor = Color(category.colorHex)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) catColor.copy(alpha = 0.16f) else fintekColors.subtleSurface,
                            border = if (selected) BorderStroke(1.5.dp, catColor) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCategoryId = category.id }
                                .testTag("tx_category_chip_${category.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = iconForKey(category.iconKey),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Note
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul Transaksi") },
                placeholder = {
                    Text(
                        when (selectedType) {
                            TransactionType.EXPENSE -> "Mis. Makan Siang, Bensin"
                            TransactionType.INCOME -> "Mis. Gaji, Bonus Proyek"
                            TransactionType.TRANSFER -> "Mis. Top Up E-Wallet"
                        }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (Opsional)") },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = false,
                    onClick = { selectedTimestamp = System.currentTimeMillis() },
                    label = { Text("Hari Ini") }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        selectedTimestamp = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -1)
                        }.timeInMillis
                    },
                    label = { Text("Kemarin") }
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatDateIndo(selectedTimestamp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (existing != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(existing.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("tx_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Hapus transaksi"
                        )
                    }
                }

                Button(
                    onClick = {
                        when {
                            parsedAmount <= 0L -> {
                                errorMessage = "Masukkan nominal lebih dari Rp 0."
                            }
                            selectedWalletId == 0L -> {
                                errorMessage = "Pilih dompet terlebih dahulu."
                            }
                            selectedType == TransactionType.TRANSFER && wallets.size < 2 -> {
                                errorMessage = "Transfer membutuhkan minimal 2 dompet."
                            }
                            selectedType == TransactionType.TRANSFER && selectedWalletId == selectedTargetWalletId -> {
                                errorMessage = "Dompet asal dan tujuan tidak boleh sama."
                            }
                            else -> {
                                val resolvedCategoryId = if (selectedType == TransactionType.TRANSFER) {
                                    0L
                                } else {
                                    selectedCategoryId.takeIf { id ->
                                        categoriesForType.any { it.id == id }
                                    } ?: categoriesForType.firstOrNull()?.id ?: 0L
                                }
                                onSave(
                                    existing?.id ?: 0L,
                                    selectedType,
                                    parsedAmount,
                                    selectedWalletId,
                                    if (selectedType == TransactionType.TRANSFER) selectedTargetWalletId else null,
                                    resolvedCategoryId,
                                    title,
                                    note,
                                    selectedTimestamp
                                )
                                onDismiss()
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("tx_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existing == null) "Simpan Transaksi" else "Simpan Perubahan",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pickedUtc ->
                            val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = pickedUtc
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            selectedTimestamp = localCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
