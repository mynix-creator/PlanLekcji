package com.mynix.planlekcji.ui.timetable

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mynix.planlekcji.ui.viewmodel.SettingsViewModel
import com.mynix.planlekcji.ui.viewmodel.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    timetableViewModel: TimetableViewModel = viewModel(),
    onBack: () -> Unit
) {
    val enabled by viewModel.notifyEnabled.collectAsStateWithLifecycle()
    val minutes by viewModel.notifyMinutes.collectAsStateWithLifecycle()
    
    val isImporting by timetableViewModel.isImporting.collectAsStateWithLifecycle()
    val importResult by timetableViewModel.importResult.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(importResult) {
        importResult?.let { result ->
            if (result.success) {
                snackbarHostState.showSnackbar("Pomyślnie zaimportowano ${result.count} lekcji")
            } else {
                snackbarHostState.showSnackbar(if (result.errorCount > 0) "Błąd importu: ${result.source}" else "Plik .ics jest pusty")
            }
            timetableViewModel.clearImportResult()
        }
    }

    val icsPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { timetableViewModel.importFromIcs(it) } }

    var showClearConfirm by remember { mutableStateOf(false) }

    val options = listOf(5, 10, 15, 20, 30, 45, 60)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
            Text("Powiadomienia", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Włącz powiadomienia przed lekcją",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        viewModel.setEnabled(it)
                    }
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            AnimatedVisibility(visible = enabled) {
                Column {
                    Text(
                        "Czas przed lekcją",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.forEach { opt ->
                            FilterChip(
                                selected = minutes == opt,
                                onClick = {
                                    viewModel.setMinutes(opt)
                                },
                                label = { Text("$opt min") }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(32.dp))

            Text("Import danych", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { icsPickerLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Importuj z Librusa (.ics)")
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                "Pobierz plik z Librusa: Plan lekcji → Eksportuj → .ics\n" +
                "Aplikacja automatycznie pobierze plan z ostatniego tygodnia roku szkolnego.\n" +
                "Sala musi być uzupełniona ręcznie po imporcie.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Wyczyść plan lekcji")
            }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text("Wyczyść plan?") },
                    text  = { Text("Wszystkie lekcje zostaną usunięte. Tej operacji nie można cofnąć.") },
                    confirmButton = {
                        TextButton(onClick = {
                            timetableViewModel.clearAllLessons()
                            showClearConfirm = false
                        }) {
                            Text("Wyczyść", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) {
                            Text("Anuluj")
                        }
                    }
                )
            }
        }

        if (isImporting) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Importowanie planu...", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
}
