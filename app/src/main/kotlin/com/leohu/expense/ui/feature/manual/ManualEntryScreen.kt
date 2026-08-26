package com.leohu.expense.ui.feature.manual

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.leohu.expense.domain.model.Tag
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    viewModel: ManualEntryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTagDialog by remember { mutableStateOf(false) }
    
    // 日期選擇器狀態
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
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
            tags = uiState.allTags,
            selectedTagIds = uiState.selectedTags,
            onDismiss = { showTagDialog = false },
            onToggle = { tagId -> viewModel.toggleTag(tagId) }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(millis))
                        viewModel.onDateChange(date)
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
                title = { Text("手動新增消費") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                
                if (uiState.selectedTags.isEmpty()) {
                    Text("尚無標籤", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.selectedTags.forEach { tagId ->
                            uiState.allTags.find { it.id == tagId }?.let { tag ->
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
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChange(it) },
                label = { Text("說明/商家") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.onAmountChange(it) },
                    label = { Text("金額") },
                    modifier = Modifier.weight(1f)
                )
                TextField(
                    value = uiState.currency,
                    onValueChange = { viewModel.onCurrencyChange(it) },
                    label = { Text("幣別") },
                    modifier = Modifier.weight(0.5f)
                )
            }
            
            if (uiState.currency.uppercase() != "TWD") {
                TextField(
                    value = uiState.amountTwd,
                    onValueChange = { viewModel.onAmountTwdChange(it) },
                    label = { Text("約當台幣 (TWD Equivalent)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 日期選擇欄位
            TextField(
                value = uiState.consumeDate,
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
                    value = uiState.method,
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
                                viewModel.onMethodChange(option)
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
                    value = uiState.account ?: "",
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
                            viewModel.onAccountChange(null)
                            cardExpanded = false
                        }
                    )
                    uiState.cards.forEach { card ->
                        DropdownMenuItem(
                            text = { Text("${card.name}${card.last4?.let { " ($it)" } ?: ""}") },
                            onClick = {
                                viewModel.onAccountChange(card)
                                cardExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("直接儲存")
            }
        }
    }
}

@Composable
private fun SelectTagDialog(
    tags: List<Tag>,
    selectedTagIds: List<String>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit
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
                                .clickable { onToggle(tag.id) }
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
                                onCheckedChange = { onToggle(tag.id) }
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
