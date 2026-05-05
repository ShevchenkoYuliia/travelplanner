package com.example.travelplanner.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelplanner.models.UserProfile
import com.example.travelplanner.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    isLoggedIn: Boolean,
    onEditProfileClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onRealtimeStatusClick: () -> Unit,
    onRealtimeEventsClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val user by viewModel.userProfile.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("uk"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профіль") },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = onEditProfileClick) {
                            Icon(Icons.Default.Edit, contentDescription = "Редагувати профіль")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isLoggedIn) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Увійдіть або зареєструйтеся, щоб переглядати профіль і статистику поїздок",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Увійти в профіль")
                }
                OutlinedButton(
                    onClick = onRegisterClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Зареєструватися")
                }
                return@Column
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val initials = user.displayName
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                Text(
                    text = initials.ifBlank { "?" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = user.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (user.isPremium) {
                AssistChip(
                    onClick = {},
                    label = { Text("Premium") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            HorizontalDivider()

            Text(
                text = "Статистика",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Поїздок",
                    value = user.totalTrips.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Км маршрутів",
                    value = if (user.totalKmTraveled >= 1000)
                        "${"%.1f".format(user.totalKmTraveled / 1000)} тис. км"
                    else
                        "${"%.0f".format(user.totalKmTraveled)} км",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Км рахуються між точками маршруту кожної поїздки",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            HorizontalDivider()

            Text(
                text = "Налаштування",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow(label = "Рідне місто", value = user.homeCity.ifBlank { "Не вказано" })
                    DetailRow(label = "Валюта", value = user.preferredCurrency)
                    DetailRow(label = "Дата реєстрації", value = dateFormat.format(user.registeredAt))
                }
            }

            OutlinedButton(
                onClick = onEditProfileClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Редагувати профіль")
            }
            OutlinedButton(
                onClick = onRealtimeStatusClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Стан WebSocket")
            }
            OutlinedButton(
                onClick = onRealtimeEventsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Події сервера")
            }
            OutlinedButton(
                onClick = onSecurityClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Безпека")
            }
            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Вийти з профілю")
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}
