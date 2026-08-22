package com.leohu.expense.ui.feature.ewallets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leohu.expense.domain.model.EWalletAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EWalletListScreen(
    viewModel: EWalletViewModel,
    onNavigateBack: () -> Unit
) {
    val ewallets by viewModel.ewallets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<EWalletAccount?>(null) }
    
    var newName by remember { mutableStateOf("") }
    var newKeywords by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("電子支付維護") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                newName = ""
                newKeywords = ""
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(ewallets) { account ->
                ListItem(
                    headlineContent = { Text(account.name) },
                    supportingContent = { Text("關鍵字: ${account.keywords.joinToString(", ")}") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = {
                                editingAccount = account
                                newName = account.name
                                newKeywords = account.keywords.joinToString(", ")
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "編輯")
                            }
                            IconButton(onClick = { viewModel.deleteEWallet(account) }) {
                                Icon(Icons.Default.Delete, contentDescription = "刪除")
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        editingAccount = account
                        newName = account.name
                        newKeywords = account.keywords.joinToString(", ")
                    }
                )
                HorizontalDivider()
            }
        }

        if (showAddDialog || editingAccount != null) {
            AlertDialog(
                onDismissRequest = { 
                    showAddDialog = false
                    editingAccount = null
                },
                title = { Text(if (editingAccount != null) "編輯電子支付" else "新增電子支付") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(value = newName, onValueChange = { newName = it }, label = { Text("名稱") })
                        TextField(value = newKeywords, onValueChange = { newKeywords = it }, label = { Text("關鍵字 (逗號分隔)") })
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (editingAccount != null) {
                            viewModel.updateEWallet(editingAccount!!.copy(name = newName), newKeywords)
                        } else {
                            viewModel.addEWallet(newName, newKeywords)
                        }
                        showAddDialog = false
                        editingAccount = null
                    }) {
                        Text(if (editingAccount != null) "儲存" else "新增")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddDialog = false
                        editingAccount = null
                    }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
