package com.leohu.expense.ui.feature.approval

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leohu.expense.app.ExpenseApplication
import com.leohu.expense.domain.model.PaymentStatus
import com.leohu.expense.util.PreferenceHelper
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
    val context = LocalContext.current
    val preferenceHelper = (context.applicationContext as ExpenseApplication).preferenceHelper
    
    var showTagDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSaveSyncDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(uiState.isApproved) {
        if (uiState.isApproved) onNavigateBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Dialogs
    if (showTagDialog) {
        SelectTagDialog(
            tags = uiState.tags,
            selectedTagIds = uiState.record?.tags ?: emptyList(),
            onDismiss = { showTagDialog = false },
            onSelect = viewModel::addTagToRecord,
            onDeselect = viewModel::removeTagFromRecord
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            showServerOption = preferenceHelper.getStorageMode() == PreferenceHelper.MODE_CLOUD,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { deleteFromServer ->
                viewModel.deleteRecord(deleteFromServer)
                showDeleteDialog = false
            }
        )
    }

    if (showSaveSyncDialog) {
        SaveSyncConfirmationDialog(
            showServerOption = preferenceHelper.getStorageMode() == PreferenceHelper.MODE_CLOUD,
            onDismiss = { showSaveSyncDialog = false },
            onConfirm = { sync ->
                viewModel.approve(sync)
                showSaveSyncDialog = false
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
            ApprovalDetailTopBar(
                isApproved = uiState.record?.status == PaymentStatus.APPROVED,
                onNavigateBack = onNavigateBack,
                onDeleteClick = { showDeleteDialog = true }
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
                uiState.image?.let { ReceiptImage(it.localPath) }

                TagSection(
                    record = record,
                    tagMap = uiState.tagMap,
                    onEditTags = { showTagDialog = true }
                )

                HorizontalDivider()

                TextField(
                    value = record.description ?: "",
                    onValueChange = { viewModel.updateRecord(record.copy(description = it)) },
                    label = { Text("說明/商家") },
                    modifier = Modifier.fillMaxWidth()
                )

                AmountRow(
                    amount = record.amount,
                    currency = record.currency ?: "TWD",
                    onAmountChange = { viewModel.updateRecord(record.copy(amount = it)) },
                    onCurrencyChange = { viewModel.updateRecord(record.copy(currency = it)) }
                )

                if (record.currency?.uppercase() != "TWD") {
                    TextField(
                        value = record.amountTwd?.toString() ?: "",
                        onValueChange = { viewModel.updateRecord(record.copy(amountTwd = it.toDoubleOrNull())) },
                        label = { Text("約當台幣") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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

                PaymentMethodDropdown(
                    selectedMethod = record.method,
                    ewallets = uiState.ewallets,
                    onMethodSelected = { viewModel.updateRecord(record.copy(method = it)) }
                )

                CreditCardDropdown(
                    selectedAccount = record.account,
                    cards = uiState.cards,
                    onCardSelected = { card ->
                        viewModel.updateRecord(record.copy(account = card?.name, cardLast4 = card?.last4))
                    }
                )

                Button(
                    onClick = { 
                        if (preferenceHelper.getStorageMode() == PreferenceHelper.MODE_CLOUD && record.status == PaymentStatus.APPROVED) {
                            showSaveSyncDialog = true
                        } else {
                            viewModel.approve(true)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (record.status == PaymentStatus.APPROVED) "儲存修改" else "核准並送出")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApprovalDetailTopBar(
    isApproved: Boolean,
    onNavigateBack: () -> Unit,
    onDeleteClick: () -> Unit
) {
    TopAppBar(
        title = { Text(if (isApproved) "編輯消費記錄" else "核准消費記錄") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "刪除記錄", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
}
