package com.leohu.expense.ui.feature.approval

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.leohu.expense.domain.model.Tag

@Composable
fun SelectTagDialog(
    tags: List<Tag>,
    selectedTagIds: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onDeselect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇標籤") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tags.isEmpty()) {
                    Text("請先到設定中建立標籤", style = MaterialTheme.typography.bodySmall)
                } else {
                    tags.forEach { tag ->
                        val isSelected = selectedTagIds.contains(tag.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (isSelected) onDeselect(tag.id) else onSelect(tag.id) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val tagColor = try {
                                    Color(android.graphics.Color.parseColor(tag.color))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(tagColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(tag.name)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { if (isSelected) onDeselect(tag.id) else onSelect(tag.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    showServerOption: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var deleteFromServer by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("刪除記錄") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("確定要刪除這筆消費記錄嗎？此動作無法復原。")
                if (showServerOption) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { deleteFromServer = !deleteFromServer }
                    ) {
                        Checkbox(
                            checked = deleteFromServer,
                            onCheckedChange = { deleteFromServer = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("同時刪除伺服器上的記錄")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(deleteFromServer) }) {
                Text("刪除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun SaveSyncConfirmationDialog(
    showServerOption: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var syncWithServer by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("確認動作") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("是否確認儲存變更？")
                if (showServerOption) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { syncWithServer = !syncWithServer }
                    ) {
                        Checkbox(
                            checked = syncWithServer,
                            onCheckedChange = { syncWithServer = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("同時同步至伺服器")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(syncWithServer) }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
