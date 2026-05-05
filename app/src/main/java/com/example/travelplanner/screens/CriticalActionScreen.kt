package com.example.travelplanner.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.presentation.trip.list.TripListViewModel
import com.example.travelplanner.security.BiometricAuthResult
import com.example.travelplanner.security.BiometricAuthState
import com.example.travelplanner.security.SecurityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriticalActionScreen(
    tripId: String,
    tripListViewModel: TripListViewModel,
    securityViewModel: SecurityViewModel,
    onBackClick: () -> Unit,
    onActionConfirmed: () -> Unit
) {
    val trips by tripListViewModel.trips.collectAsState()
    val securityState by securityViewModel.uiState.collectAsState()
    val trip = trips.firstOrNull { it.id == tripId }
    val authState = securityState.authState
    val busy = authState == BiometricAuthState.Authenticating

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Підтвердження дії") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Видалення поїздки",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = trip?.let { "Підтвердіть видалення: ${it.title}" }
                    ?: "Поїздку не знайдено",
                style = MaterialTheme.typography.bodyLarge
            )
            when (authState) {
                BiometricAuthState.Success -> Text(
                    text = "Підтверджено",
                    color = MaterialTheme.colorScheme.primary
                )
                is BiometricAuthState.Failed -> Text(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.error
                )
                is BiometricAuthState.Unavailable -> Text(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.error
                )
                else -> Unit
            }
            Button(
                onClick = {
                    securityViewModel.authenticateAsync("Підтвердіть видалення поїздки") { result ->
                        if (result == BiometricAuthResult.Success && trip != null) {
                            tripListViewModel.deleteTrip(trip)
                            onActionConfirmed()
                        }
                    }
                },
                enabled = trip != null && !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Перевірка..." else "Підтвердити біометрією")
            }
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Скасувати")
            }
        }
    }
}
