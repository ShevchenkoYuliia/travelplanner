package com.example.travelplanner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.travelplanner.data.realtime.PendingMessage
import com.example.travelplanner.data.realtime.PendingSyncStatus

/**
 * Компонент для відображення pending-повідомлень з офлайн-синхронізацією
 *
 * Показує статус кожного повідомлення:
 * - PendingSync: "Очікує відправки на сервер"
 * - Sent: "Успішно відправлено"
 * - Failed: "Помилка при надсиланні"
 */
@Composable
fun PendingMessagesListUI(
    pendingMessages: List<PendingMessage>,
    modifier: Modifier = Modifier
) {
    if (pendingMessages.isEmpty()) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Синхронізація повідомлень",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(8.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(pendingMessages) { message ->
                PendingMessageItem(message = message)
            }
        }
    }
}

@Composable
fun PendingMessageItem(message: PendingMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (message.status) {
                PendingSyncStatus.PendingSync -> {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Очікує відправки",
                        tint = Color.Yellow,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                PendingSyncStatus.Sent -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Відправлено",
                        tint = Color.Green,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                PendingSyncStatus.Failed -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Помилка",
                        tint = Color.Red,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getStatusText(message.status, message.errorMessage),
                    style = MaterialTheme.typography.labelSmall,
                    color = getStatusColor(message.status)
                )
                if (message.retryCount > 0) {
                    Text(
                        text = "Спроб: ${message.retryCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun getStatusText(status: PendingSyncStatus, errorMessage: String?): String {
    return when (status) {
        PendingSyncStatus.PendingSync -> "Очікує відправки на сервер"
        PendingSyncStatus.Sent -> "Успішно відправлено"
        PendingSyncStatus.Failed -> errorMessage ?: "Помилка при надсиланні"
    }
}

@Composable
private fun getStatusColor(status: PendingSyncStatus): Color {
    return when (status) {
        PendingSyncStatus.PendingSync -> Color.Yellow
        PendingSyncStatus.Sent -> Color.Green
        PendingSyncStatus.Failed -> Color.Red
    }
}
