package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionDetail
import com.example.data.local.TransactionType
import com.example.ui.theme.LocalFintekColors
import com.example.util.AvailableCategoryColors
import com.example.util.AvailableCategoryIcons
import com.example.util.iconForKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerSheet(
    categories: List<CategoryEntity>,
    allTransactions: List<TransactionDetail>,
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSaveCategory: (
        existingId: Long,
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: Long
    ) -> Unit,
    onDeleteCategory: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fintekColors = LocalFintekColors.current

    var selectedType by remember(initialType) {
        mutableStateOf(
            if (initialType == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
        )
    }
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var pendingDeleteCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    val txCountByCategory = remember(allTransactions) {
        allTransactions
            .filter { it.transaction.type != TransactionType.TRANSFER }
            .groupingBy { it.transaction.categoryId }
            .eachCount()
    }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kelola Kategori",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Tambah, ubah, atau hapus kategori transaksi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            editingCategory = null
                            showEditorDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("category_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Type segmented toggle (Pengeluaran / Pemasukan)
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
                        TransactionType.INCOME to "Pemasukan"
                    ).forEach { (type, label) ->
                        val active = selectedType == type
                        val activeColor = if (type == TransactionType.EXPENSE) {
                            fintekColors.expense
                        } else {
                            fintekColors.income
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) activeColor else Color.Transparent)
                                .clickable { selectedType = type }
                                .testTag("category_tab_${type.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredCategories, key = { it.id }) { category ->
                    val catColor = Color(category.colorHex)
                    val txCount = txCountByCategory[category.id] ?: 0
                    val isFallback = category.name.equals("Lainnya", ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = fintekColors.subtleSurface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, fintekColors.cardBorder.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_row_${category.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(catColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconForKey(category.iconKey),
                                    contentDescription = category.name,
                                    tint = catColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isFallback) {
                                        "Kategori penampung • $txCount transaksi"
                                    } else {
                                        "$txCount transaksi"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    editingCategory = category
                                    showEditorDialog = true
                                },
                                modifier = Modifier.testTag("category_edit_${category.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Ubah ${category.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isFallback) {
                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Kategori bawaan dilindungi",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { pendingDeleteCategory = category },
                                    modifier = Modifier.testTag("category_delete_${category.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Hapus ${category.name}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        CategoryEditorDialog(
            editingCategory = editingCategory,
            defaultType = selectedType,
            existingCategories = categories,
            onDismiss = { showEditorDialog = false },
            onSave = { id, name, type, iconKey, colorHex ->
                onSaveCategory(id, name, type, iconKey, colorHex)
                selectedType = type
            }
        )
    }

    pendingDeleteCategory?.let { target ->
        val count = txCountByCategory[target.id] ?: 0
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = null },
            title = { Text("Hapus Kategori \"${target.name}\"?") },
            text = {
                Text(
                    if (count > 0) {
                        "Terdapat $count transaksi pada kategori ini. Seluruh transaksi tersebut akan dipindahkan otomatis ke kategori \"Lainnya\"."
                    } else {
                        "Kategori ini belum memiliki transaksi dan akan dihapus permanen."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(target.id)
                        pendingDeleteCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("category_confirm_delete_button")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun CategoryEditorDialog(
    editingCategory: CategoryEntity?,
    defaultType: TransactionType,
    existingCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long,
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: Long
    ) -> Unit
) {
    val fintekColors = LocalFintekColors.current
    var name by remember(editingCategory) { mutableStateOf(editingCategory?.name ?: "") }
    var selectedType by remember(editingCategory, defaultType) {
        mutableStateOf(editingCategory?.type ?: defaultType)
    }
    var selectedIconKey by remember(editingCategory) {
        mutableStateOf(
            editingCategory?.iconKey ?: if (defaultType == TransactionType.INCOME) "salary" else "food"
        )
    }
    var selectedColorHex by remember(editingCategory) {
        mutableLongStateOf(
            editingCategory?.colorHex ?: AvailableCategoryColors.first()
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingCategory == null) "Tambah Kategori Baru" else "Ubah Kategori",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nama Kategori") },
                    placeholder = { Text("Mis. Kos, Sedekah, Gaji Sampingan") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                if (editingCategory == null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Jenis Kategori", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            TransactionType.EXPENSE to "Pengeluaran",
                            TransactionType.INCOME to "Pemasukan"
                        ).forEach { (type, label) ->
                            val active = selectedType == type
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    fintekColors.subtleSurface
                                },
                                border = if (active) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedType = type }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Pilih Ikon", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvailableCategoryIcons.forEach { (iconKey, label) ->
                        val active = selectedIconKey == iconKey
                        val activeColor = Color(selectedColorHex)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (active) activeColor.copy(alpha = 0.18f) else fintekColors.subtleSurface
                                )
                                .clickable { selectedIconKey = iconKey }
                                .testTag("category_icon_$iconKey"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconForKey(iconKey),
                                contentDescription = label,
                                tint = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Pilih Warna", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AvailableCategoryColors.forEach { colorHex ->
                        val active = selectedColorHex == colorHex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .clickable { selectedColorHex = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Warna terpilih",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    val duplicate = existingCategories.any {
                        it.id != (editingCategory?.id ?: 0L) &&
                            it.type == selectedType &&
                            it.name.trim().equals(trimmed, ignoreCase = true)
                    }
                    when {
                        trimmed.isEmpty() -> errorMessage = "Nama kategori wajib diisi."
                        duplicate -> errorMessage = "Nama kategori \"$trimmed\" sudah digunakan."
                        else -> {
                            onSave(
                                editingCategory?.id ?: 0L,
                                trimmed,
                                selectedType,
                                selectedIconKey,
                                selectedColorHex
                            )
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.testTag("category_save_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
