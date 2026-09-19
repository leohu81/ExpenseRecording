package com.leohu.expense.ui.feature.approval

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.leohu.expense.domain.model.CreditCard
import com.leohu.expense.domain.model.EWalletAccount
import com.leohu.expense.domain.model.PaymentRecord
import com.leohu.expense.domain.model.Tag
import java.io.File

@Composable
fun ReceiptImage(localPath: String) {
    AsyncImage(
        model = File(localPath),
        contentDescription = "原始截圖",
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentScale = ContentScale.Fit
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSection(
    record: PaymentRecord,
    tagMap: Map<String, Tag>,
    onEditTags: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("消費標籤", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onEditTags) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("編輯標籤")
            }
        }
        
        if (record.tags.isEmpty()) {
            Text("尚無標籤", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                record.tags.forEach { tagId ->
                    tagMap[tagId]?.let { tag ->
                        val tagColor = try {
                            Color(android.graphics.Color.parseColor(tag.color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                        SuggestionChip(
                            onClick = onEditTags,
                            label = { Text(tag.name) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = tagColor.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AmountRow(
    amount: Double,
    currency: String,
    onAmountChange: (Double) -> Unit,
    onCurrencyChange: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(
            value = amount.toString(),
            onValueChange = { 
                val newVal = it.toDoubleOrNull() ?: 0.0
                onAmountChange(newVal) 
            },
            label = { Text("金額") },
            modifier = Modifier.weight(1f)
        )
        TextField(
            value = currency,
            onValueChange = onCurrencyChange,
            label = { Text("幣別") },
            modifier = Modifier.weight(0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodDropdown(
    selectedMethod: String,
    ewallets: List<EWalletAccount>,
    onMethodSelected: (String) -> Unit
) {
    val methodOptions = remember(ewallets) {
        listOf("一般刷卡", "現金", "未知") + ewallets.map { it.name }
    }
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedMethod,
            onValueChange = {},
            readOnly = true,
            label = { Text("支付方式") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            methodOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onMethodSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDropdown(
    selectedAccount: String?,
    cards: List<CreditCard>,
    onCardSelected: (CreditCard?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedAccount ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("帳戶/信用卡") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("(無)") },
                onClick = {
                    onCardSelected(null)
                    expanded = false
                }
            )
            cards.forEach { card ->
                DropdownMenuItem(
                    text = { Text("${card.name}${card.last4?.let { " ($it)" } ?: ""}") },
                    onClick = {
                        onCardSelected(card)
                        expanded = false
                    }
                )
            }
        }
    }
}
