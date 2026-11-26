package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TransactionDetail
import com.example.data.local.TransactionType
import com.example.ui.theme.LocalFintekColors
import com.example.ui.theme.MoneyMediumStyle
import com.example.ui.theme.MoneySmallStyle
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
fun DailyLedgerCard(
    ledger: DailyLedgerGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditTransaction: (TransactionDetail) -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
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

                // Daily summary row: Masuk | Keluar
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

@Composable
fun EmptyFinanceState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LocalFintekColors.current.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_finance_1795542258482),
                contentDescription = stringResource(R.string.cd_empty_finance),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_state_action_button")
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}
