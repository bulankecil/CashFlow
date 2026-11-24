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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.WalletEntity
import com.example.data.local.WalletType
import com.example.ui.theme.LocalFintekColors
import com.example.ui.theme.MoneySmallStyle
import com.example.util.AvailableWalletAccents
import com.example.util.accentColorForKey
import com.example.util.formatRupiah
import com.example.util.walletTypeLabel

@Composable
fun WalletEditorDialog(
    editingWallet: WalletEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long,
        name: String,
        type: WalletType,
        balance: Long,
        accentKey: String
    ) -> Unit
) {
    var name by remember(editingWallet) { mutableStateOf(editingWallet?.name ?: "") }
    var selectedType by remember(editingWallet) { mutableStateOf(editingWallet?.type ?: WalletType.BANK) }
    var balanceDigits by remember(editingWallet) {
        mutableStateOf(editingWallet?.balance?.toString() ?: "0")
    }
    var selectedAccent by remember(editingWallet) {
        mutableStateOf(editingWallet?.accentKey ?: "emerald")
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    val parsedBalance = balanceDigits.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingWallet == null) "Tambah Dompet Baru" else "Ubah Dompet",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("Nama Dompet / Akun") },
                    placeholder = { Text("Mis. BCA, OVO, Dompet Tunai") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Tipe Akun", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WalletType.entries.forEach { type ->
                        val active = selectedType == type
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                LocalFintekColors.current.subtleSurface
                            },
                            border = if (active) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedType = type }
                        ) {
                            Text(
                                text = walletTypeLabel(type),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = balanceDigits,
                    onValueChange = { raw ->
                        balanceDigits = raw.filter { it.isDigit() }.take(13)
                    },
                    label = { Text(if (editingWallet == null) "Saldo Awal (IDR)" else "Saldo Saat Ini (IDR)") },
                    prefix = { Text("Rp ") },
                    supportingText = {
                        Text(
                            text = formatRupiah(parsedBalance),
                            style = MoneySmallStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_balance_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Warna Identitas", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AvailableWalletAccents.forEach { (key, label) ->
                        val color = accentColorForKey(key)
                        val isSelected = selectedAccent == key
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedAccent = key }
                                .testTag("wallet_accent_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = label,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorText = "Nama dompet wajib diisi."
                    } else {
                        onSave(
                            editingWallet?.id ?: 0L,
                            name.trim(),
                            selectedType,
                            parsedBalance,
                            selectedAccent
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("wallet_save_button")
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
