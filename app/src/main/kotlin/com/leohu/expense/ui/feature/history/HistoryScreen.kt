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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.leohu.expense.domain.model.PaymentRecord
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
                items(historyItems, key = { it.id }) { item ->
                    when (item) {
                        is HistoryUiItem.Record -> {
                            RecordItemCard(
                                record = item.record,
                                imagePath = item.sourceImage?.localPath,
                                onClick = { onNavigateToDetail(item.record.sourceImageId, item.record.id) }
                            )
                        }
                        is HistoryUiItem.ImageOnly -> {
                            ImageOnlyItemCard(
                                status = item.sourceImage.status,
                                lastError = item.sourceImage.lastError,
                                imagePath = item.sourceImage.localPath,
                                onClick = { onNavigateToDetail(item.sourceImage.id, null) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordItemCard(
    record: PaymentRecord,
    imagePath: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "解析完成",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatAmount(record.amount, record.currency, record.amountTwd)} - ${record.description ?: "無說明"}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    SuggestionChip(onClick = {}, label = { Text(record.method) })
                    if (!record.account.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        SuggestionChip(onClick = {}, label = { Text(record.account!!) })
                    }
                }
            }

            if (!imagePath.isNullOrBlank()) {
                AsyncImage(
                    model = File(imagePath),
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

@Composable
private fun ImageOnlyItemCard(
    status: SourceImageStatus,
    lastError: String?,
    imagePath: String,
    onClick: () -> Unit
) {
    val cardColor = when (status) {
        SourceImageStatus.PENDING_OCR -> MaterialTheme.colorScheme.primaryContainer
        SourceImageStatus.PROCESSING -> MaterialTheme.colorScheme.tertiaryContainer
        SourceImageStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val statusText = when (status) {
        SourceImageStatus.PENDING_OCR -> "等待解析"
        SourceImageStatus.PROCESSING -> "解析中..."
        SourceImageStatus.FAILED -> "解析失敗"
        else -> "處理中"
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
                if (status == SourceImageStatus.FAILED) {
                    Text(
                        text = "錯誤: ${lastError ?: "未知原因"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "點擊以管理或重試",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun formatAmount(amount: Double, currency: String?, amountTwd: Double?): String {
    val cur = currency ?: "TWD"
    return if (cur.uppercase() == "TWD") {
        "$amount"
    } else {
        val twdPart = if (amountTwd != null) " (≈ $amountTwd TWD)" else ""
        "$amount $cur$twdPart"
    }
}
