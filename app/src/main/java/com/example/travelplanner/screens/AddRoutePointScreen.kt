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
import com.example.travelplanner.domain.model.PointCategory
import com.example.travelplanner.domain.model.RoutePoint
import com.example.travelplanner.domain.result.SyncWriteResult
import com.example.travelplanner.presentation.trip.detail.TripDetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoutePointScreen(
    tripId: String,
    viewModel: TripDetailViewModel,
    preferredCurrency: String,
    onBackClick: () -> Unit,
    onPointSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitudeStr by remember { mutableStateOf("") }
    var longitudeStr by remember { mutableStateOf("") }
    var durationDaysStr by remember { mutableStateOf("1") }
    var estimatedCostStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PointCategory.OTHER) }
    var arrivalDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var latError by remember { mutableStateOf(false) }
    var lonError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("uk"))

    val categories = PointCategory.entries.toList()
    val categoryLabels = mapOf(
        PointCategory.HOTEL to "Готель",
        PointCategory.ATTRACTION to "Атракція",
        PointCategory.RESTAURANT to "Ресторан",
        PointCategory.TRANSPORT to "Транспорт",
        PointCategory.OTHER to "Інше"
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = arrivalDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { arrivalDate = Date(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Скасувати") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Нова точка маршруту") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Назва точки *") },
                placeholder = { Text("Наприклад: Готель Shinjuku") },
                isError = nameError,
                supportingText = if (nameError) {{ Text("Обов'язкове поле") }} else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it; addressError = false },
                label = { Text("Адреса *") },
                placeholder = { Text("Вулиця, місто, країна") },
                isError = addressError,
                supportingText = if (addressError) {{ Text("Обов'язкове поле") }} else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "Координати (для підрахунку км)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitudeStr,
                    onValueChange = { latitudeStr = it; latError = false },
                    label = { Text("Широта *") },
                    placeholder = { Text("35.6938") },
                    isError = latError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = longitudeStr,
                    onValueChange = { longitudeStr = it; lonError = false },
                    label = { Text("Довгота *") },
                    placeholder = { Text("139.7034") },
                    isError = lonError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            if (latError || lonError) {
                Text(
                    text = "Введіть коректні координати",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = dateFormat.format(arrivalDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("Дата прибуття") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Змінити") }
                }
            )

            OutlinedTextField(
                value = durationDaysStr,
                onValueChange = { durationDaysStr = it },
                label = { Text("Кількість днів") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "Категорія",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(categoryLabels[cat] ?: cat.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = estimatedCostStr,
                onValueChange = { estimatedCostStr = it },
                label = { Text("Орієнтовна вартість ($preferredCurrency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = { Text(currencySymbol(preferredCurrency)) }
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Нотатки") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    if (isSaving) return@Button
                    nameError = name.isBlank()
                    addressError = address.isBlank()
                    val lat = latitudeStr.toDoubleOrNull()
                    val lon = longitudeStr.toDoubleOrNull()
                    latError = lat == null || lat !in -90.0..90.0
                    lonError = lon == null || lon !in -180.0..180.0

                    if (!nameError && !addressError && !latError && !lonError && lat != null && lon != null) {
                        val point = RoutePoint(
                            tripId = tripId,
                            name = name.trim(),
                            address = address.trim(),
                            latitude = lat,
                            longitude = lon,
                            arrivalDate = arrivalDate,
                            durationDays = durationDaysStr.toIntOrNull() ?: 1,
                            estimatedCost = estimatedCostStr.toDoubleOrNull() ?: 0.0,
                            currencyCode = preferredCurrency,
                            category = selectedCategory,
                            notes = notes.trim()
                        )
                        scope.launch {
                            isSaving = true
                            when (val result = viewModel.addRoutePointAndAwait(point)) {
                                SyncWriteResult.Synced -> onPointSaved()
                                is SyncWriteResult.Queued -> {
                                    snackbarHostState.showSnackbar(
                                        message = "Дані збережено локально. Синхронізація запуститься автоматично, коли з'явиться інтернет.",
                                        duration = SnackbarDuration.Long
                                    )
                                    onPointSaved()
                                }
                            }
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Збереження..." else "Зберегти точку")
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
