package com.leohu.expense.ui.feature.cards

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
import com.leohu.expense.domain.model.CreditCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    viewModel: CardViewModel,
    onNavigateBack: () -> Unit
) {
    val cards by viewModel.cards.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }
    
    var newCardName by remember { mutableStateOf("") }
    var newCardLast4 by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("信用卡維護") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                newCardName = ""
                newCardLast4 = ""
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
            items(cards) { card ->
                ListItem(
                    headlineContent = { Text(card.name) },
                    supportingContent = { Text("末四碼: ${card.last4 ?: "無"}") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = {
                                editingCard = card
                                newCardName = card.name
                                newCardLast4 = card.last4 ?: ""
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "編輯")
                            }
                            IconButton(onClick = { viewModel.deleteCard(card) }) {
                                Icon(Icons.Default.Delete, contentDescription = "刪除")
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        editingCard = card
                        newCardName = card.name
                        newCardLast4 = card.last4 ?: ""
                    }
                )
                HorizontalDivider()
            }
        }

        // Add/Edit Dialog
        if (showAddDialog || editingCard != null) {
            AlertDialog(
                onDismissRequest = { 
                    showAddDialog = false
                    editingCard = null
                },
                title = { Text(if (editingCard != null) "編輯信用卡" else "新增信用卡") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(value = newCardName, onValueChange = { newCardName = it }, label = { Text("卡片名稱") })
                        TextField(value = newCardLast4, onValueChange = { newCardLast4 = it }, label = { Text("末四碼") })
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (editingCard != null) {
                            viewModel.updateCard(editingCard!!.copy(name = newCardName, last4 = newCardLast4))
                        } else {
                            viewModel.addCard(newCardName, newCardLast4)
                        }
                        showAddDialog = false
                        editingCard = null
                    }) {
                        Text(if (editingCard != null) "儲存" else "新增")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddDialog = false
                        editingCard = null
                    }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
