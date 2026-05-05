package com.example.travelplanner.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelplanner.domain.model.PointCategory
import com.example.travelplanner.domain.model.RoutePoint
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.presentation.trip.detail.TripDetailViewModel
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel,
    onBackClick: () -> Unit,
    onAddRoutePointClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("uk"))
    val context = LocalContext.current

    val trip by viewModel.trip.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.title ?: "Деталі поїздки") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    val isGenerating by viewModel.isGeneratingInvite.collectAsState()
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp).size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.createInviteLink(
                                    onLinkGenerated = { link ->
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, link)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Поділитись"))
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Поділитись")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRoutePointClick) {
                Icon(Icons.Default.Add, contentDescription = "Додати точку маршруту")
            }
        }
    ) { paddingValues ->
        val currentTrip = trip
        if (currentTrip == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TripInfoCard(trip = currentTrip, dateFormat = dateFormat)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Маршрут (${routePoints.size} точок)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (routePoints.size >= 2) {
                        val totalKm = routePoints.zipWithNext()
                            .sumOf { (a, b) -> a.distanceTo(b) }
                        Text(
                            text = "${"%.0f".format(totalKm)} км",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (routePoints.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Точок маршруту ще немає.\nНатисніть + щоб додати першу.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(routePoints, key = { it.id }) { point ->
                    RoutePointCard(
                        point = point,
                        onDelete = { viewModel.deleteRoutePoint(point) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TripInfoCard(trip: Trip, dateFormat: java.text.SimpleDateFormat) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Інформація про поїздку",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(label = "Місце призначення", value = trip.destination)
            DetailRow(label = "Початок", value = dateFormat.format(trip.startDate))
            DetailRow(label = "Кінець", value = dateFormat.format(trip.endDate))
            DetailRow(label = "Бюджет", value = "${"%.2f".format(trip.totalBudget)} ${trip.currencyCode}")
            DetailRow(
                label = "Синхронізовано",
                value = if (trip.isSynced) "Так" else "Ні (офлайн)"
            )
            if (trip.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Нотатки",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = trip.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RoutePointCard(
    point: RoutePoint,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.forLanguageTag("uk"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (point.isVisited)
                MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = when (point.category) {
                    PointCategory.HOTEL -> Icons.Default.Hotel
                    PointCategory.RESTAURANT -> Icons.Default.Restaurant
                    PointCategory.TRANSPORT -> Icons.Default.Train
                    PointCategory.ATTRACTION -> Icons.Default.Star
                    PointCategory.OTHER -> Icons.Default.Place
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = point.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = point.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${dateFormat.format(point.arrivalDate)}, ${point.durationDays} дн.",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (point.estimatedCost > 0) {
                        Text(
                            text = "${"%.0f".format(point.estimatedCost)} ${point.currencyCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${"%.2f".format(point.latitude)}, ${"%.2f".format(point.longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (point.notes.isNotBlank()) {
                    Text(
                        text = point.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (point.isVisited) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(start = 8.dp)
            ) {
                Text(
                    text = "Видалити",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
