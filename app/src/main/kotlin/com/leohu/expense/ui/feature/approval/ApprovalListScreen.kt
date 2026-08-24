package com.leohu.expense.ui.feature.approval

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leohu.expense.domain.model.PaymentRecord

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ApprovalListScreen(
    viewModel: ApprovalViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val items by viewModel.pendingApprovals.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (selectedIds.isNotEmpty()) {
                        Text("已選擇 ${selectedIds.size} 筆")
                    } else {
                        Text("待核准列表")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (selectedIds.isNotEmpty()) viewModel::clearSelection else onNavigateBack) {
                        Icon(
                            if (selectedIds.isNotEmpty()) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { viewModel.batchApprove() }) {
                            Icon(Icons.Default.Check, contentDescription = "批次核准")
                        }
                        IconButton(onClick = { viewModel.batchDelete() }) {
                            Icon(Icons.Default.Delete, contentDescription = "批次刪除")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(items) { record ->
                val isSelected = selectedIds.contains(record.id)
                ListItem(
                    headlineContent = { 
                        Text("${formatAmountWithCurrency(record.amount, record.currency)}") 
                    },
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
                    trailingContent = { 
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(record.consumeDate ?: "", style = MaterialTheme.typography.bodySmall)
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleSelection(record.id) }
                            )
                        }
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = { 
                            if (selectedIds.isNotEmpty()) {
                                viewModel.toggleSelection(record.id)
                            } else {
                                onNavigateToDetail(record.id)
                            }
                        },
                        onLongClick = { viewModel.toggleSelection(record.id) }
                    )
                )
                HorizontalDivider()
            }
        }
    }
}

private fun formatAmountWithCurrency(amount: Double, currency: String?): String {
    val cur = currency ?: "TWD"
    return when (cur.uppercase()) {
        "TWD", "" -> "$amount"
        else -> "$amount $cur"
    }
}
