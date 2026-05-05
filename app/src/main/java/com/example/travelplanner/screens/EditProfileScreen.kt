package com.example.travelplanner.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.travelplanner.domain.result.ProfileSaveResult
import com.example.travelplanner.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val user by viewModel.userProfile.collectAsState()

    var displayName by remember(user.displayName) { mutableStateOf(user.displayName) }
    var email by remember(user.email) { mutableStateOf(user.email) }
    var homeCity by remember(user.homeCity) { mutableStateOf(user.homeCity) }
    var preferredCurrency by remember(user.preferredCurrency) { mutableStateOf(user.preferredCurrency) }

    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var saveBanner by remember { mutableStateOf<ProfileSaveResult?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.profileSaveResult.collect { saveBanner = it }
    }

    val currencies = listOf("USD", "EUR", "UAH", "GBP", "JPY", "PLN")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налаштування профілю") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            when (val banner = saveBanner) {
                is ProfileSaveResult.SyncedToServer -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Збережено на сервері.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                is ProfileSaveResult.SavedLocallyOnly -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = banner.reason?.let { "Збережено локально. $it" }
                                ?: "Збережено локально. Синхронізація буде виконана пізніше.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                null -> Unit
            }

            Text(
                text = "Особисті дані",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it; nameError = false; saveBanner = null },
                label = { Text("Ім'я та прізвище *") },
                isError = nameError,
                supportingText = if (nameError) {{ Text("Обов'язкове поле") }} else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = false; saveBanner = null },
                label = { Text("Email *") },
                isError = emailError,
                supportingText = if (emailError) {{ Text("Введіть коректний email") }} else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = homeCity,
                onValueChange = { homeCity = it; saveBanner = null },
                label = { Text("Рідне місто") },
                placeholder = { Text("Наприклад: Київ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()

            Text(
                text = "Налаштування",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Бажана валюта",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currencies.forEach { currency ->
                    FilterChip(
                        selected = preferredCurrency == currency,
                        onClick = { preferredCurrency = currency; saveBanner = null },
                        label = { Text(currency) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    nameError = displayName.isBlank()
                    emailError = email.isBlank() || !email.contains("@")

                    if (!nameError && !emailError) {
                        viewModel.updateProfile(
                            displayName = displayName.trim(),
                            email = email.trim(),
                            homeCity = homeCity.trim(),
                            preferredCurrency = preferredCurrency
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Зберегти зміни")
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
