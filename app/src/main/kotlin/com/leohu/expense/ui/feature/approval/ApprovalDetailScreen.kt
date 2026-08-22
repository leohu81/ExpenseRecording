package com.leohu.expense.ui.feature.approval

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.leohu.expense.domain.model.PaymentStatus
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalDetailScreen(
    viewModel: ApprovalDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.record?.status == PaymentStatus.APPROVED) "查看消費記錄" else "核准消費記錄") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val record = uiState.record
        if (uiState.isLoading || record == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val isReadOnly = record.status == PaymentStatus.APPROVED

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

                TextField(
                    value = record.description ?: "",
                    onValueChange = { if (!isReadOnly) viewModel.updateRecord(record.copy(description = it)) },
                    label = { Text("說明/商家") },
                    readOnly = isReadOnly,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = record.amount.toString(),
                        onValueChange = { 
                            if (!isReadOnly) {
                                val newVal = it.toDoubleOrNull() ?: 0.0
                                viewModel.updateRecord(record.copy(amount = newVal)) 
                            }
                        },
                        label = { Text("金額") },
                        readOnly = isReadOnly,
                        modifier = Modifier.weight(1f)
                    )
                    TextField(
                        value = record.currency ?: "TWD",
                        onValueChange = { if (!isReadOnly) viewModel.updateRecord(record.copy(currency = it)) },
                        label = { Text("幣別") },
                        readOnly = isReadOnly,
                        modifier = Modifier.weight(0.5f)
                    )
                }

                TextField(
                    value = record.consumeDate ?: "",
                    onValueChange = { if (!isReadOnly) viewModel.updateRecord(record.copy(consumeDate = it)) },
                    label = { Text("日期 (YYYY/MM/DD)") },
                    readOnly = isReadOnly,
                    modifier = Modifier.fillMaxWidth()
                )

                // 支付方式
                val methodOptions = remember(uiState.ewallets) {
                    listOf("一般刷卡", "現金", "未知") + uiState.ewallets.map { it.name }
                }
                var methodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = if (isReadOnly) false else methodExpanded,
                    onExpandedChange = { if (!isReadOnly) methodExpanded = !methodExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = record.method,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("支付方式") },
                        trailingIcon = { if (!isReadOnly) ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (!isReadOnly) {
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
                }

                // 帳戶/信用卡
                var cardExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = if (isReadOnly) false else cardExpanded,
                    onExpandedChange = { if (!isReadOnly) cardExpanded = !cardExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = record.account ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("帳戶/信用卡") },
                        trailingIcon = { if (!isReadOnly) ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (!isReadOnly) {
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
                }

                if (!isReadOnly) {
                    Button(
                        onClick = { viewModel.approve() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("核准並送出")
                    }
                }
            }
        }
    }
}
