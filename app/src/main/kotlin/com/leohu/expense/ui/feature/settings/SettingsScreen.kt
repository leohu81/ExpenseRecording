package com.leohu.expense.ui.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCards: () -> Unit,
    onNavigateToEWallets: () -> Unit
) {
    val context = LocalContext.current
    var retentionDays by remember { mutableStateOf("90") }
    var storageMode by remember { mutableStateOf(viewModel.storageMode) }
    var showTagDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 監聽開關狀態變化 (從 ViewModel 同步到本地 State)
    val enablePreParseEdit by viewModel.enablePreParseEdit.collectAsState()

    // Export Backup Launcher (JSON)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(context, it) { success ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (success) "備份匯出成功" else "備份匯出失敗")
                }
            }
        }
    }

    // Import Backup Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importBackup(context, it) { success, error ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (success) "備份匯入成功" else "匯入失敗: $error")
                }
            }
        }
    }

    // Export CSV Launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportToCsv(context, it) { success, error ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (success) "CSV匯出成功" else "匯入失敗: $error")
                }
            }
        }
    }

    if (showTagDialog) {
        AddTagDialog(
            onDismiss = { showTagDialog = false },
            onAdd = { name, color ->
                viewModel.addTag(name, color)
                showTagDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
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
            Text("環境設定", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNavigateToCards, modifier = Modifier.weight(1f)) {
                    Text("信用卡維護")
                }
                OutlinedButton(onClick = onNavigateToEWallets, modifier = Modifier.weight(1f)) {
                    Text("電子支付維護")
                }
            }

            HorizontalDivider()

            // Tag Management Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tag 管理", style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = { showTagDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "新增 Tag")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val tags by viewModel.allTags.collectAsState(initial = emptyList())
                    if (tags.isEmpty()) {
                        Text("尚無 Tag，點擊 + 新增", style = MaterialTheme.typography.bodySmall)
                    } else {
                        tags.forEach { tag ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(tag.color)))
                                )
                                Text(tag.name, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.deleteTag(tag) }) {
                                    Text("刪除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Pre-parse Edit Toggle
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("解析設定", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("啟用解析前編輯")
                        Switch(
                            checked = enablePreParseEdit,
                            onCheckedChange = { viewModel.togglePreParseEdit(it) }
                        )
                    }
                    Text(
                        "開啟後，上傳圖片後可先編輯說明再進行解析",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Storage Mode Setting
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("資料儲存位置", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                storageMode = "cloud"
                                viewModel.setStorageMode("cloud")
                                scope.launch { snackbarHostState.showSnackbar("已切換至雲端儲存") }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = storageMode != "cloud"
                        ) {
                            Text("雲端儲存")
                        }
                        OutlinedButton(
                            onClick = { 
                                storageMode = "local"
                                viewModel.setStorageMode("local")
                                scope.launch { snackbarHostState.showSnackbar("已切換至本地儲存") }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = storageMode != "local"
                        ) {
                            Text("本地儲存")
                        }
                    }
                    Text(
                        if (storageMode == "cloud") "目前：雲端（核准後上傳至伺服器）" else "目前：本地（核准後僅保存於手機上）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Text("資料清理", style = MaterialTheme.typography.titleMedium)
            
            TextField(
                value = retentionDays,
                onValueChange = { retentionDays = it },
                label = { Text("已核准記錄保留天數") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { 
                    val days = retentionDays.toIntOrNull() ?: 90
                    viewModel.cleanupNow(days)
                    scope.launch { snackbarHostState.showSnackbar("清理完成") }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("立即批次刪除過期記錄")
            }
            
            Text(
                "說明：這將永久刪除超過指定天數的已核准消費記錄及其對應的截圖檔案。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Text("備份與還原", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = { exportLauncher.launch("expense_backup_${System.currentTimeMillis()}.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("匯出備份 (信用卡與電子支付)")
            }

            OutlinedButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("匯入備份")
            }

            // CSV Export (only available in local mode)
            if (storageMode == "local") {
                OutlinedButton(
                    onClick = { csvLauncher.launch("expense_records_${System.currentTimeMillis()}.csv") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("匯出歷史記錄 (CSV)")
                }
                Text(
                    "匯出的 CSV 檔案包含：日期、金額、幣別、支付方式、帳戶、末四碼、商家名稱、標籤。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Simulate Failure Button - Only in Debug mode and moved to the end
            // Note: In Compose with R8/Minification, BuildConfig.DEBUG might not be enough
            // Using a system property check or similar for a more robust "Debug Only" UI
            if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                HorizontalDivider()
                Text("開發者調試", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("調試功能", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.simulateParsingFailure()
                                scope.launch {
                                    snackbarHostState.showSnackbar("已觸發模擬解析失敗")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("模擬解析失敗 (Debug Only)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#FFF9C4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增 Tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tag 名稱") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("顏色:")
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(android.graphics.Color.parseColor(color)))
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, color)
                    }
                }
            ) {
                Text("新增")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
