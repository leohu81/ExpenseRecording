package com.leohu.expense.ui.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.leohu.expense.util.PreferenceHelper
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToApproval: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFailedList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPreParseEdit: (List<String>) -> Unit,
    onNavigateToManualEntry: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddOptions by remember { mutableStateOf(false) }
    val preferenceHelper = (context.applicationContext as com.leohu.expense.app.ExpenseApplication).preferenceHelper
    val scrollState = rememberScrollState()

    // Request Notification Permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

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
    
    // Gallery Launcher for multiple images
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(
                context = context,
                uris = uris,
                shouldEdit = preferenceHelper.getBool(PreferenceHelper.KEY_ENABLE_PRE_PARSE_EDIT, false),
                onNavigateToEdit = onNavigateToPreParseEdit
            )
        }
    }

    // Camera Launcher
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraImageUri?.let { uri ->
                viewModel.onImagesSelected(
                    context = context,
                    uris = listOf(uri),
                    shouldEdit = preferenceHelper.getBool(PreferenceHelper.KEY_ENABLE_PRE_PARSE_EDIT, false),
                    onNavigateToEdit = onNavigateToPreParseEdit
                )
            }
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
                            onNavigateToManualEntry()
                            showAddOptions = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "手動輸入")
                    }
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
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("收據處理狀態", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("截圖處理: ${uiState.pendingImageCount}")
                    Text("分析中: ${uiState.processingImageCount}")
                    Text("待核准: ${uiState.pendingApprovalCount}")
                    if (uiState.failedImageCount > 0) {
                        Text(
                            "解析失敗: ${uiState.failedImageCount}", 
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable {
                                onNavigateToFailedList()
                            }
                        )
                    } else {
                        Text("解析失敗: ${uiState.failedImageCount}", color = MaterialTheme.colorScheme.error)
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
                Text("查看全部記錄")
            }

            OutlinedButton(onClick = onNavigateToSettings, modifier = Modifier.fillMaxWidth()) {
                Text("設定")
            }
        }
    }
}
