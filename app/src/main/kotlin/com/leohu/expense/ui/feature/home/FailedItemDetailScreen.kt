package com.leohu.expense.ui.feature.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.leohu.expense.domain.model.SourceImageStatus
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FailedItemDetailScreen(
    viewModel: FailedItemDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("刪除項目") },
            text = { Text("確定要刪除此收據圖片及其關聯資料嗎？") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteItem() }) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    val sourceImage = uiState.sourceImage
    val title = when (sourceImage?.status) {
        SourceImageStatus.FAILED -> "檢視失敗項目"
        SourceImageStatus.PENDING_OCR -> "等待解析中"
        SourceImageStatus.PROCESSING -> "解析處理中"
        SourceImageStatus.READY -> "解析完成"
        else -> "項目詳情"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val record = uiState.record
        
        if (sourceImage == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AsyncImage(
                    model = File(sourceImage.localPath),
                    contentDescription = "原始截圖",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Fit
                )

                if (sourceImage.status == SourceImageStatus.FAILED || sourceImage.lastError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("解析狀態/錯誤訊息", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                sourceImage.lastError ?: "未知狀態",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                TextField(
                    value = record?.description ?: "",
                    onValueChange = { /* 唯讀 */ },
                    label = { Text("說明/商家") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = record?.amount?.toString() ?: "0",
                        onValueChange = { /* 唯讀 */ },
                        label = { Text("金額") },
                        readOnly = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextField(
                        value = record?.currency ?: "TWD",
                        onValueChange = { /* 唯讀 */ },
                        label = { Text("幣別") },
                        readOnly = true,
                        modifier = Modifier.weight(0.5f)
                    )
                }

                TextField(
                    value = record?.consumeDate ?: "",
                    onValueChange = { /* 唯讀 */ },
                    label = { Text("日期 (YYYY/MM/DD)") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (sourceImage.status == SourceImageStatus.FAILED || sourceImage.status == SourceImageStatus.PENDING_OCR) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.retryParse(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (sourceImage.status == SourceImageStatus.PENDING_OCR) "啟動解析流程" else "再次送出解析")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { viewModel.approve() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("直接手動核准")
                    }
                }
            }
        }
    }
}
