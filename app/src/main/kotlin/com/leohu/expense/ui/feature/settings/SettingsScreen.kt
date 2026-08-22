package com.leohu.expense.ui.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var retentionDays by remember { mutableStateOf("90") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Export Launcher
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

    // Import Launcher
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
        }
    }
}
