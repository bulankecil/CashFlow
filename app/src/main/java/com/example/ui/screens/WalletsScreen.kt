package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.WalletEntity
import com.example.ui.components.EmptyFinanceState
import com.example.ui.theme.LocalFintekColors
import com.example.ui.theme.MoneyDisplayStyle
import com.example.ui.theme.MoneyMediumStyle
import com.example.ui.viewmodel.FintekUiState
import com.example.util.accentColorForKey
import com.example.util.formatRupiah
import com.example.util.iconForKey
import com.example.util.walletTypeLabel

@Composable
fun WalletsScreen(
    uiState: FintekUiState,
    onAddWalletClick: () -> Unit,
    onEditWalletClick: (WalletEntity) -> Unit,
    onDeleteWalletClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val fintekColors = LocalFintekColors.current
    val totalBalance = uiState.dashboardSummary.totalBalance

    val txCountByWallet = remember(uiState.allTransactions) {
        val counts = mutableMapOf<Long, Int>()
        uiState.allTransactions.forEach { detail ->
            val tx = detail.transaction
            counts[tx.walletId] = (counts[tx.walletId] ?: 0) + 1
            tx.targetWalletId?.let { targetId ->
                counts[targetId] = (counts[targetId] ?: 0) + 1
            }
        }
        counts
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dompet & Akun",
                    style = MaterialTheme.typography.headlineMedium
                )
                Button(
                    onClick = onAddWalletClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("wallets_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah")
                }
            }
        }

        // Net Worth Summary Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(fintekColors.heroStart, fintekColors.heroEnd)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Total Saldo Seluruh Dompet (${uiState.wallets.size} Akun)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatRupiah(totalBalance),
                        style = MoneyDisplayStyle,
                        color = Color.White
                    )
                }
            }
        }

        if (uiState.wallets.isEmpty()) {
            item {
                EmptyFinanceState(
                    title = "Belum Ada Dompet",
                    subtitle = "Buat dompet pertama Anda seperti Rekening Bank, E-Wallet, atau Tunai.",
                    actionLabel = "Tambah Dompet Pertama",
                    onActionClick = onAddWalletClick
                )
            }
        } else {
            items(uiState.wallets, key = { it.id }) { wallet ->
                val accent = accentColorForKey(wallet.accentKey)
                val txCount = txCountByWallet[wallet.id] ?: 0

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, fintekColors.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_card_${wallet.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconForKey(wallet.iconKey),
                                contentDescription = wallet.name,
                                tint = accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${walletTypeLabel(wallet.type)} • $txCount transaksi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatRupiah(wallet.balance),
                                style = MoneyMediumStyle.copy(fontWeight = FontWeight.Bold),
                                color = accent
                            )
                        }

                        IconButton(
                            onClick = { onEditWalletClick(wallet) },
                            modifier = Modifier.testTag("wallet_edit_${wallet.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Ubah ${wallet.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { onDeleteWalletClick(wallet.id) },
                            modifier = Modifier.testTag("wallet_delete_${wallet.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Hapus ${wallet.name}",
                                tint = if (txCount == 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(132.dp)) }
    }
}
