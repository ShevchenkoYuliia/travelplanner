package com.example.travelplanner.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.domain.result.SyncWriteResult
import com.example.travelplanner.presentation.trip.list.TripListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    viewModel: TripListViewModel,
    preferredCurrency: String,
    onBackClick: () -> Unit,
    onTripSaved: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember { mutableStateOf(Date()) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf(false) }
    var destinationError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("uk"))

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = Date(it)
                        dateError = false
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Скасувати") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        endDate = Date(it)
                        dateError = false
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Скасувати") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.uiMessages.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },

        topBar = {
            TopAppBar(
                title = { Text("Нова поїздка") },
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
            Text(
                text = "Заповніть деталі поїздки",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = false },
                label = { Text("Назва поїздки *") },
                placeholder = { Text("Наприклад: Японський місяць") },
                isError = titleError,
                supportingText = if (titleError) {{ Text("Обов'язкове поле") }} else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it; destinationError = false },
                label = { Text("Місце призначення *") },
                placeholder = { Text("Наприклад: Токіо, Японія") },
                isError = destinationError,
                supportingText = if (destinationError) {{ Text("Обов'язкове поле") }} else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = dateFormat.format(startDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("Дата початку") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showStartDatePicker = true }) {
                        Text("Змінити")
                    }
                }
            )

            OutlinedTextField(
                value = dateFormat.format(endDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("Дата завершення") },
                isError = dateError,
                supportingText = if (dateError) {{ Text("Дата завершення має бути після початку") }} else null,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showEndDatePicker = true }) {
                        Text("Змінити")
                    }
                }
            )

            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it },
                label = { Text("Бюджет ($preferredCurrency)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = { Text(currencySymbol(preferredCurrency)) }
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Нотатки") },
                placeholder = { Text("Плани, побажання, нагадування...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isSaving) return@Button
                    titleError = title.isBlank()
                    destinationError = destination.isBlank()
                    dateError = endDate.before(startDate)

                    if (!titleError && !destinationError && !dateError) {
                        val newTrip = Trip(
                            title = title.trim(),
                            destination = destination.trim(),
                            totalBudget = budget.toDoubleOrNull() ?: 0.0,
                            currencyCode = preferredCurrency,
                            startDate = startDate,
                            endDate = endDate,
                            notes = notes.trim()
                        )
                        scope.launch {
                            isSaving = true
                            when (val result = viewModel.addTripAndAwait(newTrip)) {
                                SyncWriteResult.Synced -> onTripSaved()
                                is SyncWriteResult.Queued -> {
                                    delay(6_000)
                                    onTripSaved()
                                }
                            }
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Збереження..." else "Зберегти поїздку")
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

private fun currencySymbol(currencyCode: String): String = when (currencyCode.uppercase()) {
    "USD" -> "$"
    "EUR" -> "EUR"
    "UAH" -> "UAH"
    "GBP" -> "GBP"
    "JPY" -> "JPY"
    "PLN" -> "PLN"
    else -> currencyCode.uppercase()
}
