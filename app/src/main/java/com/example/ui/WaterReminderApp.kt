package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HistoryEntry
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

enum class AppTab {
    TRACKER,
    STATS_SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterReminderApp(viewModel: WaterViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.TRACKER) }

    // State collections
    val dailyGoal by viewModel.dailyGoal.collectAsStateWithLifecycle()
    val todayGlasses by viewModel.todayGlasses.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val reminderInterval by viewModel.reminderInterval.collectAsStateWithLifecycle()
    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()

    // Handle runtime notification permissions for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled! 💧", Toast.LENGTH_SHORT).show()
            // Reset background reminder tasks
            viewModel.setupOnStartup()
        } else {
            Toast.makeText(context, "Permission denied. Reminders won't show.", Toast.LENGTH_LONG).show()
        }
    }

    // Run startup registration side effects
    LaunchedEffect(Unit) {
        viewModel.setupOnStartup()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "💧",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Water Reminder",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.TRACKER,
                    onClick = { currentTab = AppTab.TRACKER },
                    label = { Text("Tracker") },
                    icon = {
                        Text(
                            text = "💧",
                            fontSize = 20.sp
                        )
                    },
                    modifier = Modifier.testTag("nav_tracker")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.STATS_SETTINGS,
                    onClick = { currentTab = AppTab.STATS_SETTINGS },
                    label = { Text("Stats & Settings") },
                    icon = {
                        Text(
                            text = "📊",
                            fontSize = 20.sp
                        )
                    },
                    modifier = Modifier.testTag("nav_stats")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Notification permission banner if missing on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permission Required",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Allow notifications to receive hydration reminders.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Enable", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Screen content switcher
            when (currentTab) {
                AppTab.TRACKER -> TrackerScreen(
                    todayGlasses = todayGlasses,
                    dailyGoal = dailyGoal,
                    onAdd = { viewModel.addGlass() },
                    onRemove = { viewModel.removeGlass() }
                )
                AppTab.STATS_SETTINGS -> StatsSettingsScreen(
                    todayGlasses = todayGlasses,
                    dailyGoal = dailyGoal,
                    history = history,
                    reminderInterval = reminderInterval,
                    remindersEnabled = remindersEnabled,
                    onGoalChange = { viewModel.updateDailyGoal(it) },
                    onIntervalChange = { viewModel.updateReminderInterval(it) },
                    onRemindersToggle = { viewModel.toggleReminders(it) }
                )
            }
        }
    }
}

