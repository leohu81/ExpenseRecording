package com.leohu.expense.ui.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToApproval: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCards: () -> Unit,
    onNavigateToEWallets: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddOptions by remember { mutableStateOf(false) }

    fun launchCameraInternal(uriCallback: (Uri) -> Unit) {
        val file = File(context.filesDir, "Pictures").apply { mkdirs() }
        val imageFile = File(file, "IMG_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        uriCallback(uri)
    }
    
    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(context, it) }
    }

    // Camera Launcher
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { viewModel.onImageSelected(context, it) }
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCameraInternal { uri ->
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            }
        }
    }

    fun handleCameraClick() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                launchCameraInternal { uri ->
                    cameraImageUri = uri
                    cameraLauncher.launch(uri)
                }
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("記帳助手") })
        },
        floatingActionButton = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                if (showAddOptions) {
                    SmallFloatingActionButton(
                        onClick = {
                            galleryLauncher.launch("image/*")
                            showAddOptions = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "從相簿選擇")
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            handleCameraClick()
                            showAddOptions = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "拍照")
                    }
                }
                FloatingActionButton(onClick = { showAddOptions = !showAddOptions }) {
                    Icon(if (showAddOptions) Icons.Default.Close else Icons.Default.Add, contentDescription = "新增收據")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("收據解析狀態", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("等待壓縮/解析: ${uiState.pendingImageCount}")
                    Text("正在解析中: ${uiState.processingImageCount}")
                    Text("解析完成: ${uiState.readyImageCount}")
                    Text("解析失敗: ${uiState.failedImageCount}", color = MaterialTheme.colorScheme.error)

                    if (uiState.failedImages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.failedImages.forEach { image ->
                            Text(
                                text = "錯誤: ${image.lastError ?: "未知原因"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onNavigateToApproval,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.pendingApprovalCount > 0
            ) {
                Text("待核准項目 (${uiState.pendingApprovalCount})")
            }

            Button(
                onClick = onNavigateToHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("查看歷史記錄")
            }

            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNavigateToCards, modifier = Modifier.weight(1f)) {
                    Text("信用卡")
                }
                OutlinedButton(onClick = onNavigateToEWallets, modifier = Modifier.weight(1f)) {
                    Text("電子支付")
                }
            }

            OutlinedButton(onClick = onNavigateToSettings, modifier = Modifier.fillMaxWidth()) {
                Text("設定")
            }
        }
    }
}
