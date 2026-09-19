package com.leohu.expense.ui.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.leohu.expense.data.remote.db.DbProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseSettingsScreen(
    viewModel: DatabaseSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var currentProfile by remember { mutableStateOf<DbProfile?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("數據庫設置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新增")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 當前選中的 Profile
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("當前數據庫", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.currentProfileId?.let { id ->
                        uiState.profiles.find { it.id == id }?.let { profile ->
                            Text("Profile: ${profile.name}", style = MaterialTheme.typography.bodyLarge)
                            Text("Host: ${profile.host}:${profile.port}", style = MaterialTheme.typography.bodyMedium)
                            Text("Database: ${profile.database}", style = MaterialTheme.typography.bodyMedium)
                            Text("User: ${profile.username}", style = MaterialTheme.typography.bodyMedium)
                        } ?: Text("未選擇數據庫", style = MaterialTheme.typography.bodyMedium)
                    } ?: Text("未配置數據庫", style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.testCurrentProfile { success ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) "連接測試成功" else "連接測試失敗"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("測試連接")
                    }
                }
            }
            
            // Profile 列表
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("數據庫配置列表", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (uiState.profiles.isEmpty()) {
                        Text("暫無數據庫配置", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.profiles) { profile ->
                                ProfileCard(
                                    profile = profile,
                                    isSelected = profile.id == uiState.currentProfileId,
                                    onSelect = { viewModel.selectProfile(profile.id) },
                                    onEdit = { 
                                        currentProfile = profile
                                        showEditDialog = true 
                                    },
                                    onDelete = { 
                                        viewModel.deleteProfile(profile.id)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("已刪除")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 新增對話框
        if (showAddDialog) {
            ProfileDialog(
                profile = DbProfile(),
                onDismiss = { showAddDialog = false },
                onSave = { newProfile ->
                    viewModel.addProfile(newProfile)
                    showAddDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("已添加")
                    }
                }
            )
        }
        
        // 編輯對話框
        if (showEditDialog && currentProfile != null) {
            ProfileDialog(
                profile = currentProfile!!,
                onDismiss = { showEditDialog = false },
                onSave = { updatedProfile ->
                    viewModel.updateProfile(updatedProfile)
                    showEditDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("已更新")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileCard(
    profile: DbProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleSmall)
                Text("${profile.host}:${profile.port}", style = MaterialTheme.typography.bodySmall)
                Text(profile.database, style = MaterialTheme.typography.bodySmall)
            }
            if (isSelected) {
                Text("已選中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "編輯")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onSelect) {
                    Text("選擇")
                }
            }
        }
    }
}

@Composable
fun ProfileDialog(
    profile: DbProfile,
    onDismiss: () -> Unit,
    onSave: (DbProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var host by remember { mutableStateOf(profile.host) }
    var port by remember { mutableStateOf(profile.port.toString()) }
    var database by remember { mutableStateOf(profile.database) }
    var username by remember { mutableStateOf(profile.username) }
    var password by remember { mutableStateOf(profile.password) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (profile.id.isEmpty()) "新增數據庫" else "編輯數據庫") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名稱") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = database,
                    onValueChange = { database = it },
                    label = { Text("Database") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    profile.copy(
                        name = name,
                        host = host,
                        port = port.toIntOrNull() ?: 5432,
                        database = database,
                        username = username,
                        password = password
                    )
                )
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
