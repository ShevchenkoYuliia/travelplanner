package com.example.travelplanner.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.travelplanner.data.realtime.SocketEvent
import com.example.travelplanner.presentation.realtime.RealtimeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeStatusScreen(
    viewModel: RealtimeViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val events by viewModel.events.collectAsState()
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Стан WebSocket") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Поточний стан: $state", style = MaterialTheme.typography.titleMedium)
            Text("Отримано подій: ${events.size}")
            Text("Останні події", style = MaterialTheme.typography.titleSmall)
            if (events.isEmpty()) {
                Text(
                    "Подій WebSocket ще немає",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                events.takeLast(5).reversed().forEach { event ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${event.type}/${event.action}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(event.message, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${event.objectLabel()} • ${dateFormat.format(Date(event.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = viewModel::sendPing, modifier = Modifier.weight(1f)) {
                    Text("Надіслати ping")
                }
                OutlinedButton(onClick = onBackClick, modifier = Modifier.weight(1f)) {
                    Text("Назад")
                }
            }
        }
    }
}

private fun SocketEvent.objectLabel(): String = when {
    userId != null -> "Користувач: $userId"
    tripId != null -> "Поїздка: $tripId"
    else -> "Об'єкт: -"
}
