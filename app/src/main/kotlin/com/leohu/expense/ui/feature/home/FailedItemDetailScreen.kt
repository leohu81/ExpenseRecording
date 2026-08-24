package com.leohu.expense.ui.feature.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.leohu.expense.domain.model.SourceImageStatus
import java.io.File

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

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
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
                    .padding(16.dp)
                    .verticalScroll(scrollState)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 顯示原始截圖
                sourceImage.localPath.let { path ->
                    File(path).takeIf { it.exists() }?.let { file ->
                        BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "原始截圖",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // 只有失敗或有錯誤時才顯示解析失敗原因區塊
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

                // 顯示欄位（唯讀）
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

                TextField(
                    value = record?.method ?: "",
                    onValueChange = { /* 唯讀 */ },
                    label = { Text("支付方式") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = record?.account ?: "",
                    onValueChange = { /* 唯讀 */ },
                    label = { Text("帳戶/信用卡") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 幣別顯示
                val displayCurrency = record?.currency ?: "TWD"
                if (displayCurrency.uppercase() != "TWD") {
                    Text(
                        text = "幣別：$displayCurrency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (sourceImage.status == SourceImageStatus.FAILED) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.retryParse(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("再次送出解析")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
