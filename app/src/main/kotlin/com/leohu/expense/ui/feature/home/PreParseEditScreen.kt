package com.leohu.expense.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.leohu.expense.domain.model.Tag
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreParseEditScreen(
    viewModel: PreParseEditViewModel,
    onNavigateBack: () -> Unit,
    onParseComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showTagDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isParsed) {
        if (uiState.isParsed) {
            onParseComplete()
        }
    }

    if (showTagDialog && uiState.selectedImageId != null) {
        SelectTagDialog(
            tags = uiState.allTags,
            selectedTagIds = uiState.selectedImage?.tagIds ?: emptyList(),
            onDismiss = { showTagDialog = false },
            onToggle = { tagId -> viewModel.toggleTagForImage(uiState.selectedImageId!!, tagId) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("解析前編輯") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!uiState.isParsing && uiState.images.isNotEmpty()) {
                        Button(onClick = { viewModel.startParsing(context) }) {
                            Text("開始解析")
                        }
                    }
                }
            )
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
            // 圖片選擇區域
            Text("收據圖片 (${uiState.images.size})", style = MaterialTheme.typography.titleMedium)
            
            LazyRow(
                modifier = Modifier.height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.images, key = { it.id }) { image ->
                    ImageItemCard(
                        image = image,
                        isSelected = image.id == uiState.selectedImageId,
                        onSelect = { viewModel.selectImage(image.id) },
                        onDelete = { viewModel.removeImage(image.id) }
                    )
                }
            }

            // 批量說明文字
            TextField(
                value = uiState.batchDescription,
                onValueChange = { viewModel.updateBatchDescription(it) },
                label = { Text("批次說明（所有圖片共用）") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // 單個圖片編輯區
            val selectedImage = uiState.selectedImage
            if (selectedImage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "編輯目前圖片",
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        TextField(
                            value = selectedImage.preDescription ?: "",
                            onValueChange = { viewModel.updateImageDescription(it) },
                            label = { Text("這張圖的特別說明") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        // Tag 區域
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("標籤 (Tags)", style = MaterialTheme.typography.labelLarge)
                                TextButton(onClick = { showTagDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("管理標籤")
                                }
                            }
                            
                            if (selectedImage.tagIds.isEmpty()) {
                                Text("尚未設定標籤", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    selectedImage.tagIds.forEach { tagId ->
                                        uiState.allTags.find { it.id == tagId }?.let { tag ->
                                            SuggestionChip(
                                                onClick = { viewModel.toggleTagForImage(selectedImage.id, tagId) },
                                                label = { Text(tag.name) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isParsing) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ImageItemCard(
    image: SourceImageEditItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Box(modifier = Modifier.width(100.dp)) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, end = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            onClick = onSelect
        ) {
            AsyncImage(
                model = File(image.localPath),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // 刪除小按鈕
        Surface(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopEnd)
                .clickable { onDelete() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            tonalElevation = 4.dp
        ) {
            Icon(
                Icons.Default.Close, 
                contentDescription = "刪除", 
                modifier = Modifier.padding(4.dp),
                tint = Color.White
            )
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
                    Text("請先到設定中建立標籤")
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
                                Box(modifier = Modifier.size(16.dp).background(Color(android.graphics.Color.parseColor(tag.color)), CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(tag.name)
                            }
                            Checkbox(checked = isSelected, onCheckedChange = { onToggle(tag.id) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}
