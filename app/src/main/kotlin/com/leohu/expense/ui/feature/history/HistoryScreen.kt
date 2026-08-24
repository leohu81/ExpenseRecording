package com.leohu.expense.ui.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.leohu.expense.domain.model.SourceImageStatus
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String, String?) -> Unit
) {
    val historyItems by viewModel.historyItems.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部記錄") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("目前沒有上傳過任何收據", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyItems, key = { it.sourceImage.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onClick = { 
                            val recordId = item.paymentRecords.firstOrNull()?.id
                            onNavigateToDetail(item.sourceImage.id, recordId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: ImageHistoryItem,
    onClick: () -> Unit
) {
    val status = item.sourceImage.status
    val cardColor = when (status) {
        SourceImageStatus.PENDING_OCR -> MaterialTheme.colorScheme.primaryContainer
        SourceImageStatus.PROCESSING -> MaterialTheme.colorScheme.tertiaryContainer
        SourceImageStatus.READY -> MaterialTheme.colorScheme.secondaryContainer
        SourceImageStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
    }
    
    val statusText = when (status) {
        SourceImageStatus.PENDING_OCR -> "等待解析"
        SourceImageStatus.PROCESSING -> "解析中..."
        SourceImageStatus.READY -> "解析完成"
        SourceImageStatus.FAILED -> "解析失敗"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (status == SourceImageStatus.FAILED) {
                    Text(
                        text = "錯誤: ${item.sourceImage.lastError ?: "未知原因"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (item.paymentRecords.isNotEmpty()) {
                    item.paymentRecords.forEach { record ->
                        Text(
                            text = "${formatAmountWithCurrency(record.amount, record.currency)} - ${record.description ?: "無說明"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            if (record.method.isNotEmpty()) {
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(record.method) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            if (!record.account.isNullOrBlank()) {
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(record.account + (record.cardLast4?.let { " ($it)" } ?: "")) }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "準備處理...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 若尚未完成解析或解析失敗，顯示圖片縮圖
            if (status != SourceImageStatus.READY || item.paymentRecords.isEmpty()) {
                AsyncImage(
                    model = File(item.sourceImage.localPath),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private fun formatAmountWithCurrency(amount: Double, currency: String?): String {
    val cur = currency ?: "TWD"
    return if (cur.uppercase() == "TWD") {
        "$amount"
    } else {
        "$amount $cur"
    }
}
