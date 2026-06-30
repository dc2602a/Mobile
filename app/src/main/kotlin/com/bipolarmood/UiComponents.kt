package com.bipolarmood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenFormDialog(
    title: String,
    saveLabel: String = "Сохранить",
    saveEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                        }
                    },
                    actions = {
                        TextButton(onClick = onSave, enabled = saveEnabled) {
                            Text(saveLabel, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun FeedActionIcons(
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        onEdit?.let {
            IconButton(onClick = it) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Редактировать",
                    tint = AppTheme.InkMuted
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Удалить",
                tint = AppTheme.InkMuted
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomMultiSelect(
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    onAddCustom: (String) -> Unit
) {
    var customInput by remember { mutableStateOf("") }
    Text("Симптомы и ощущения", fontWeight = FontWeight.Bold)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { symptom ->
            FilterChip(
                selected = selected.contains(symptom),
                onClick = { onToggle(symptom) },
                label = { Text(symptom) }
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = customInput,
            onValueChange = { customInput = it },
            label = { Text("Своё ощущение") },
            modifier = Modifier.weight(1f)
        )
        Button(
            enabled = customInput.isNotBlank(),
            onClick = {
                onAddCustom(customInput.trim())
                customInput = ""
            }
        ) { Text("Добавить") }
    }
}

@Composable
fun rememberSymptomSelection(initial: List<String> = emptyList()) =
    remember { mutableStateListOf<String>().apply { addAll(initial) } }