@Composable
fun TrackerScreen(
    todayGlasses: Int,
    dailyGoal: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hydration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Today's Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Beautiful Animated Progress Indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    // Background Ring track
                    CircularProgressIndicator(
                        progress = { 1f },
                        strokeWidth = 14.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxSize()
                    )

                    val animatedProgress by animateFloatAsState(
                        targetValue = if (dailyGoal > 0) todayGlasses.toFloat() / dailyGoal else 0f,
                        animationSpec = tween(durationMillis = 800),
                        label = "tracker_progress_anim"
                    )

                    // Foreground track
                    CircularProgressIndicator(
                        progress = { animatedProgress.coerceIn(0f, 1f) },
                        strokeWidth = 14.dp,
                        color = MaterialTheme.colorScheme.primary,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Dynamic text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val percent = if (dailyGoal > 0) ((todayGlasses.toFloat() / dailyGoal) * 100).toInt() else 0
                        Text(
                            text = "$percent%",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$todayGlasses / $dailyGoal glasses",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Completion badge
                if (todayGlasses >= dailyGoal) {
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily Goal Reached! 🎉",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Action controls (+ / - buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subtract button (-)
            OutlinedButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(68.dp)
                    .testTag("btn_remove"),
                shape = CircleShape,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "—",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Big Water Add Button (+)
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .size(86.dp)
                    .testTag("btn_add"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add one glass",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid-like Glass Visualizer
        Text(
            text = "Your Glass Board",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val columns = 4
            val rows = (dailyGoal + columns - 1) / columns
            
            Column(
                modifier = Modifier.width(260.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (c in 0 until columns) {
                            val glassIndex = r * columns + c
                            if (glassIndex < dailyGoal) {
                                val isWaterDrank = glassIndex < todayGlasses
                                val emoji = if (isWaterDrank) "🥤" else "🥛"
                                
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (isWaterDrank) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 24.sp,
                                        modifier = Modifier.animateContentSize(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsSettingsScreen(
    todayGlasses: Int,
    dailyGoal: Int,
    history: List<HistoryEntry>,
    reminderInterval: Int,
    remindersEnabled: Boolean,
    onGoalChange: (Int) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onRemindersToggle: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Today's Intake Stat Card
        Text(
            text = "Statistics Dashboard",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Today's Hydration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$todayGlasses glasses", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Goal Completion", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    val pct = if (dailyGoal > 0) ((todayGlasses.toFloat() / dailyGoal) * 100).toInt() else 0
                    Text("$pct%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Interactive weekly hydration trends chart
        WeeklyHydrationTrendsChart(
            history = history,
            dailyGoal = dailyGoal
        )

        // Customization Settings Card
        Text(
            text = "Preferences",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Goal customiser
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎯",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daily Drinking Goal", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "$dailyGoal glasses",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Preset Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(6, 8, 10, 12).forEach { option ->
                            val isSelected = dailyGoal == option
                            InputChip(
                                selected = isSelected,
                                onClick = { onGoalChange(option) },
                                label = { Text("$option glasses") },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Reminder Activation Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (remindersEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hydration Reminders", fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = onRemindersToggle,
                        modifier = Modifier.testTag("switch_reminders")
                    )
                }

                // If triggers are enabled, show interval timing selector
                if (remindersEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reminder Interval", fontWeight = FontWeight.SemiBold)
                            }
                            
                            val intervalText = when (reminderInterval) {
                                30 -> "30 min"
                                60 -> "1 hour"
                                120 -> "2 hours"
                                180 -> "3 hours"
                                else -> "$reminderInterval min"
                            }
                            Text(
                                intervalText,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Presets Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val intervalOptions = listOf(
                                Pair(30, "30m"),
                                Pair(60, "1h"),
                                Pair(120, "2h"),
                                Pair(180, "3h")
                            )
                            intervalOptions.forEach { (mins, label) ->
                                val isSelected = reminderInterval == mins
                                InputChip(
                                    selected = isSelected,
                                    onClick = { onIntervalChange(mins) },
                                    label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyHydrationTrendsChart(
    history: List<HistoryEntry>,
    dailyGoal: Int
) {
    if (history.isEmpty()) return

    // Track which bar is currently selected. Default to today (last index)
    var selectedIndex by remember(history) { mutableStateOf(history.size - 1) }
    val selectedEntry = history.getOrNull(selectedIndex) ?: history.last()

    val maxGlassesInHistory = history.maxOfOrNull { it.glasses } ?: dailyGoal
    val maxScale = maxOf(maxGlassesInHistory, dailyGoal, 1)

    // Calculate Weekly stats
    val totalVolume = history.sumOf { it.glasses }
    val averageIntake = if (history.isNotEmpty()) totalVolume.toFloat() / history.size else 0f
    val daysGoalMet = history.count { it.glasses >= dailyGoal }

    // Visual Card Container
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Hydration Trends",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap bars to inspect details",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                // Quick Summary badge
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Met: $daysGoalMet/7 days",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Interactive Bar Graph area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Left axis labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("$maxScale🥛", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    
                    // Goal text marker
                    if (dailyGoal < maxScale && dailyGoal > 0) {
                        Text("Goal ($dailyGoal)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    
                    Text("0🥛", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }

                // Chart Bars Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // gridlines and daily goal threshold
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val height = size.height
                        val width = size.width
                        val gridPaint = Color(0x1F00B0FF)
                        val dashPath = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

                        // 3 light gridlines
                        val intervals = listOf(0.25f, 0.5f, 0.75f)
                        intervals.forEach { fraction ->
                            val y = height * (1f - fraction)
                            drawLine(
                                color = gridPaint.copy(alpha = 0.05f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Special visible dashed goal line
                        val goalPercentage = dailyGoal.toFloat() / maxScale
                        val goalY = height * (1f - goalPercentage.coerceIn(0f, 1f))
                        drawLine(
                            color = Color(0xFF00B0FF).copy(alpha = 0.4f),
                            start = Offset(0f, goalY),
                            end = Offset(width, goalY),
                            pathEffect = dashPath,
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Foreground columns containing actual clickable bars
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        history.forEachIndexed { index, entry ->
                            val scalePercentage = entry.glasses.toFloat() / maxScale
                            val boundedPercentage = scalePercentage.coerceIn(0f, 1f)
                            val isSelected = selectedIndex == index

                            // Trigger spring-like scaling when loading heights
                            val animatedHeightFraction by animateFloatAsState(
                                targetValue = boundedPercentage,
                                animationSpec = tween(
                                    durationMillis = 700,
                                    delayMillis = index * 40
                                ),
                                label = "bar_height_anim_$index"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedIndex = index },
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Glasses number indicator
                                Box(
                                    modifier = Modifier.height(18.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Text(
                                        text = "${entry.glasses}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else if (entry.glasses >= dailyGoal) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        }
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))

                                // Capsule Bar Container
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(115.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    if (entry.glasses > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(animatedHeightFraction)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = if (entry.glasses >= dailyGoal) {
                                                            listOf(
                                                                MaterialTheme.colorScheme.secondary,
                                                                MaterialTheme.colorScheme.primary
                                                            )
                                                        } else {
                                                            listOf(
                                                                MaterialTheme.colorScheme.tertiary,
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    )
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = entry.dayOfWeek,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Interactive Day-Detail Display Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedEntry.dateLabel} (${selectedEntry.dayOfWeek})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Achieved status badge
                        val progressPercent = if (dailyGoal > 0) ((selectedEntry.glasses.toFloat() / dailyGoal) * 100).toInt() else 0
                        val isGoalMet = selectedEntry.glasses >= dailyGoal
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isGoalMet) Color(0xFF4CAF50).copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                    CircleShape
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isGoalMet) "Goal Reached! 🎉" else "$progressPercent%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGoalMet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quick informative emoji icon
                        Text(
                            text = if (selectedEntry.glasses >= dailyGoal) "🏆" else if (selectedEntry.glasses > 0) "🥤" else "💤",
                            fontSize = 24.sp
                        )
                        
                        Column {
                            val glassesDifference = dailyGoal - selectedEntry.glasses
                            val summaryMessage = if (selectedEntry.glasses >= dailyGoal) {
                                "Excellent hydration! You met and exceeded your daily goal. Outstanding job! 🙌"
                            } else if (selectedEntry.glasses > 0) {
                                "You drank ${selectedEntry.glasses} glasses. Just $glassesDifference more to reach your goal! You got this! 💪"
                            } else {
                                "No water tracked on this day. Remember to log your intake! 💧"
                            }

                            Text(
                                text = "$summaryMessage",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Historical metrics panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Average card info
                val formattedAvg = (averageIntake * 10).toInt() / 10.0
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Weekly Average",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$formattedAvg glasses",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Total card info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Consumed",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalVolume glasses",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
