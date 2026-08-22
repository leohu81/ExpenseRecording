package com.leohu.expense.ui.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val records by viewModel.approvedRecords.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("歷史記錄") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(records) { record ->
                ListItem(
                    headlineContent = { Text("${record.amount} ${record.currency ?: "TWD"}") },
                    supportingContent = { 
                        Column {
                            Text(record.description ?: "無說明")
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(record.method) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                if (!record.account.isNullOrBlank()) {
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(record.account + (record.cardLast4?.let { " ($it)" } ?: "")) }
                                    )
                                }
                            }
                        }
                    },
                    trailingContent = { Text(record.consumeDate ?: "", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.clickable { onNavigateToDetail(record.id) }
                )
                HorizontalDivider()
            }
        }
    }
}
