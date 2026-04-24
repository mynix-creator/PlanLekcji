package com.mynix.planlekcji

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mynix.planlekcji.data.model.Lesson
import com.mynix.planlekcji.ui.theme.PlanLekcjiTheme
import com.mynix.planlekcji.ui.timetable.*
import com.mynix.planlekcji.ui.viewmodel.SettingsViewModel
import com.mynix.planlekcji.ui.viewmodel.TimetableViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        if (!notifGranted) {
            // In a real app we might show a snackbar here, but we need a Scaffold state
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRequiredPermissions()

        // Przelicz alarmy przy każdym starcie (przywraca po aktualizacji apki)
        val settingsViewModel: SettingsViewModel by viewModels()
        lifecycleScope.launch {
            settingsViewModel.rescheduleAll()
        }

        setContent {
            PlanLekcjiTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(onNavigateToSettings = { navController.navigate("settings") })
                    }
                    composable("settings") {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(AlarmManager::class.java)
            if (am != null && !am.canScheduleExactAlarms()) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:$packageName"))
                )
            }
        }
        if (permsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permsToRequest.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TimetableViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onNavigateToSettings: () -> Unit
) {
    val persistedViewMode by settingsViewModel.lastViewMode.collectAsStateWithLifecycle()
    var selectedView by remember { mutableIntStateOf(persistedViewMode) }
    
    // Sync local state with persisted value when it loads/changes
    LaunchedEffect(persistedViewMode) {
        selectedView = persistedViewMode
    }
    
    val schoolDays = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    )
    var selectedDayOfWeek by remember {
        mutableStateOf(
            if (LocalDate.now().dayOfWeek in schoolDays)
                LocalDate.now().dayOfWeek
            else
                DayOfWeek.MONDAY
        )
    }
    
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLessonSheet by remember { mutableStateOf(false) }
    var editingLesson by remember { mutableStateOf<Lesson?>(null) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Lekcji", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        SegmentedButton(
                            selected = selectedView == 0,
                            onClick = { 
                                selectedView = 0
                                settingsViewModel.setLastViewMode(0)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Tydzień")
                        }
                        SegmentedButton(
                            selected = selectedView == 1,
                            onClick = { 
                                selectedView = 1
                                settingsViewModel.setLastViewMode(1)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Dzień")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingLesson = null
                    showLessonSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj lekcję")
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (lessons.isEmpty()) {
                EmptyTimetablePlaceholder()
            } else if (selectedView == 0) {
                WeeklyTimetable(
                    lessons = lessons,
                    onLessonClick = {
                        editingLesson = it
                        showLessonSheet = true
                    },
                    onEmptySpaceLongClick = { day, time ->
                        editingLesson = Lesson(
                            subject = "",
                            teacher = "",
                            room = "",
                            dayOfWeek = day,
                            startTime = time,
                            endTime = time.plusMinutes(45),
                            color = 0 
                        )
                        showLessonSheet = true
                    },
                    onDayHeaderClick = { day ->
                        selectedDayOfWeek = day
                        selectedView = 1
                    }
                )
            } else {
                DailyTimetable(
                    lessons = lessons,
                    selectedDay = selectedDayOfWeek,
                    onDaySelected = { selectedDayOfWeek = it },
                    onLessonClick = {
                        editingLesson = it
                        showLessonSheet = true
                    }
                )
            }
        }

        if (showLessonSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLessonSheet = false },
                sheetState = sheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                LessonEditor(
                    lesson = if (editingLesson?.subject?.isEmpty() == true) null else editingLesson,
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showLessonSheet = false
                        }
                    },
                    onSave = { lesson ->
                        if (lesson.id == 0L) {
                            viewModel.insertLesson(lesson)
                        } else {
                            viewModel.updateLesson(lesson)
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showLessonSheet = false
                        }
                    },
                    onDelete = { lesson ->
                        viewModel.deleteLesson(lesson)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showLessonSheet = false
                        }
                    }
                )
            }
        }
    }
}
