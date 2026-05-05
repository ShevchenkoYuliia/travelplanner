package com.example.travelplanner.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelplanner.navigation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeferredOnboardingScreen(
    deepLink: String,
    destination: Destination?,
    onOpenFully: () -> Unit,
    onSkip: () -> Unit
) {
    val description = when (destination) {
        is Destination.Detail -> "Ви перейшли за посиланням на деталі елемента ${destination.id}."
        is Destination.Catalog -> if (destination.filter.isNullOrBlank()) {
            "Ви перейшли на каталог без фільтра."
        } else {
            "Ви перейшли на каталог з фільтром '${destination.filter}'."
        }
        is Destination.Invite -> "Вас запросили за токеном ${destination.token}."
        is Destination.Public -> "Ви відкрили публічну сторінку застосунку."
        else -> "Ви перейшли за посиланням: $deepLink"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ласкаво просимо") },
                navigationIcon = {
                    IconButton(onClick = onSkip) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Дякуємо, що встановили наш застосунок!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ви потрапили сюди за посиланням:",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = deepLink,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onOpenFully,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Відкрити повністю")
            }
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Пропустити")
            }
        }
    }
}
