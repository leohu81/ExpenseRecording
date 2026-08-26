package com.leohu.expense.ui.feature.approval

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.domain.model.Tag
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalDetailScreen(
    viewModel: ApprovalDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTagDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // 日期選擇器狀態
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(uiState.isApproved) {
        if (uiState.isApproved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showTagDialog) {
        SelectTagDialog(
            tags = uiState.tags,
            selectedTagIds = uiState.record?.tags ?: emptyList(),
            onDismiss = { showTagDialog = false },
            onSelect = { tagId -> viewModel.addTagToRecord(tagId) },
            onDeselect = { tagId -> viewModel.removeTagFromRecord(tagId) }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("刪除記錄") },
            text = { Text("確定要刪除這筆消費記錄嗎？此動作無法復原。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRecord() }) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(millis))
                        uiState.record?.let { viewModel.updateRecord(it.copy(consumeDate = date)) }
                    }
                    showDatePicker = false
                }) { Text("確定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.record?.status == PaymentStatus.APPROVED) "編輯消費記錄" else "核准消費記錄") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "刪除記錄", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val record = uiState.record
        if (uiState.isLoading || record == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.image?.let { img ->
                    AsyncImage(
                        model = File(img.localPath),
                        contentDescription = "原始截圖",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Tag 區域
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("消費標籤", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { showTagDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("編輯標籤")
                        }
                    }
                    
                    if (record.tags.isEmpty()) {
                        Text("尚無標籤", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            record.tags.forEach { tagId ->
                                uiState.tagMap[tagId]?.let { tag ->
                                    val tagColor = try {
                                        Color(android.graphics.Color.parseColor(tag.color))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                                    SuggestionChip(
                                        onClick = { showTagDialog = true },
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

                HorizontalDivider()

                TextField(
                    value = record.description ?: "",
                    onValueChange = { viewModel.updateRecord(record.copy(description = it)) },
                    label = { Text("說明/商家") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = record.amount.toString(),
                        onValueChange = { 
                            val newVal = it.toDoubleOrNull() ?: 0.0
                            viewModel.updateRecord(record.copy(amount = newVal)) 
                        },
                        label = { Text("金額") },
                        modifier = Modifier.weight(1f)
                    )
                    TextField(
                        value = record.currency ?: "TWD",
                        onValueChange = { viewModel.updateRecord(record.copy(currency = it)) },
                        label = { Text("幣別") },
                        modifier = Modifier.weight(0.5f)
                    )
                }

                if (record.currency?.uppercase() != "TWD") {
                    TextField(
                        value = record.amountTwd?.toString() ?: "",
                        onValueChange = { 
                            val newVal = it.toDoubleOrNull()
                            viewModel.updateRecord(record.copy(amountTwd = newVal))
                        },
                        label = { Text("約當台幣") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 日期選擇欄位
                TextField(
                    value = record.consumeDate ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("日期") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "選擇日期")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )

                // 支付方式
                val methodOptions = remember(uiState.ewallets) {
                    listOf("一般刷卡", "現金", "未知") + uiState.ewallets.map { it.name }
                }
                var methodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = !methodExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = record.method,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("支付方式") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        methodOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.updateRecord(record.copy(method = option))
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                // 帳戶/信用卡
                var cardExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = cardExpanded,
                    onExpandedChange = { cardExpanded = !cardExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = record.account ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("帳戶/信用卡") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = cardExpanded,
                        onDismissRequest = { cardExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("(無)") },
                            onClick = {
                                viewModel.updateRecord(record.copy(account = null, cardLast4 = null))
                                cardExpanded = false
                            }
                        )
                        uiState.cards.forEach { card ->
                            DropdownMenuItem(
                                text = { Text("${card.name}${card.last4?.let { " ($it)" } ?: ""}") },
                                onClick = {
                                    viewModel.updateRecord(record.copy(account = card.name, cardLast4 = card.last4))
                                    cardExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.approve() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (record.status == PaymentStatus.APPROVED) "儲存修改" else "核准並送出")
                }
            }
        }
    }
}

@Composable
private fun SelectTagDialog(
    tags: List<Tag>,
    selectedTagIds: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onDeselect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇標籤") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tags.isEmpty()) {
                    Text("請先到設定中建立標籤", style = MaterialTheme.typography.bodySmall)
                } else {
                    tags.forEach { tag ->
                        val isSelected = selectedTagIds.contains(tag.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (isSelected) onDeselect(tag.id) else onSelect(tag.id) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val tagColor = try {
                                    Color(android.graphics.Color.parseColor(tag.color))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(tagColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(tag.name)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { if (isSelected) onDeselect(tag.id) else onSelect(tag.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}
