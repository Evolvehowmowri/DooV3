package com.example.ui

import android.os.CountDownTimer
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import com.example.data.DoNothingLog
import com.example.data.DoNothingData
import com.example.data.MeasurementTracker
import com.example.data.MeasurementLog
import com.example.data.AffirmationCategory
import com.example.data.AffirmationItem
import com.example.data.AffirmationLog
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import androidx.compose.foundation.Canvas
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.util.Locale

val OrangeAccent = Color(0xFFFF9800)
val Purple40 = Color(0xFF6650a4)

// Tools Navigation State
enum class ToolView {
    Dashboard, AlgoCare, Breathing, Pomodoro, DoNothing, MeasurementTracker, Affirmation, MomentumMaker
}

// AlgoCare Steps definition
enum class AlgoStep {
    Intro,
    WaterCheck, WaterAction,
    FoodCheck, FoodEnergyCheck, FoodMealPrepAction, FoodEasySnackAction,
    MedCheck, MedAction,
    PainCheck, PainAction,
    TempCheck, TempAction,
    HygieneCheck, HygieneEnergyCheck, ShowerAction, SmallHygieneAction,
    ClothesCheck, ClothesAction,
    SleepCheck, BedtimeCheck, SleepAction, NapAction,
    EnvironmentCheck, EnvironmentAction,
    MovementCheck, MovementAction,
    SocialCheck, SocialAction,
    PostureCheck, PostureAction,
    BrainStateCheck, BrainDumpAction,
    EmotionalCheck, EmotionalAction,
    Conclusion
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: DoViewModel,
    modifier: Modifier = Modifier
) {
    var activeTool by remember { mutableStateOf(ToolView.Dashboard) }

    Crossfade(
        targetState = activeTool,
        animationSpec = navFadeSpec(),
        label = "ToolsNavigation"
    ) { tool ->
        when (tool) {
            ToolView.Dashboard -> ToolsDashboard(viewModel = viewModel, onSelectTool = { activeTool = it })
            ToolView.AlgoCare -> AlgoCareScreen(viewModel = viewModel, onBack = { activeTool = ToolView.Dashboard })
            ToolView.Breathing -> BreathingBuddyScreen(onBack = { activeTool = ToolView.Dashboard })
            ToolView.Pomodoro -> PomodoroScreen(onBack = { activeTool = ToolView.Dashboard })
            ToolView.DoNothing -> DoNothingScreen(viewModel = viewModel, onBack = { activeTool = ToolView.Dashboard })
            ToolView.MeasurementTracker -> MeasurementTrackerScreen(viewModel = viewModel, onBack = { activeTool = ToolView.Dashboard })
            ToolView.Affirmation -> AffirmationCounterScreen(viewModel = viewModel, onBack = { activeTool = ToolView.Dashboard })
            ToolView.MomentumMaker -> MomentumMakerScreen(viewModel = viewModel, onBack = { activeTool = ToolView.Dashboard })
        }
    }
}

// Custom simple navigation animation spec
fun <T> navFadeSpec() = androidx.compose.animation.core.tween<T>(durationMillis = 300)

@Composable
fun ToolsDashboard(
    viewModel: DoViewModel,
    onSelectTool: (ToolView) -> Unit
) {
    val pendingNotifications by viewModel.pendingNotifications.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Self-Care & Mind Space",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Text(
            text = "A collection of interactive utilities to help you navigate executive dysfunction, stress, and busy days.",
            color = DarkTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (pendingNotifications.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFFEF5350).copy(alpha = 0.6f), LavenderAccent.copy(alpha = 0.3f)))),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                    )
                                    Text(
                                        text = "Mind Space Alerts (${pendingNotifications.size})",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                
                                Text(
                                    text = "Reset States",
                                    color = LavenderAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { viewModel.resetNotificationStates() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                pendingNotifications.forEach { item ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = DarkSurface
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                val icon = when (item.type) {
                                                    "MEASUREMENT" -> Icons.Default.TrendingUp
                                                    "AFFIRMATION" -> Icons.Default.AutoAwesome
                                                    "DO_NOTHING" -> Icons.Default.Spa
                                                    else -> Icons.Default.Notifications
                                                }
                                                val iconColor = when (item.type) {
                                                    "MEASUREMENT" -> Color(0xFF64B5F6)
                                                    "AFFIRMATION" -> Color(0xFFFFB74D)
                                                    "DO_NOTHING" -> Color(0xFF81C784)
                                                    else -> LavenderAccent
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(iconColor.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = item.type,
                                                        tint = iconColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = item.description,
                                                        color = DarkTextSecondary,
                                                        fontSize = 11.sp,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val dest = when (item.targetScreen) {
                                                            "MEASUREMENT_TRACKER" -> ToolView.MeasurementTracker
                                                            "AFFIRMATION" -> ToolView.Affirmation
                                                            "DO_NOTHING" -> ToolView.DoNothing
                                                            else -> ToolView.Dashboard
                                                        }
                                                        if (dest != ToolView.Dashboard) {
                                                            onSelectTool(dest)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = LavenderAccent,
                                                        contentColor = DeepPurpleContainer
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier
                                                        .weight(1.2f)
                                                        .height(36.dp)
                                                        .testTag("action_notif_start_${item.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Start",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = when (item.type) {
                                                            "MEASUREMENT" -> "Log"
                                                            "AFFIRMATION" -> "Recite"
                                                            "DO_NOTHING" -> "Zen"
                                                            else -> "Start"
                                                        },
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                OutlinedButton(
                                                    onClick = { viewModel.snoozeNotification(item.id, 2) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, GlassBorder),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(36.dp)
                                                        .testTag("action_notif_snooze_${item.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccessTime,
                                                        contentDescription = "Snooze",
                                                        modifier = Modifier.size(13.dp),
                                                        tint = DarkTextSecondary
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Snooze 2h", fontSize = 10.sp, color = DarkTextSecondary)
                                                }

                                                OutlinedButton(
                                                    onClick = { viewModel.ignoreNotification(item.id) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, GlassBorder),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(36.dp)
                                                        .testTag("action_notif_ignore_${item.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VisibilityOff,
                                                        contentDescription = "Ignore",
                                                        modifier = Modifier.size(13.dp),
                                                        tint = DarkTextSecondary
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Ignore Today", fontSize = 10.sp, color = DarkTextSecondary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Your mind space is completely clear of alerts today.",
                                color = DarkTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            item {
                ToolCard(
                    title = "AlgoCare Guide",
                    description = "An interactive, branching step-by-step decision tree that helps you identify physical & emotional blocks. Based on 'You Feel Like Shit' algorithm.",
                    icon = Icons.Default.Favorite,
                    iconColor = HighPriorityRed,
                    badgeText = "Complete Flow",
                    onClick = { onSelectTool(ToolView.AlgoCare) }
                )
            }

            item {
                ToolCard(
                    title = "Breathing Buddy",
                    description = "Restore mental clarity and activate your parasympathetic nervous system with responsive visual and timed Box Breathing cues.",
                    icon = Icons.Default.Air,
                    iconColor = LightBlueAccent,
                    badgeText = "Box Breathing",
                    onClick = { onSelectTool(ToolView.Breathing) }
                )
            }

            item {
                ToolCard(
                    title = "Minimalist Pomodoro",
                    description = "Stay focused without cognitive clutter. Toggle easily between work sessions and deep breaks to clear your head.",
                    icon = Icons.Default.Timer,
                    iconColor = LavenderAccent,
                    badgeText = "Focus",
                    onClick = { onSelectTool(ToolView.Pomodoro) }
                )
            }

            item {
                ToolCard(
                    title = "Do Nothing (Wu wei)",
                    description = "Enter a state of absolute quiet and non-doing. Practice Effortless Being with gentle, randomized contemplative thoughts. Triple-tap anywhere to exit.",
                    icon = Icons.Default.Spa,
                    iconColor = Color(0xFF81C784),
                    badgeText = "Effortless Being",
                    onClick = { onSelectTool(ToolView.DoNothing) }
                )
            }

            item {
                ToolCard(
                    title = "Measurement Tracker",
                    description = "Track and log your weight, BMI, body measurements, or custom indicators with support for custom targets, milestones, progress charts, and data exports.",
                    icon = Icons.Default.TrendingUp,
                    iconColor = Color(0xFF64B5F6),
                    badgeText = "Progress",
                    onClick = { onSelectTool(ToolView.MeasurementTracker) }
                )
            }

            item {
                ToolCard(
                    title = "Affirmation Counter",
                    description = "Focus on positive mindset shifts. Count repetitions by tapping the screen, swipe left or right to change sentences, load batch lists, and export progress logs.",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = Color(0xFFFFB74D),
                    badgeText = "Mindset",
                    onClick = { onSelectTool(ToolView.Affirmation) }
                )
            }

            item {
                ToolCard(
                    title = "Momentum Maker ⚡",
                    description = "Get out of a rut instantly. Receive a random small, bite-sized thing to do based on your category and time filters to kickstart your momentum.",
                    icon = Icons.Default.Bolt,
                    iconColor = BrightAmber,
                    badgeText = "Get Unstuck",
                    onClick = { onSelectTool(ToolView.MomentumMaker) }
                )
            }
        }
    }
}

@Composable
fun ToolCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LavenderAccent) },
                    border = BorderStroke(1.dp, GlassBorder),
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.Transparent)
                )
            }

            Text(
                text = description,
                color = DarkTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Launch", color = LavenderAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ---------------- ALGO CARE SCREEN ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgoCareScreen(
    viewModel: DoViewModel,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableStateOf(AlgoStep.Intro) }
    val stepHistory = remember { mutableStateListOf<AlgoStep>() }
    
    // Tracks results of each self-care check to display a report at the end
    val checkedStatus = remember { mutableStateMapOf<String, String>() }

    fun navigateTo(next: AlgoStep) {
        stepHistory.add(currentStep)
        currentStep = next
    }

    fun navigateBack() {
        if (stepHistory.isNotEmpty()) {
            currentStep = stepHistory.removeLast()
        } else {
            onBack()
        }
    }

    val progress = remember(currentStep) {
        val stepsList = AlgoStep.values()
        val index = stepsList.indexOf(currentStep)
        (index.toFloat() / (stepsList.size - 1)).coerceIn(0f, 1f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AlgoCare Self-Care", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Progress Bar
            if (currentStep != AlgoStep.Intro && currentStep != AlgoStep.Conclusion) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        color = LavenderAccent,
                        trackColor = GlassBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step ${AlgoStep.values().indexOf(currentStep)} of ${AlgoStep.values().size - 2}",
                            color = DarkTextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Checkup Progress",
                            color = DarkTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Main Active Card Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = currentStep, label = "AlgoStepAnimation") { step ->
                    AlgoStepContent(
                        step = step,
                        viewModel = viewModel,
                        checkedStatus = checkedStatus,
                        onChoice = { nextStep -> navigateTo(nextStep) },
                        onReset = {
                            stepHistory.clear()
                            checkedStatus.clear()
                            currentStep = AlgoStep.Intro
                        },
                        onFinish = onBack
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgoStepContent(
    step: AlgoStep,
    viewModel: DoViewModel,
    checkedStatus: MutableMap<String, String>,
    onChoice: (AlgoStep) -> Unit,
    onReset: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                AlgoStep.Intro -> {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = HighPriorityRed, modifier = Modifier.size(54.dp))
                    Text(
                        text = "Interactive Self-Care Guide",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Struggling with physical discomfort, fatigue, or executive dysfunction? This guide is a complete implementation of the famous self-care algorithm to walk you through basic biological and emotional checkups.",
                        color = DarkTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = { onChoice(AlgoStep.WaterCheck) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Begin Checkup", fontWeight = FontWeight.Bold)
                    }
                }

                // 1. WATER
                AlgoStep.WaterCheck -> {
                    StepTitle("1. Hydration Check", Icons.Default.LocalActivity, LightBlueAccent)
                    StepQuestion("Have you drank a glass of water in the last hour?")
                    ActionButton("Yes, I am hydrated", LightBlueAccent) { 
                        checkedStatus["Hydration"] = "Already Hydrated 💧"
                        onChoice(AlgoStep.FoodCheck) 
                    }
                    ActionButton("No, I haven't / I don't remember", DarkSurfaceVariant) { onChoice(AlgoStep.WaterAction) }
                }
                AlgoStep.WaterAction -> {
                    StepTitle("Hydration Break", Icons.Default.LocalActivity, LightBlueAccent)
                    StepInstruction("Go drink a glass of water right now. Go on, it takes less than a minute. Sip it slowly and let the refresh hit you.")
                    
                    // Interactive visual water game
                    var sipsTaken by remember(step) { mutableStateOf(0) }
                    
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 110.dp)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Water fill
                        val animatedHeight = animateFloatAsState(targetValue = sipsTaken * 30f, label = "WaterHeight")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(animatedHeight.value.dp)
                                .background(LightBlueAccent.copy(alpha = 0.8f))
                        )
                    }
                    
                    Text(
                        text = "Take 3 mindful sips of water!",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = if (index < sipsTaken) LightBlueAccent else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (sipsTaken < 3) {
                        Button(
                            onClick = { sipsTaken++ },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LightBlueAccent, contentColor = Color.Black)
                        ) {
                            Text("Take a Sip 💧", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        ActionButton("I drank a glass of water!", LightBlueAccent) { 
                            checkedStatus["Hydration"] = "Sipped & Hydrated 💧"
                            onChoice(AlgoStep.FoodCheck) 
                        }
                    }
                }

                // 2. FOOD
                AlgoStep.FoodCheck -> {
                    StepTitle("2. Nourishment", Icons.Default.Restaurant, OrangeAccent)
                    StepQuestion("Have you eaten in the past 4 hours?")
                    ActionButton("Yes, I have eaten recently", OrangeAccent) { 
                        checkedStatus["Nourishment"] = "Already Nourished 🍎"
                        onChoice(AlgoStep.MedCheck) 
                    }
                    ActionButton("No, I haven't eaten in 4+ hours", DarkSurfaceVariant) { onChoice(AlgoStep.FoodEnergyCheck) }
                }
                AlgoStep.FoodEnergyCheck -> {
                    StepTitle("Energy Check", Icons.Default.Restaurant, OrangeAccent)
                    StepQuestion("Do you have the energy or mental space to prepare a simple meal/snack?")
                    ActionButton("Yes, I can make something", OrangeAccent) { onChoice(AlgoStep.FoodMealPrepAction) }
                    ActionButton("No, preparing food feels exhausting", DarkSurfaceVariant) { onChoice(AlgoStep.FoodEasySnackAction) }
                }
                AlgoStep.FoodMealPrepAction -> {
                    StepTitle("Prepare Meal", Icons.Default.Restaurant, OrangeAccent)
                    StepInstruction("Excellent! Go prepare a simple meal: Toast, cereal, a sandwich, or fruit. Sit down and eat it without staring at a screen. Take your time.")
                    ActionButton("I have eaten!", OrangeAccent) { 
                        checkedStatus["Nourishment"] = "Prepared a Meal 🍳"
                        onChoice(AlgoStep.MedCheck) 
                    }
                }
                AlgoStep.FoodEasySnackAction -> {
                    StepTitle("Low Energy Snack", Icons.Default.Restaurant, OrangeAccent)
                    StepInstruction("That is totally okay. Can you manage an ultra-easy snack that requires zero preparation? A handful of nuts, a spoonful of peanut butter, some fruit, crackers, or a juice box. Any fuel is better than zero fuel!")
                    ActionButton("I grabbed a quick bite!", OrangeAccent) { 
                        checkedStatus["Nourishment"] = "Quick Snack Eaten 🍌"
                        onChoice(AlgoStep.MedCheck) 
                    }
                }

                // 3. MEDICATION
                AlgoStep.MedCheck -> {
                    StepTitle("3. Medication", Icons.Default.Medication, HighPriorityRed)
                    StepQuestion("Do you take any daily medications or required supplements?")
                    ActionButton("No, I don't take any", DarkSurfaceVariant) { 
                        checkedStatus["Medication"] = "No Meds Required 💊"
                        onChoice(AlgoStep.PainCheck) 
                    }
                    ActionButton("Yes, and I already took them today", HighPriorityRed) { 
                        checkedStatus["Medication"] = "Already Took Meds 💊"
                        onChoice(AlgoStep.PainCheck) 
                    }
                    ActionButton("Yes, but I forgot or haven't yet", HighPriorityRed) { onChoice(AlgoStep.MedAction) }
                }
                AlgoStep.MedAction -> {
                    StepTitle("Take Meds", Icons.Default.Medication, HighPriorityRed)
                    StepInstruction("Please go take your prescribed medication or supplements now, assuming it is safe to do so. Setting a phone reminder for the same time daily can help you build this habit.")
                    ActionButton("I have taken them!", HighPriorityRed) { 
                        checkedStatus["Medication"] = "Meds Taken Now! 💊"
                        onChoice(AlgoStep.PainCheck) 
                    }
                }

                // 4. PHYSICAL PAIN (NEW!)
                AlgoStep.PainCheck -> {
                    StepTitle("4. Physical Comfort", Icons.Default.Healing, OrangeAccent)
                    StepQuestion("Are you feeling physical pain (headache, muscle ache, back pain, cramp)?")
                    ActionButton("No, my body feels fine", OrangeAccent) {
                        checkedStatus["Comfort"] = "No Body Pain 🩹"
                        onChoice(AlgoStep.TempCheck)
                    }
                    ActionButton("Yes, I am in physical discomfort", DarkSurfaceVariant) { onChoice(AlgoStep.PainAction) }
                }
                AlgoStep.PainAction -> {
                    StepTitle("Address Physical Pain", Icons.Default.Healing, OrangeAccent)
                    StepInstruction("Take standard pain relief if safe & recommended. Apply a warm heat pad or an ice compress to the sore area. Lie down in a comfortable posture. Be gentle with your body right now.")
                    ActionButton("Pain relief/care initiated!", OrangeAccent) {
                        checkedStatus["Comfort"] = "Pain Care Initiated 🩹"
                        onChoice(AlgoStep.TempCheck)
                    }
                }

                // 5. TEMPERATURE (NEW!)
                AlgoStep.TempCheck -> {
                    StepTitle("5. Temperature Comfort", Icons.Default.Thermostat, LightBlueAccent)
                    StepQuestion("Are you currently shivering, too cold, or uncomfortably hot?")
                    ActionButton("My temperature is fine", LightBlueAccent) {
                        checkedStatus["Temperature"] = "Temperature Perfect 🌡️"
                        onChoice(AlgoStep.HygieneCheck)
                    }
                    ActionButton("Yes, I am too hot / too cold", DarkSurfaceVariant) { onChoice(AlgoStep.TempAction) }
                }
                AlgoStep.TempAction -> {
                    StepTitle("Adjust Temperature", Icons.Default.Thermostat, LightBlueAccent)
                    StepInstruction("If too cold: Put on cozy socks, a hoodie, grab a blanket, or make a warm cup of herbal tea.\nIf too hot: Open a window, turn on a fan, tie your hair up, or grab a glass of ice-cold water.")
                    ActionButton("Temperature adjusted!", LightBlueAccent) {
                        checkedStatus["Temperature"] = "Comfort Adjusted 🌡️"
                        onChoice(AlgoStep.HygieneCheck)
                    }
                }

                // 6. HYGIENE
                AlgoStep.HygieneCheck -> {
                    StepTitle("6. Personal Hygiene", Icons.Default.Bathtub, LightBlueAccent)
                    StepQuestion("Have you bathed, washed your face, or brushed your teeth in the last 24 hours?")
                    ActionButton("Yes, I feel fresh and clean", LightBlueAccent) { 
                        checkedStatus["Hygiene"] = "Fresh & Clean Shower Shower🚿"
                        onChoice(AlgoStep.ClothesCheck) 
                    }
                    ActionButton("No, I haven't done that", DarkSurfaceVariant) { onChoice(AlgoStep.HygieneEnergyCheck) }
                }
                AlgoStep.HygieneEnergyCheck -> {
                    StepTitle("Hygiene Check", Icons.Default.Bathtub, LightBlueAccent)
                    StepQuestion("Do you have the energy to take a full shower or bath right now?")
                    ActionButton("Yes, a warm shower sounds nice", LightBlueAccent) { onChoice(AlgoStep.ShowerAction) }
                    ActionButton("No, a full shower is too much effort", DarkSurfaceVariant) { onChoice(AlgoStep.SmallHygieneAction) }
                }
                AlgoStep.ShowerAction -> {
                    StepTitle("Take a Shower", Icons.Default.Bathtub, LightBlueAccent)
                    StepInstruction("Perfect. Go enjoy a warm shower. Feel the water wash away tension. It is a wonderful physical reset. Put on fresh clothes afterwards.")
                    ActionButton("I feel refreshed now!", LightBlueAccent) { 
                        checkedStatus["Hygiene"] = "Enjoyed Warm Shower 🚿"
                        onChoice(AlgoStep.ClothesCheck) 
                    }
                }
                AlgoStep.SmallHygieneAction -> {
                    StepTitle("Micro-Hygiene", Icons.Default.Bathtub, LightBlueAccent)
                    StepInstruction("Understood. Let's do a micro-hygiene step instead: Go wash your face with cool water, rinse your mouth with water or mouthwash, or wipe your hands and neck with a damp cloth. It takes 15 seconds.")
                    ActionButton("I did a quick refresh!", LightBlueAccent) { 
                        checkedStatus["Hygiene"] = "Completed Micro-Hygiene 🫧"
                        onChoice(AlgoStep.ClothesCheck) 
                    }
                }

                // 7. CLOTHES
                AlgoStep.ClothesCheck -> {
                    StepTitle("7. Comfortable Clothing", Icons.Default.Checkroom, OrangeAccent)
                    StepQuestion("Are you dressed in clean, comfortable clothes that suit the temperature?")
                    ActionButton("Yes, I'm dressed and comfortable", OrangeAccent) { 
                        checkedStatus["Clothing"] = "Dressed Cozy 👕"
                        onChoice(AlgoStep.SleepCheck) 
                    }
                    ActionButton("No, my clothes are worn/dirty/uncomfortable", DarkSurfaceVariant) { onChoice(AlgoStep.ClothesAction) }
                }
                AlgoStep.ClothesAction -> {
                    StepTitle("Change Clothes", Icons.Default.Checkroom, OrangeAccent)
                    StepInstruction("Take off those worn clothes or pajamas. Go put on a clean shirt, fresh underwear, and comfortable sweatpants or loose clothes. If you're cold, put on warm socks/hoodie; if too warm, switch to lighter clothes.")
                    ActionButton("I've changed clothes!", OrangeAccent) { 
                        checkedStatus["Clothing"] = "Switched to Cozy Clothes 👕"
                        onChoice(AlgoStep.SleepCheck) 
                    }
                }

                // 8. SLEEP
                AlgoStep.SleepCheck -> {
                    StepTitle("8. Sleep & Fatigue", Icons.Default.Bedtime, Purple40)
                    StepQuestion("Did you sleep well last night? Or have you been awake for more than 18 hours?")
                    ActionButton("I slept fine and feel rested", Purple40) { 
                        checkedStatus["Sleep & Fatigue"] = "Feeling Well-Rested 🛌"
                        onChoice(AlgoStep.EnvironmentCheck) 
                    }
                    ActionButton("No, I'm completely exhausted/sleep-deprived", DarkSurfaceVariant) { onChoice(AlgoStep.BedtimeCheck) }
                }
                AlgoStep.BedtimeCheck -> {
                    StepTitle("Bedtime Check", Icons.Default.Bedtime, Purple40)
                    StepQuestion("Is it currently a reasonable time to go to sleep?")
                    ActionButton("Yes, it's night/bedtime", Purple40) { onChoice(AlgoStep.SleepAction) }
                    ActionButton("No, it's the middle of the day", DarkSurfaceVariant) { onChoice(AlgoStep.NapAction) }
                }
                AlgoStep.SleepAction -> {
                    StepTitle("Go to Bed", Icons.Default.Bedtime, Purple40)
                    StepInstruction("Close this app, put your phone on charge away from your bed, turn off the lights, and let yourself fall asleep. Your body needs rest more than anything else right now.")
                    ActionButton("I will go to sleep now", Purple40) { 
                        checkedStatus["Sleep & Fatigue"] = "Going to Sleep 🛌"
                        onChoice(AlgoStep.EnvironmentCheck) 
                    }
                }
                AlgoStep.NapAction -> {
                    StepTitle("Take a Nap", Icons.Default.Bedtime, Purple40)
                    StepInstruction("Set an alarm for 20 minutes from now (so you don't fall into deep sleep and wake up groggy). Lie down, close your eyes, breathe, and rest. If you can't sleep, just resting your eyes helps.")
                    ActionButton("I finished resting!", Purple40) { 
                        checkedStatus["Sleep & Fatigue"] = "Took 20-min Nap 🛌"
                        onChoice(AlgoStep.EnvironmentCheck) 
                    }
                }

                // 9. ENVIRONMENT
                AlgoStep.EnvironmentCheck -> {
                    StepTitle("9. Environment", Icons.Default.Home, LightBlueAccent)
                    StepQuestion("Have you been cooped up in a single dark, messy, or cluttered room for hours?")
                    ActionButton("No, my space is fine", LightBlueAccent) { 
                        checkedStatus["Environment"] = "Space was Clean & Bright 🌿"
                        onChoice(AlgoStep.MovementCheck) 
                    }
                    ActionButton("Yes, I feel stuck or messy", DarkSurfaceVariant) { onChoice(AlgoStep.EnvironmentAction) }
                }
                AlgoStep.EnvironmentAction -> {
                    StepTitle("Shift Environment", Icons.Default.Home, LightBlueAccent)
                    StepInstruction("Let's bring in a small shift: Open the blinds or curtains to let daylight in. Open a window for 1 minute to get fresh air. If your room is cluttered, pick up just 3 items and put them away. Or walk to a different room.")
                    ActionButton("Space shifted!", LightBlueAccent) { 
                        checkedStatus["Environment"] = "Shaped & Brightened Space 🌿"
                        onChoice(AlgoStep.MovementCheck) 
                    }
                }

                // 10. MOVEMENT
                AlgoStep.MovementCheck -> {
                    StepTitle("10. Movement Check", Icons.Default.DirectionsRun, OrangeAccent)
                    StepQuestion("Have you stretched or moved your body in the last few hours?")
                    ActionButton("Yes, I've moved around", OrangeAccent) { 
                        checkedStatus["Movement"] = "Already Active Today 🏃"
                        onChoice(AlgoStep.SocialCheck) 
                    }
                    ActionButton("No, I've been completely stationary", DarkSurfaceVariant) { onChoice(AlgoStep.MovementAction) }
                }
                AlgoStep.MovementAction -> {
                    StepTitle("Quick Stretch Routine", Icons.Default.DirectionsRun, OrangeAccent)
                    StepInstruction("Stand up and perform these 3 gentle physical stretches right now:")
                    
                    // Interactive checkboxes for stretch routine
                    val stretch1 = remember(step) { mutableStateOf(false) }
                    val stretch2 = remember(step) { mutableStateOf(false) }
                    val stretch3 = remember(step) { mutableStateOf(false) }
                    
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = stretch1.value,
                                onCheckedChange = { stretch1.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = OrangeAccent, uncheckedColor = Color.White.copy(alpha = 0.4f))
                            )
                            Text("Neck rolls (Drop ear to shoulder, roll slowly) 🧘", color = Color.White, fontSize = 12.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = stretch2.value,
                                onCheckedChange = { stretch2.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = OrangeAccent, uncheckedColor = Color.White.copy(alpha = 0.4f))
                            )
                            Text("Shoulder rolls (Roll backward 10 times) 🔄", color = Color.White, fontSize = 12.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = stretch3.value,
                                onCheckedChange = { stretch3.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = OrangeAccent, uncheckedColor = Color.White.copy(alpha = 0.4f))
                            )
                            Text("Sky Reach (Reach high, take slow breath in) ☁️", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    if (stretch1.value && stretch2.value && stretch3.value) {
                        ActionButton("I finished my stretches!", OrangeAccent) { 
                            checkedStatus["Movement"] = "Stretched Body & Neck 🏃"
                            onChoice(AlgoStep.SocialCheck) 
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Check off stretches to proceed", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 11. SOCIAL
                AlgoStep.SocialCheck -> {
                    StepTitle("11. Connection", Icons.Default.Group, LavenderAccent)
                    StepQuestion("Have you interacted with another human being today?")
                    ActionButton("Yes, I've had social connection", LavenderAccent) { 
                        checkedStatus["Connection"] = "Already Connected 👥"
                        onChoice(AlgoStep.PostureCheck) 
                    }
                    ActionButton("No, I've been entirely isolated", DarkSurfaceVariant) { onChoice(AlgoStep.SocialAction) }
                }
                AlgoStep.SocialAction -> {
                    StepTitle("Reach Out (Copier)", Icons.Default.Group, LavenderAccent)
                    StepInstruction("Tapping any message card below instantly copies it to your clipboard. Send it to a friend, family member, or trusted contact.")
                    
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    var copiedText by remember(step) { mutableStateOf<String?>(null) }
                    
                    val templates = listOf(
                        "Hey! Just wanted to say hello and see how you are doing today. 😊" to "Casual Check-in",
                        "Hey, feeling a little low energy today, but wanted to text and say hi. Hope your week is going well!" to "Low-Energy Wave",
                        "Hey, saw this funny thing and thought of you! Hope you're having a good day." to "Meme / Simple Hello"
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        templates.forEach { (text, label) ->
                            val isSelected = copiedText == text
                            Card(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                                    copiedText = text
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) LavenderAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isSelected) LavenderAccent else GlassBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, color = LavenderAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(text, color = Color.White, fontSize = 11.sp, lineHeight = 14.sp)
                                    }
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = if (isSelected) LavenderAccent else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp).padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (copiedText != null) {
                        Text("Copied successfully!", color = LavenderAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    ActionButton("I reached out / sent a message!", LavenderAccent) { 
                        checkedStatus["Connection"] = "Reached out with Note 👥"
                        onChoice(AlgoStep.PostureCheck) 
                    }
                }

                // 12. POSTURE
                AlgoStep.PostureCheck -> {
                    StepTitle("12. Posture Check", Icons.Default.SelfImprovement, LightBlueAccent)
                    StepQuestion("Are you currently hunched over, slouching, or holding tension?")
                    ActionButton("My posture is good!", LightBlueAccent) { 
                        checkedStatus["Posture"] = "Posture Checked 🧘"
                        onChoice(AlgoStep.BrainStateCheck) 
                    }
                    ActionButton("Yes, I'm slouching or tense", DarkSurfaceVariant) { onChoice(AlgoStep.PostureAction) }
                }
                AlgoStep.PostureAction -> {
                    StepTitle("Sit Up & Align", Icons.Default.SelfImprovement, LightBlueAccent)
                    StepInstruction("Sit up straight, roll your shoulders back and down. Relax your jaw, un-clench your teeth, and let your hands rest. Take three deep, slow belly breaths. Breathe in... hold... release...")
                    ActionButton("I feel more aligned!", LightBlueAccent) { 
                        checkedStatus["Posture"] = "Aligned Shoulders & Posture 🧘"
                        onChoice(AlgoStep.BrainStateCheck) 
                    }
                }

                // 13. RACING MIND DUMP (NEW!)
                AlgoStep.BrainStateCheck -> {
                    StepTitle("13. Mental Overload", Icons.Default.Psychology, LavenderAccent)
                    StepQuestion("Is your brain racing with thoughts, lists, worries, or intrusive overload?")
                    ActionButton("No, my mind feels okay", LavenderAccent) {
                        checkedStatus["Mind State"] = "Mind Peaceful 🧠"
                        onChoice(AlgoStep.EmotionalCheck)
                    }
                    ActionButton("Yes, my mind is racing / overloaded", DarkSurfaceVariant) { onChoice(AlgoStep.BrainDumpAction) }
                }
                AlgoStep.BrainDumpAction -> {
                    StepTitle("Clear Mind (Brain Dump)", Icons.Default.Psychology, LavenderAccent)
                    StepInstruction("Get those thoughts out of your head and onto paper. Write whatever is running in your mind. We will save it directly into your notes database so you don't lose it.")
                    
                    var dumpText by remember(step) { mutableStateOf("") }
                    
                    OutlinedTextField(
                        value = dumpText,
                        onValueChange = { dumpText = it },
                        placeholder = { Text("Dump your intrusive worries, lists, or thoughts here...", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("self_care_dump_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    
                    Button(
                        onClick = {
                            if (dumpText.isNotBlank()) {
                                viewModel.addNote(
                                    title = "Self-Care Mind Dump",
                                    content = dumpText + "\n\n#self-care-dump"
                                )
                                checkedStatus["Mind State"] = "Mind Dumped to Notes 🧠"
                            } else {
                                checkedStatus["Mind State"] = "Mind Cleaned 🧠"
                            }
                            onChoice(AlgoStep.EmotionalCheck)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(if (dumpText.isBlank()) "Skip Dump" else "Dump & Save to Notes 💾", fontWeight = FontWeight.Bold)
                    }
                }

                // 14. EMOTIONAL COGNITIVE
                AlgoStep.EmotionalCheck -> {
                    StepTitle("14. Self Compassion", Icons.Default.Favorite, HighPriorityRed)
                    StepQuestion("Are you feeling overwhelmed by guilt, failure, or paralysis because you aren't 'doing enough'?")
                    ActionButton("No, I feel emotionally okay", HighPriorityRed) { 
                        checkedStatus["Self-Compassion"] = "Emotionally Centered ❤️"
                        onChoice(AlgoStep.Conclusion) 
                    }
                    ActionButton("Yes, I feel paralyzed or guilty", DarkSurfaceVariant) { onChoice(AlgoStep.EmotionalAction) }
                }
                AlgoStep.EmotionalAction -> {
                    StepTitle("Be Gentle with Yourself", Icons.Default.Favorite, HighPriorityRed)
                    StepInstruction("Remember, you are a complex biological organism with physical limits and emotional needs. You are not a machine. Today is about survival and caring for yourself, not maximizing productivity. Give yourself permission to do nothing guilt-free.")
                    ActionButton("I understand and forgive myself", HighPriorityRed) { 
                        checkedStatus["Self-Compassion"] = "Practiced Compassion ❤️"
                        onChoice(AlgoStep.Conclusion) 
                    }
                }

                // CONCLUSION
                AlgoStep.Conclusion -> {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(44.dp))
                    Text(
                        text = "Checkup Completed!",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Your completed wellness scorecard. Fantastic work taking action for your health!",
                        color = DarkTextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    // Scorecard grid
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(checkedStatus.toList()) { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = key,
                                        color = DarkTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = value,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = LavenderAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Divider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }

                    Button(
                        onClick = onReset,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Restart Checkup", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onFinish,
                        shape = RoundedCornerShape(16.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Return to Tools", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StepTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun StepQuestion(question: String) {
    Text(
        text = question,
        color = Color.White,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun StepInstruction(instruction: String) {
    Text(
        text = instruction,
        color = DarkTextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun ActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (color == DarkSurfaceVariant) DarkSurfaceVariant else color,
            contentColor = if (color == DarkSurfaceVariant) Color.White else Color.Black
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ---------------- BREATHING BUDDY ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingBuddyScreen(
    onBack: () -> Unit
) {
    var phase by remember { mutableStateOf("Inhale") }
    var countdown by remember { mutableStateOf(4) }
    var isRunning by remember { mutableStateOf(false) }

    // Coroutine loop for breath phase progression
    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning) {
            phase = "Inhale"
            for (i in 4 downTo 1) {
                countdown = i
                delay(1000)
            }
            phase = "Hold"
            for (i in 4 downTo 1) {
                countdown = i
                delay(1000)
            }
            phase = "Exhale"
            for (i in 4 downTo 1) {
                countdown = i
                delay(1000)
            }
            phase = "Hold"
            for (i in 4 downTo 1) {
                countdown = i
                delay(1000)
            }
        }
    }

    // Interactive custom scale depending on phase
    val breathingScale by animateFloatAsState(
        targetValue = when {
            !isRunning -> 1.0f
            phase == "Inhale" -> 1.8f
            phase == "Exhale" -> 1.0f
            phase == "Hold" && countdown == 4 && isRunning -> 1.8f // keep expanded or contracted
            else -> 1.4f
        },
        animationSpec = androidx.compose.animation.core.tween<Float>(durationMillis = 4000),
        label = "BreathCircleScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Breathing Buddy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Box Breathing Reset",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Equal parts inhale, hold, exhale, hold. Simple and calming.",
                    color = DarkTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Interactive Circle Visual
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                // Expanding glowing ripple background
                Box(
                    modifier = Modifier
                        .size((120 * breathingScale).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    LightBlueAccent.copy(alpha = 0.4f),
                                    LightBlueAccent.copy(alpha = 0.05f)
                                )
                            )
                        )
                )

                // Main Core Breathing Circle
                Box(
                    modifier = Modifier
                        .size((80 * breathingScale).dp)
                        .clip(CircleShape)
                        .background(LightBlueAccent)
                        .border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isRunning) phase else "Ready",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (isRunning) {
                            Text(
                                text = countdown.toString(),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }

            // Controls
            Button(
                onClick = { isRunning = !isRunning },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) HighPriorityRed else LavenderAccent,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isRunning) "Pause Exercises" else "Start Breathing",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------------- POMODORO SCREEN ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    onBack: () -> Unit
) {
    var isWorking by remember { mutableStateOf(true) }
    var totalTime by remember { mutableStateOf(1500L) } // 25 minutes
    var timeLeft by remember { mutableStateOf(1500L) }
    var timerRunning by remember { mutableStateOf(false) }
    var timer: CountDownTimer? by remember { mutableStateOf(null) }

    // Start or stop countdown
    DisposableEffect(timerRunning) {
        if (timerRunning) {
            val countDownTimer = object : CountDownTimer(timeLeft * 1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeft = millisUntilFinished / 1000
                }

                override fun onFinish() {
                    timerRunning = false
                    isWorking = !isWorking
                    timeLeft = if (isWorking) 1500L else 300L
                }
            }
            countDownTimer.start()
            timer = countDownTimer
        } else {
            timer?.cancel()
        }

        onDispose {
            timer?.cancel()
        }
    }

    val minutes = (timeLeft / 60).toString().padStart(2, '0')
    val seconds = (timeLeft % 60).toString().padStart(2, '0')

    val progress = remember(timeLeft, totalTime) {
        if (totalTime > 0) (timeLeft.toFloat() / totalTime).coerceIn(0f, 1f) else 1f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Pomodoro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isWorking) "Work Session" else "Short Rest",
                    color = if (isWorking) LavenderAccent else LightBlueAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "A minimalist timer to keep you on task without cognitive load.",
                    color = DarkTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Circular progress with minutes and seconds
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    color = if (isWorking) LavenderAccent else LightBlueAccent,
                    trackColor = GlassBorder,
                    strokeWidth = 10.dp,
                    modifier = Modifier.fillMaxSize()
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$minutes:$seconds",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    )
                    Text(
                        text = if (timerRunning) "Focusing" else "Paused",
                        color = DarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { timerRunning = !timerRunning },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timerRunning) HighPriorityRed else LavenderAccent,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = if (timerRunning) "Pause" else "Start",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        timerRunning = false
                        isWorking = true
                        totalTime = 1500L
                        timeLeft = 1500L
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// DO NOTHING (WU WEI) APPLET IMPLEMENTATION
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoNothingScreen(
    viewModel: DoViewModel,
    onBack: () -> Unit
) {
    val logs by viewModel.doNothingLogs.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var isPracticing by remember { mutableStateOf(false) }
    var selectedDuration by remember { mutableStateOf(3) } // 3 minutes default
    var selectedCategory by remember { mutableStateOf("Contemplative thoughts") }
    
    val durations = listOf(1, 3, 5, 10, 20, -1) // -1 means untimed/infinite
    val categories = listOf("Contemplative thoughts", "Positive growth mindset", "All blended")
    
    if (isPracticing) {
        DoNothingPracticeScreen(
            targetMinutes = selectedDuration,
            categoryName = selectedCategory,
            onFinished = { actualSeconds, sentencesLogged, count ->
                viewModel.addDoNothingLog(
                    targetMinutes = selectedDuration,
                    actualSeconds = actualSeconds,
                    categoryName = selectedCategory,
                    sentencesCount = count,
                    sentencesLogged = sentencesLogged
                )
                isPracticing = false
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Do Nothing (Wu wei)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // 1. Zen Calm Introduction
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF81C784).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Spa,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "The Art of Wu Wei",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Wu wei is the ancient practice of effortless action or non-doing. Step away from deadlines, schedules, and screens. Allow yourself to simply exist.",
                                    color = DarkTextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // 2. Duration Selector
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "How long would you like to do nothing?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                durations.forEach { duration ->
                                    val label = if (duration == -1) "Untimed ♾️" else "${duration}m"
                                    val isSelected = selectedDuration == duration
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFF81C784).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color(0xFF81C784) else GlassBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedDuration = duration },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else DarkTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Category Selector
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Select contemplative category",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { category ->
                                    val isSelected = selectedCategory == category
                                    val description = when (category) {
                                        "Contemplative thoughts" -> "200+ silent observation and Zen mindfulness prompts."
                                        "Positive growth mindset" -> "200+ growth attitude and self-compassion thoughts."
                                        else -> "400+ mixed thoughts from both libraries."
                                    }
                                    
                                    Card(
                                        onClick = { selectedCategory = category },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFF81C784).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color(0xFF81C784) else GlassBorder,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedCategory = category },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = Color(0xFF81C784),
                                                    unselectedColor = Color.White.copy(alpha = 0.3f)
                                                )
                                            )
                                            
                                            Column {
                                                Text(
                                                    text = category,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = description,
                                                    color = DarkTextSecondary,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Begin Button
                    item {
                        Button(
                            onClick = { isPracticing = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF81C784),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Begin Practice of Stillness", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    // 5. Statistics Divider
                    item {
                        HorizontalDivider(color = GlassBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                    }

                    item {
                        Text(
                            text = "Wu Wei Analytics & History",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    item {
                        val totalSessions = logs.size
                        val totalSeconds = logs.sumOf { it.actualSeconds }
                        val totalTimeStr = formatDuration(totalSeconds)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f).border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Total Sessions", color = DarkTextSecondary, fontSize = 10.sp)
                                    Text("$totalSessions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1.3f).border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Total Stillness", color = DarkTextSecondary, fontSize = 10.sp)
                                    Text(totalTimeStr, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }

                    // Action Buttons Row (Export & Clear)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val report = exportDoNothingData(logs)
                                    NoteExporter.shareNoteAsFile(context, "wu_wei_stillness_analytics", report, "txt")
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                                Text("Export Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (logs.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.clearDoNothingLogs() },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, HighPriorityRed.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HighPriorityRed),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                                    Text("Clear Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Detailed History
                    if (logs.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.01f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                Box(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No logs yet. Begin your practice of Wu Wei to write your stillness story.",
                                        color = DarkTextSecondary,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(logs) { log ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(log.categoryName, fontSize = 9.sp, color = Color(0xFF81C784)) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.Transparent),
                                            border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.3f))
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val targetLabel = if (log.targetMinutes == -1) "Infinite" else "${log.targetMinutes}m target"
                                        Text(
                                            text = "Spent: ${formatDuration(log.actualSeconds)} (Target: $targetLabel)",
                                            color = DarkTextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "${log.sentencesCount} thoughts seen",
                                            color = DarkTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoNothingPracticeScreen(
    targetMinutes: Int,
    categoryName: String,
    onFinished: (Int, String, Int) -> Unit
) {
    val baseList = remember(categoryName) {
        when (categoryName) {
            "Contemplative thoughts" -> DoNothingData.contemplativeSentences
            "Positive growth mindset" -> DoNothingData.positiveMindsetSentences
            else -> DoNothingData.contemplativeSentences + DoNothingData.positiveMindsetSentences
        }
    }
    
    val shuffledList = remember(baseList) { baseList.shuffled() }
    
    var currentSentenceIndex by remember { mutableStateOf(0) }
    var currentSentence by remember { mutableStateOf("Take a deep breath and settle in...") }
    var textAlpha by remember { mutableStateOf(0f) }
    
    val alpha by animateFloatAsState(
        targetValue = textAlpha,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1500),
        label = "TextFade"
    )
    
    val startTime = remember { System.currentTimeMillis() }
    val seenSentences = remember { mutableStateListOf<String>() }
    var practiceCompleted by remember { mutableStateOf(false) }
    
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    
    val targetMillis = if (targetMinutes > 0) targetMinutes * 60 * 1000L else Long.MAX_VALUE
    
    LaunchedEffect(Unit) {
        delay(1000)
        textAlpha = 1f
        seenSentences.add("Intro (Settle in)")
        
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            
            if (elapsed >= targetMillis && !practiceCompleted && targetMinutes > 0) {
                textAlpha = 0f
                delay(1500)
                
                currentSentence = "Your chosen practice time is complete. You have rested. Tap three times anywhere to return."
                practiceCompleted = true
                textAlpha = 1f
                seenSentences.add("Practice complete")
                break
            }
            
            val stayTime = kotlin.random.Random.nextLong(4000, 11000)
            delay(stayTime)
            
            if ((System.currentTimeMillis() - startTime) >= targetMillis && targetMinutes > 0) {
                continue
            }
            
            textAlpha = 0f
            delay(1500)
            
            if (shuffledList.isNotEmpty()) {
                val nextSentence = shuffledList[currentSentenceIndex % shuffledList.size]
                currentSentenceIndex++
                currentSentence = nextSentence
                seenSentences.add(nextSentence)
            }
            
            textAlpha = 1f
            delay(1500)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060606))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                val now = System.currentTimeMillis()
                if (now - lastTapTime > 1500) {
                    tapCount = 1
                } else {
                    tapCount++
                }
                lastTapTime = now
                if (tapCount >= 3) {
                    val actualSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                    val sentencesSummary = seenSentences.joinToString(", ")
                    onFinished(actualSeconds, sentencesSummary, seenSentences.size)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentSentence,
                color = Color.White.copy(alpha = alpha),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Tap 3 times to exit",
                color = Color.White.copy(alpha = (0.12f * alpha)),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// DO NOTHING HELPER FUNCTIONS
// ==========================================

private fun formatDuration(seconds: Int): String {
    return if (seconds < 60) {
        "${seconds}s"
    } else {
        val mins = seconds / 60
        val secs = seconds % 60
        if (secs == 0) "${mins}m" else "${mins}m ${secs}s"
    }
}

private fun exportDoNothingData(logs: List<DoNothingLog>): String {
    val sb = StringBuilder()
    sb.append("=========================================\n")
    sb.append("WU WEI (DO NOTHING) PRACTICE ANALYTICS\n")
    sb.append("=========================================\n")
    sb.append("Generated on: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())).append("\n\n")
    
    if (logs.isEmpty()) {
        sb.append("No practice logs recorded yet. Begin your stillness journey today!")
        return sb.toString()
    }
    
    val totalSeconds = logs.sumOf { it.actualSeconds }
    val totalMinutesStr = formatDuration(totalSeconds)
    
    sb.append("SUMMARY STATISTICS:\n")
    sb.append("- Total Sessions Completed: ").append(logs.size).append("\n")
    sb.append("- Total Accumulated Stillness: ").append(totalMinutesStr).append("\n")
    
    val favoriteCategory = logs.groupBy { it.categoryName }
        .maxByOrNull { it.value.size }?.key ?: "None"
    sb.append("- Most Practiced Category: ").append(favoriteCategory).append("\n\n")
    
    sb.append("DETAILED SESSION HISTORY:\n")
    sb.append("-----------------------------------------\n")
    
    logs.forEachIndexed { index, log ->
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
        val targetStr = if (log.targetMinutes == -1) "Infinite" else "${log.targetMinutes} minutes"
        val actualStr = formatDuration(log.actualSeconds)
        
        sb.append("Session #").append(logs.size - index).append(" | ").append(dateStr).append("\n")
        sb.append("- Chosen Category: ").append(log.categoryName).append("\n")
        sb.append("- Intended Target: ").append(targetStr).append("\n")
        sb.append("- Actual Rest Time: ").append(actualStr).append("\n")
        sb.append("- Sentences Contemplated (").append(log.sentencesCount).append("):\n")
        
        val sentences = log.sentencesLogged.split(", ")
        sentences.filter { it.isNotBlank() }.forEach { sentence ->
            sb.append("  * ").append(sentence).append("\n")
        }
        sb.append("-----------------------------------------\n")
    }
    
    return sb.toString()
}

// ==========================================
// MEASUREMENT TRACKER APPLET IMPLEMENTATION
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementTrackerScreen(
    viewModel: DoViewModel,
    onBack: () -> Unit
) {
    val trackers by viewModel.measurementTrackers.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedTrackerId by remember { mutableStateOf<Long?>(null) }
    
    // Auto-select first tracker if none is selected
    LaunchedEffect(trackers) {
        if (selectedTrackerId == null && trackers.isNotEmpty()) {
            selectedTrackerId = trackers.first().id
        }
    }

    val activeTracker = trackers.find { it.id == selectedTrackerId } ?: trackers.firstOrNull()

    // Observe logs for active tracker
    val logs = if (activeTracker != null) {
        viewModel.getLogsForTracker(activeTracker.id).collectAsState(initial = emptyList()).value
    } else {
        emptyList()
    }

    // State for dialogs
    var showAddTrackerDialog by remember { mutableStateOf(false) }
    var showEditTrackerDialog by remember { mutableStateOf(false) }
    var showAddLogDialog by remember { mutableStateOf(false) }
    var logToEdit by remember { mutableStateOf<MeasurementLog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Measurement Tracker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddTrackerDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Tracker", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (trackers.isEmpty()) {
                // Empty state & Preset Onboarding
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF64B5F6).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = "Ready to start tracking?",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Log and analyze any health, body, or custom metric over time. Track milestones and progress towards your target goals.",
                                    color = DarkTextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Or start instantly with a preset:",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }

                    item {
                        PresetTrackerCard(
                            title = "Body Weight Goal",
                            description = "Track loss or gain with friendly milestone checkpoints.",
                            presetName = "Body Weight",
                            presetUnit = "kg",
                            presetInitial = 102.7,
                            presetTarget = 84.0,
                            presetMilestones = "100.0,99.0,98.0,96.0,94.0,91.0,89.0,87.0,84.0",
                            onSelected = { name, unit, initVal, targetVal, milestones ->
                                viewModel.addMeasurementTracker(name, unit, initVal, targetVal, milestones, "Weekly")
                            }
                        )
                    }

                    item {
                        PresetTrackerCard(
                            title = "Waist Size Tracker",
                            description = "Monitor core waist dimensions over time in cm or inches.",
                            presetName = "Waist Circumference",
                            presetUnit = "cm",
                            presetInitial = 96.0,
                            presetTarget = 85.0,
                            presetMilestones = "94,92,90,88,86,85",
                            onSelected = { name, unit, initVal, targetVal, milestones ->
                                viewModel.addMeasurementTracker(name, unit, initVal, targetVal, milestones, "Weekly")
                            }
                        )
                    }

                    item {
                        PresetTrackerCard(
                            title = "Body Mass Index (BMI)",
                            description = "Move down to healthier BMI classifications.",
                            presetName = "BMI",
                            presetUnit = "",
                            presetInitial = 29.5,
                            presetTarget = 24.0,
                            presetMilestones = "28,27,26,25,24",
                            onSelected = { name, unit, initVal, targetVal, milestones ->
                                viewModel.addMeasurementTracker(name, unit, initVal, targetVal, milestones, "Monthly")
                            }
                        )
                    }

                    item {
                        Button(
                            onClick = { showAddTrackerDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6), contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 16.dp)
                        ) {
                            Text("Create Custom Tracker", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                // Main Tracker UI
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Tracker Selector TabRow
                    item {
                        ScrollableTabRow(
                            selectedTabIndex = trackers.indexOfFirst { it.id == selectedTrackerId }.coerceAtLeast(0),
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = { tabPositions ->
                                if (tabPositions.isNotEmpty()) {
                                    val index = trackers.indexOfFirst { it.id == selectedTrackerId }.coerceAtLeast(0)
                                    if (index < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                                            color = Color(0xFF64B5F6)
                                        )
                                    }
                                }
                            }
                        ) {
                            trackers.forEach { tracker ->
                                val isSelected = tracker.id == selectedTrackerId
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedTrackerId = tracker.id },
                                    text = {
                                        Text(
                                            text = tracker.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    selectedContentColor = Color.White,
                                    unselectedContentColor = DarkTextSecondary
                                )
                            }
                        }
                    }

                    if (activeTracker != null) {
                        // Overview Progress Card
                        item {
                            val currentLatestValue = if (logs.isNotEmpty()) logs.last().value else activeTracker.initialValue
                            val initialVal = activeTracker.initialValue
                            val targetVal = activeTracker.targetValue
                            
                            val totalDelta = targetVal - initialVal
                            val currentDelta = currentLatestValue - initialVal
                            
                            val percentProgress = if (totalDelta != 0.0) {
                                ((currentDelta / totalDelta) * 100).coerceIn(0.0, 100.0)
                            } else {
                                100.0
                            }

                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Current Value",
                                                color = DarkTextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Row(
                                                verticalAlignment = Alignment.Bottom,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "$currentLatestValue",
                                                    color = Color.White,
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (activeTracker.unit.isNotBlank()) {
                                                    Text(
                                                        text = activeTracker.unit,
                                                        color = DarkTextSecondary,
                                                        fontSize = 14.sp,
                                                        modifier = Modifier.padding(bottom = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IconButton(
                                                onClick = { showEditTrackerDialog = true },
                                                modifier = Modifier.background(Color.White.copy(alpha = 0.03f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Settings, contentDescription = "Edit Tracker Settings", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                            
                                            IconButton(
                                                onClick = { showAddLogDialog = true },
                                                modifier = Modifier.background(Color(0xFF64B5F6).copy(alpha = 0.15f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Add Measurement Log", tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }

                                    // Starting & Target Values Label
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Starting: $initialVal ${activeTracker.unit}",
                                            color = DarkTextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Target: $targetVal ${activeTracker.unit}",
                                            color = Color(0xFF81C784),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Progress Bar
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LinearProgressIndicator(
                                            progress = { (percentProgress / 100f).toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = Color(0xFF64B5F6),
                                            trackColor = Color.White.copy(alpha = 0.05f)
                                        )
                                        Text(
                                            text = String.format("%.1f%% of goal achieved", percentProgress),
                                            color = DarkTextSecondary,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Interval: ${activeTracker.trackingInterval}",
                                            color = DarkTextSecondary,
                                            fontSize = 11.sp
                                        )
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF81C784))
                                            )
                                            Text(
                                                text = "Active Tracking",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Progress Curve Line Chart
                        item {
                            val parsedMilestones = remember(activeTracker.milestonesStr) {
                                activeTracker.milestonesStr.split(",")
                                    .mapNotNull { it.trim().toDoubleOrNull() }
                            }

                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.01f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Progress Curve & Milestones",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    MeasurementLineChart(
                                        tracker = activeTracker,
                                        logs = logs,
                                        milestones = parsedMilestones,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )

                                    // Legend Card Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.size(10.dp, 3.dp).background(Color(0xFF64B5F6)))
                                            Text("My Weight", color = DarkTextSecondary, fontSize = 9.sp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(modifier = Modifier.size(10.dp, 3.dp).background(Color(0xFF81C784)))
                                            Text("Ultimate Goal", color = DarkTextSecondary, fontSize = 9.sp)
                                        }
                                        if (parsedMilestones.isNotEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(modifier = Modifier.size(10.dp, 1.dp).background(Color.White.copy(alpha = 0.3f)))
                                                Text("Milestones", color = DarkTextSecondary, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Milestones Sub-Targets Checklist
                        item {
                            val parsedMilestones = remember(activeTracker.milestonesStr) {
                                activeTracker.milestonesStr.split(",")
                                    .mapNotNull { it.trim().toDoubleOrNull() }
                            }

                            if (parsedMilestones.isNotEmpty()) {
                                val latestValue = if (logs.isNotEmpty()) logs.last().value else activeTracker.initialValue
                                val isDecreasingGoal = activeTracker.targetValue < activeTracker.initialValue

                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Milestones & Sub-targets",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            parsedMilestones.forEach { milestone ->
                                                val achieved = if (isDecreasingGoal) {
                                                    latestValue <= milestone
                                                } else {
                                                    latestValue >= milestone
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            if (achieved) Color(0xFF81C784).copy(alpha = 0.05f) else Color.Transparent,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = if (achieved) Icons.Default.CheckCircle else Icons.Default.Flag,
                                                            contentDescription = null,
                                                            tint = if (achieved) Color(0xFF81C784) else Color.White.copy(alpha = 0.2f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = "Milestone: $milestone ${activeTracker.unit}",
                                                            color = if (achieved) Color.White else Color.White.copy(alpha = 0.6f),
                                                            fontWeight = if (achieved) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 12.sp
                                                        )
                                                    }

                                                    if (achieved) {
                                                        Text(
                                                            text = "Achieved! 🎉",
                                                            color = Color(0xFF81C784),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    } else {
                                                        val delta = if (isDecreasingGoal) {
                                                            latestValue - milestone
                                                        } else {
                                                            milestone - latestValue
                                                        }
                                                        Text(
                                                            text = String.format("+%.1f %s left", delta, activeTracker.unit),
                                                            color = DarkTextSecondary,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Actions row (Export & Delete tracker)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val csvData = exportMeasurementData(activeTracker, logs)
                                        val filename = "measurement_${activeTracker.name.lowercase().replace(" ", "_")}_data"
                                        NoteExporter.shareNoteAsFile(context, filename, csvData, "txt")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                                    Text("Export Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.deleteMeasurementTracker(activeTracker.id)
                                        selectedTrackerId = null
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, HighPriorityRed.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HighPriorityRed),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                                    Text("Delete Tracker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Logs / Daily measurement logs history
                        item {
                            Text(
                                text = "Measurement History",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (logs.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.01f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No recorded logs. Tap the '+' icon above to add your first log.",
                                            color = DarkTextSecondary,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(logs.reversed()) { log ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = SimpleDateFormat("EEEE - MMM dd, yyyy", Locale.getDefault()).format(Date(log.timestamp)),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (log.notes.isNotBlank()) {
                                                Text(
                                                    text = "Notes: ${log.notes}",
                                                    color = DarkTextSecondary,
                                                    fontSize = 10.sp,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${log.value} ${activeTracker.unit}",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            IconButton(
                                                onClick = { logToEdit = log },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Modify log", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteMeasurementLog(log.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = HighPriorityRed.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Custom Tracker
    if (showAddTrackerDialog) {
        var name by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf("kg") }
        var initialStr by remember { mutableStateOf("") }
        var targetStr by remember { mutableStateOf("") }
        var milestoneStr by remember { mutableStateOf("") }
        var interval by remember { mutableStateOf("Weekly") }

        AlertDialog(
            onDismissRequest = { showAddTrackerDialog = false },
            title = { Text("New Measurement Tracker", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name (e.g. Weight, Bicep)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit of Measurement (e.g. kg, lbs, cm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initialStr,
                        onValueChange = { initialStr = it },
                        label = { Text("Starting Value") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it },
                        label = { Text("Target Goal Value") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = milestoneStr,
                        onValueChange = { milestoneStr = it },
                        label = { Text("Milestones (comma separated values)") },
                        placeholder = { Text("e.g. 100, 98, 95") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Select tracking interval:", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Daily", "Weekly", "Monthly").forEach { opt ->
                            val isSel = interval == opt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF64B5F6).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, if (isSel) Color(0xFF64B5F6) else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { interval = opt },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(opt, color = Color.White, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val initVal = initialStr.toDoubleOrNull() ?: 0.0
                        val targetVal = targetStr.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            viewModel.addMeasurementTracker(name, unit, initVal, targetVal, milestoneStr, interval)
                            showAddTrackerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6), contentColor = Color.Black)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTrackerDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Dialog: Edit Active Tracker Settings
    if (showEditTrackerDialog && activeTracker != null) {
        var name by remember { mutableStateOf(activeTracker.name) }
        var unit by remember { mutableStateOf(activeTracker.unit) }
        var initialStr by remember { mutableStateOf("${activeTracker.initialValue}") }
        var targetStr by remember { mutableStateOf("${activeTracker.targetValue}") }
        var milestoneStr by remember { mutableStateOf(activeTracker.milestonesStr) }
        var interval by remember { mutableStateOf(activeTracker.trackingInterval) }

        AlertDialog(
            onDismissRequest = { showEditTrackerDialog = false },
            title = { Text("Edit Tracker Settings", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tracker Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (e.g. kg, cm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initialStr,
                        onValueChange = { initialStr = it },
                        label = { Text("Starting Value") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it },
                        label = { Text("Target Goal Value") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = milestoneStr,
                        onValueChange = { milestoneStr = it },
                        label = { Text("Milestones") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Select tracking interval:", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Daily", "Weekly", "Monthly").forEach { opt ->
                            val isSel = interval == opt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF64B5F6).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, if (isSel) Color(0xFF64B5F6) else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { interval = opt },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(opt, color = Color.White, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val initVal = initialStr.toDoubleOrNull() ?: activeTracker.initialValue
                        val targetVal = targetStr.toDoubleOrNull() ?: activeTracker.targetValue
                        if (name.isNotBlank()) {
                            viewModel.updateMeasurementTracker(
                                activeTracker.copy(
                                    name = name,
                                    unit = unit,
                                    initialValue = initVal,
                                    targetValue = targetVal,
                                    milestonesStr = milestoneStr,
                                    trackingInterval = interval
                                )
                            )
                            showEditTrackerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6), contentColor = Color.Black)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTrackerDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Dialog: Add Measurement Log
    if (showAddLogDialog && activeTracker != null) {
        val suggestedValue = if (logs.isNotEmpty()) logs.last().value else activeTracker.initialValue
        var logValueStr by remember { mutableStateOf("$suggestedValue") }
        var logNotes by remember { mutableStateOf("") }
        var dateChoice by remember { mutableStateOf("Today") } // Today, Yesterday, Custom

        // Custom date fields
        val cal = remember { Calendar.getInstance() }
        var customDay by remember { mutableStateOf("${cal.get(Calendar.DAY_OF_MONTH)}") }
        var customMonth by remember { mutableStateOf("${cal.get(Calendar.MONTH) + 1}") }
        var customYear by remember { mutableStateOf("${cal.get(Calendar.YEAR)}") }

        AlertDialog(
            onDismissRequest = { showAddLogDialog = false },
            title = { Text("Log New Measurement", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = logValueStr,
                        onValueChange = { logValueStr = it },
                        label = { Text("Measurement Value (${activeTracker.unit})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = logNotes,
                        onValueChange = { logNotes = it },
                        label = { Text("Comment / Note (Optional)") },
                        placeholder = { Text("e.g., morning fasting, after workout") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Editable date row
                    Text("Measurement date:", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Today", "Yesterday", "Custom").forEach { opt ->
                            val isSel = dateChoice == opt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF64B5F6).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, if (isSel) Color(0xFF64B5F6) else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { dateChoice = opt },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(opt, color = Color.White, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    if (dateChoice == "Custom") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = customDay,
                                onValueChange = { customDay = it },
                                label = { Text("DD") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = customMonth,
                                onValueChange = { customMonth = it },
                                label = { Text("MM") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = customYear,
                                onValueChange = { customYear = it },
                                label = { Text("YYYY") },
                                modifier = Modifier.weight(1.5f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val valDouble = logValueStr.toDoubleOrNull()
                        if (valDouble != null) {
                            val timestamp = when (dateChoice) {
                                "Today" -> System.currentTimeMillis()
                                "Yesterday" -> System.currentTimeMillis() - 86400000L
                                else -> {
                                    val d = customDay.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                                    val m = (customMonth.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                                    val y = customYear.toIntOrNull() ?: cal.get(Calendar.YEAR)
                                    
                                    val customCal = Calendar.getInstance()
                                    customCal.set(y, m, d)
                                    customCal.timeInMillis
                                }
                            }
                            
                            viewModel.addMeasurementLog(
                                trackerId = activeTracker.id,
                                value = valDouble,
                                timestamp = timestamp,
                                notes = logNotes
                            )
                            showAddLogDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6), contentColor = Color.Black)
                ) {
                    Text("Save Log", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLogDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Dialog: Edit Log / Change Measurement Day & Value
    if (logToEdit != null && activeTracker != null) {
        val editingLog = logToEdit!!
        var logValueStr by remember { mutableStateOf("${editingLog.value}") }
        var logNotes by remember { mutableStateOf(editingLog.notes) }
        var dateChoice by remember { mutableStateOf("Original") } // Original, Today, Yesterday, Custom

        val cal = remember { Calendar.getInstance().apply { timeInMillis = editingLog.timestamp } }
        var customDay by remember { mutableStateOf("${cal.get(Calendar.DAY_OF_MONTH)}") }
        var customMonth by remember { mutableStateOf("${cal.get(Calendar.MONTH) + 1}") }
        var customYear by remember { mutableStateOf("${cal.get(Calendar.YEAR)}") }

        AlertDialog(
            onDismissRequest = { logToEdit = null },
            title = { Text("Edit Log details", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = logValueStr,
                        onValueChange = { logValueStr = it },
                        label = { Text("Log Value (${activeTracker.unit})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = logNotes,
                        onValueChange = { logNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Change date:", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Original", "Today", "Custom").forEach { opt ->
                            val isSel = dateChoice == opt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF64B5F6).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, if (isSel) Color(0xFF64B5F6) else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { dateChoice = opt },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(opt, color = Color.White, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    if (dateChoice == "Custom") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = customDay,
                                onValueChange = { customDay = it },
                                label = { Text("DD") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = customMonth,
                                onValueChange = { customMonth = it },
                                label = { Text("MM") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = customYear,
                                onValueChange = { customYear = it },
                                label = { Text("YYYY") },
                                modifier = Modifier.weight(1.5f),
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val valDouble = logValueStr.toDoubleOrNull()
                        if (valDouble != null) {
                            val timestamp = when (dateChoice) {
                                "Original" -> editingLog.timestamp
                                "Today" -> System.currentTimeMillis()
                                else -> {
                                    val d = customDay.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                                    val m = (customMonth.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                                    val y = customYear.toIntOrNull() ?: cal.get(Calendar.YEAR)
                                    
                                    val customCal = Calendar.getInstance()
                                    customCal.set(y, m, d)
                                    customCal.timeInMillis
                                }
                            }
                            
                            viewModel.updateMeasurementLog(
                                editingLog.copy(
                                    value = valDouble,
                                    timestamp = timestamp,
                                    notes = logNotes
                                )
                            )
                            logToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6), contentColor = Color.Black)
                ) {
                    Text("Update Log", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { logToEdit = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

// ==========================================
// PRESET ONBOARDING CARD COMPOSABLE
// ==========================================

@Composable
fun PresetTrackerCard(
    title: String,
    description: String,
    presetName: String,
    presetUnit: String,
    presetInitial: Double,
    presetTarget: Double,
    presetMilestones: String,
    onSelected: (String, String, Double, Double, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onSelected(presetName, presetUnit, presetInitial, presetTarget, presetMilestones) },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(description, color = DarkTextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                Text(
                    text = "Goal: $presetInitial $presetUnit ➔ $presetTarget $presetUnit",
                    color = Color(0xFF64B5F6),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "Add Preset",
                tint = Color(0xFF64B5F6),
                modifier = Modifier.size(24.dp).padding(start = 8.dp)
            )
        }
    }
}

// ==========================================
// PROGRESS LINE CHART COMPOSABLE
// ==========================================

@Composable
fun MeasurementLineChart(
    tracker: MeasurementTracker,
    logs: List<MeasurementLog>,
    milestones: List<Double>,
    modifier: Modifier = Modifier
) {
    val points = remember(tracker, logs) {
        val list = mutableListOf<Pair<Long, Double>>()
        // Add starting point
        list.add(Pair(tracker.createdAt, tracker.initialValue))
        // Add all log entries
        logs.forEach { log ->
            list.add(Pair(log.timestamp, log.value))
        }
        list.sortBy { it.first }
        list
    }

    if (points.size < 2 && logs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.White.copy(alpha = 0.01f), RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Log measurements to draw progress curve.",
                    color = DarkTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        return
    }

    val minTime = points.first().first
    val maxTime = points.last().first
    val timeRange = if (maxTime == minTime) 86400000L else maxTime - minTime

    val allValues = points.map { it.second } + tracker.targetValue + milestones
    val maxValue = allValues.maxOrNull() ?: 100.0
    val minValue = allValues.minOrNull() ?: 0.0
    val valueRange = if (maxValue == minValue) 10.0 else maxValue - minValue
    
    val yMax = maxValue + (valueRange * 0.1)
    val yMin = maxOf(0.0, minValue - (valueRange * 0.1))
    val yRange = yMax - yMin

    val chartColor = Color(0xFF64B5F6)
    val gridColor = Color.White.copy(alpha = 0.04f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height

        fun getX(timestamp: Long): Float {
            val fraction = (timestamp - minTime).toFloat() / timeRange
            return fraction * width
        }

        fun getY(value: Double): Float {
            val fraction = ((value - yMin) / yRange).toFloat()
            return height - (fraction * height)
        }

        // 1. Grid Lines
        for (i in 1..3) {
            val gridY = height * i / 4f
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, gridY),
                end = androidx.compose.ui.geometry.Offset(width, gridY),
                strokeWidth = 1f
            )
        }

        // 2. Target Goal Line
        val targetY = getY(tracker.targetValue)
        if (targetY in 0f..height) {
            drawLine(
                color = Color(0xFF81C784).copy(alpha = 0.7f),
                start = androidx.compose.ui.geometry.Offset(0f, targetY),
                end = androidx.compose.ui.geometry.Offset(width, targetY),
                strokeWidth = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
        }

        // 3. Milestone Lines
        milestones.forEach { milestone ->
            val milestoneY = getY(milestone)
            if (milestoneY in 0f..height) {
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = androidx.compose.ui.geometry.Offset(0f, milestoneY),
                    end = androidx.compose.ui.geometry.Offset(width, milestoneY),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)
                )
            }
        }

        // 4. Progress Area Curve & Line
        val path = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()

        points.forEachIndexed { index, pair ->
            val px = getX(pair.first)
            val py = getY(pair.second)

            if (index == 0) {
                path.moveTo(px, py)
                fillPath.moveTo(px, height)
                fillPath.lineTo(px, py)
            } else {
                path.lineTo(px, py)
                fillPath.lineTo(px, py)
            }
            
            if (index == points.size - 1) {
                fillPath.lineTo(px, height)
                fillPath.close()
            }
        }

        // Draw gradient fill below trend
        if (points.isNotEmpty()) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(chartColor.copy(alpha = 0.12f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )
        }

        // Draw trend outline
        drawPath(
            path = path,
            color = chartColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        // 5. Dots for measurements
        points.forEach { pair ->
            val px = getX(pair.first)
            val py = getY(pair.second)

            drawCircle(
                color = Color(0xFF0F2C59),
                radius = 4.5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(px, py)
            )
            drawCircle(
                color = chartColor,
                radius = 2.5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(px, py)
            )
        }
    }
}

// ==========================================
// EXPORT MEASUREMENT DATA HELPER
// ==========================================

private fun exportMeasurementData(tracker: MeasurementTracker, logs: List<MeasurementLog>): String {
    val sb = java.lang.StringBuilder()
    sb.append("=========================================\n")
    sb.append("MEASUREMENT TRACKER: ").append(tracker.name.uppercase()).append("\n")
    sb.append("=========================================\n")
    sb.append("Generated on: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())).append("\n\n")
    
    sb.append("TRACKER INFORMATION:\n")
    sb.append("- Initial Starting Point: ").append(tracker.initialValue).append(" ").append(tracker.unit).append("\n")
    sb.append("- Ultimate Target Goal: ").append(tracker.targetValue).append(" ").append(tracker.unit).append("\n")
    sb.append("- Tracking Interval: ").append(tracker.trackingInterval).append("\n")
    sb.append("- Configured Milestones: ").append(tracker.milestonesStr).append("\n\n")
    
    val currentLatestValue = if (logs.isNotEmpty()) logs.last().value else tracker.initialValue
    val logsCount = logs.size
    
    sb.append("CURRENT PROGRESS STATS:\n")
    sb.append("- Latest Log Value: ").append(currentLatestValue).append(" ").append(tracker.unit).append("\n")
    sb.append("- Logs Logged Count: ").append(logsCount).append("\n\n")
    
    sb.append("RECORDED HISTORY (Oldest to Newest):\n")
    sb.append("-----------------------------------------------------------------\n")
    sb.append("Date & Time              | Measurement Value | Comment\n")
    sb.append("-----------------------------------------------------------------\n")
    
    // Starting Point Entry
    val startStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(tracker.createdAt))
    sb.append(startStr.padEnd(24)).append(" | ").append("${tracker.initialValue} ${tracker.unit}".padEnd(17)).append(" | STARTING POINT\n")
    
    // Log entries
    logs.forEach { log ->
        val logStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestamp))
        val notesText = if (log.notes.isNotBlank()) log.notes else "-"
        sb.append(logStr.padEnd(24)).append(" | ").append("${log.value} ${tracker.unit}".padEnd(17)).append(" | ").append(notesText).append("\n")
    }
    sb.append("-----------------------------------------------------------------\n")
    
    return sb.toString()
}

// ==========================================
// AFFIRMATION COUNTER SUB-APP
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AffirmationCounterScreen(
    viewModel: DoViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.affirmationCategories.collectAsState()
    val logs by viewModel.affirmationLogs.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedCategoryIdForSession by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryNameForSession by remember { mutableStateOf("") }

    // Dialog state
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategoryForManagement by remember { mutableStateOf<AffirmationCategory?>(null) }
    var showCategoryOptionsDialog by remember { mutableStateOf<AffirmationCategory?>(null) }

    var activeTabState by remember { mutableStateOf(0) } // 0 = Categories, 1 = History Logs

    if (selectedCategoryIdForSession != null) {
        ActiveAffirmationSession(
            categoryId = selectedCategoryIdForSession!!,
            categoryName = selectedCategoryNameForSession,
            viewModel = viewModel,
            onClose = {
                selectedCategoryIdForSession = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Affirmation Space", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Custom Category", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Section Tabs
                TabRow(
                    selectedTabIndex = activeTabState,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty() && activeTabState < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[activeTabState]),
                                color = Color(0xFFFFB74D)
                            )
                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Tab(
                        selected = activeTabState == 0,
                        onClick = { activeTabState = 0 },
                        text = { Text("My Categories", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        selectedContentColor = Color.White,
                        unselectedContentColor = DarkTextSecondary
                    )
                    Tab(
                        selected = activeTabState == 1,
                        onClick = { activeTabState = 1 },
                        text = { Text("Repetition History", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        selectedContentColor = Color.White,
                        unselectedContentColor = DarkTextSecondary
                    )
                }

                if (activeTabState == 0) {
                    // Categories view
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                                        Text("Select a Mindset Focus", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Text(
                                        text = "Tap any category to begin practicing. Repetition trains neural pathways. Tap on the screen to count repetitions and swipe left/right to change affirmations.",
                                        color = DarkTextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        if (categories.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No categories found. Tap '+' to create one!", color = DarkTextSecondary, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(categories) { category ->
                                val itemsFlow = remember(category.id) { viewModel.getAffirmationsForCategoryFlow(category.id) }
                                val items by itemsFlow.collectAsState(initial = emptyList())

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                        .clickable {
                                            showCategoryOptionsDialog = category
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = category.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                if (category.isDefault) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFFFB74D).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("PRESET", color = Color(0xFFFFB74D), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${items.size} affirmations loaded",
                                                color = DarkTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Start practicing shortcut button
                                            Button(
                                                onClick = {
                                                    selectedCategoryIdForSession = category.id
                                                    selectedCategoryNameForSession = category.name
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D).copy(alpha = 0.12f), contentColor = Color(0xFFFFB74D)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            IconButton(
                                                onClick = { selectedCategoryForManagement = category },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Settings, contentDescription = "Manage sentences", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Repetition History Logs view
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${logs.size} Repetition Sessions recorded",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (logs.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                val csvData = exportAffirmationLogs(logs)
                                                NoteExporter.shareNoteAsFile(context, "affirmation_repetition_history", csvData, "csv")
                                            }
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Export CSV", color = Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        TextButton(
                                            onClick = { viewModel.clearAllAffirmationLogs() }
                                        ) {
                                            Text("Clear Logs", color = HighPriorityRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (logs.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.01f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No practice history recorded yet. Enter any category and tap the screen to record mindful repetitions.",
                                            color = DarkTextSecondary,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(logs) { log ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFFB74D))
                                                )
                                                Text(
                                                    text = log.categoryName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Text(
                                                text = SimpleDateFormat("MMM dd - HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                                                color = DarkTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Text(
                                            text = "\"${log.affirmationText}\"",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 16.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val mins = log.durationSeconds / 60
                                            val secs = log.durationSeconds % 60
                                            val timeStr = String.format("%02d:%02d", mins, secs)

                                            Text(
                                                text = "Duration: $timeStr",
                                                color = DarkTextSecondary,
                                                fontSize = 10.sp
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "${log.tapCount} repetitions",
                                                    color = Color(0xFFFFB74D),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                IconButton(
                                                    onClick = { viewModel.deleteAffirmationLog(log.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = HighPriorityRed.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Custom Category with batch text
    if (showAddCategoryDialog) {
        var name by remember { mutableStateOf("") }
        var batchText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Affirmation Category", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name (e.g. Focus, Courage)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Add Affirmations (Batch Import):", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Type or paste your affirmation statements. Put each new affirmation statement on its own line.", color = DarkTextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                    }

                    OutlinedTextField(
                        value = batchText,
                        onValueChange = { batchText = it },
                        placeholder = { Text("I am calm...\nI focus on one task at a time...\nI trust my process...") },
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addAffirmationCategoryWithItems(name, batchText)
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D), contentColor = Color.Black)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Dialog: Category Options (Start or Manage)
    showCategoryOptionsDialog?.let { category ->
        AlertDialog(
            onDismissRequest = { showCategoryOptionsDialog = null },
            title = { Text(category.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "What would you like to do with this affirmation category?",
                    color = DarkTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedCategoryIdForSession = category.id
                            selectedCategoryNameForSession = category.name
                            showCategoryOptionsDialog = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D), contentColor = Color.Black)
                    ) {
                        Text("Start Practice", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            selectedCategoryForManagement = category
                            showCategoryOptionsDialog = null
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Manage Statements", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!category.isDefault) {
                    TextButton(
                        onClick = {
                            viewModel.deleteAffirmationCategory(category.id)
                            showCategoryOptionsDialog = null
                        }
                    ) {
                        Text("Delete Category", color = HighPriorityRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Dialog: Manage Sentences in Category
    selectedCategoryForManagement?.let { category ->
        val itemsFlow = remember(category.id) { viewModel.getAffirmationsForCategoryFlow(category.id) }
        val items by itemsFlow.collectAsState(initial = emptyList())
        var newBatchText by remember { mutableStateOf("") }
        var showAddPanel by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedCategoryForManagement = null },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Statements: ${category.name}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddPanel = !showAddPanel }) {
                        Text(if (showAddPanel) "Show List" else "Batch Add", color = Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    if (showAddPanel) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                            Text("Batch Add Statements:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Type or paste statements below, one per line.", color = DarkTextSecondary, fontSize = 9.sp)
                            OutlinedTextField(
                                value = newBatchText,
                                onValueChange = { newBatchText = it },
                                placeholder = { Text("I love who I am...\nI am doing my best...") },
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                    } else {
                        if (items.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No affirmations loaded. Tap 'Batch Add' to import statements.", color = DarkTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(items) { statement ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = statement.text,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteAffirmationItem(statement.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HighPriorityRed.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (showAddPanel && newBatchText.isNotBlank()) {
                            viewModel.addAffirmationItemsToCategory(category.id, newBatchText)
                            newBatchText = ""
                            showAddPanel = false
                        } else {
                            selectedCategoryForManagement = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D), contentColor = Color.Black)
                ) {
                    Text(if (showAddPanel) "Import" else "Done", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryForManagement = null }) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

// ==========================================
// ACTIVE AFFIRMATION COUNTER SESSION MODE
// ==========================================

@Composable
fun ActiveAffirmationSession(
    categoryId: Long,
    categoryName: String,
    viewModel: DoViewModel,
    onClose: () -> Unit
) {
    val itemsFlow = remember(categoryId) { viewModel.getAffirmationsForCategoryFlow(categoryId) }
    val items by itemsFlow.collectAsState(initial = emptyList())

    var shuffledItems by remember { mutableStateOf<List<AffirmationItem>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }

    // Session-wide count of total repetitions
    var sessionTotalCount by remember { mutableStateOf(0) }

    // Current specific statement states
    var currentSentenceCount by remember { mutableStateOf(0) }
    var sentenceDurationSeconds by remember { mutableStateOf(0) }

    // Local timing
    var totalSessionSeconds by remember { mutableStateOf(0) }

    // Swipe drag tracking
    var dragAmount by remember { mutableStateOf(0f) }

    // Pulse animation trigger on tapping
    var scaleTrigger by remember { mutableStateOf(1f) }
    val cardScale by animateFloatAsState(
        targetValue = scaleTrigger,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        finishedListener = { scaleTrigger = 1f }
    )

    // Floating text indicator triggers
    var floatingPlusOneList by remember { mutableStateOf<List<Long>>(emptyList()) }

    // Run clock timing
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            totalSessionSeconds++
        }
    }

    LaunchedEffect(currentIndex) {
        sentenceDurationSeconds = 0
        while (true) {
            delay(1000)
            sentenceDurationSeconds++
        }
    }

    // Populate and shuffle elements once
    LaunchedEffect(items) {
        if (items.isNotEmpty() && shuffledItems.isEmpty()) {
            shuffledItems = items.shuffled()
            currentIndex = 0
        }
    }

    val currentAffirmation = shuffledItems.getOrNull(currentIndex)

    fun saveCurrentSentenceLog() {
        val sentence = shuffledItems.getOrNull(currentIndex)
        if (sentence != null && currentSentenceCount > 0) {
            viewModel.logAffirmationRepetition(
                categoryId = categoryId,
                categoryName = categoryName,
                text = sentence.text,
                count = currentSentenceCount,
                durationSecs = sentenceDurationSeconds
            )
        }
    }

    fun navigateToNext() {
        if (shuffledItems.isNotEmpty()) {
            saveCurrentSentenceLog()
            currentSentenceCount = 0
            currentIndex = (currentIndex + 1) % shuffledItems.size
        }
    }

    fun navigateToPrev() {
        if (shuffledItems.isNotEmpty()) {
            saveCurrentSentenceLog()
            currentSentenceCount = 0
            currentIndex = (currentIndex - 1 + shuffledItems.size) % shuffledItems.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0C20), Color(0xFF15102A), Color(0xFF0C091A))
                )
            )
    ) {
        // Upper session controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    saveCurrentSentenceLog()
                    onClose()
                },
                modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Exit Session", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = categoryName.uppercase(),
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                
                val elapsedMins = totalSessionSeconds / 60
                val elapsedSecs = totalSessionSeconds % 60
                Text(
                    text = String.format("%02d:%02d", elapsedMins, elapsedSecs),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // Simple total counter badge
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFB74D).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "TOTAL: $sessionTotalCount",
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        // Central interactive core
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (shuffledItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Hearing, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(48.dp))
                        Text(
                            text = "No statements configured.\nAdd statements in Category settings.",
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    text = "TAP SCREEN TO REPEAT • SWIPE TO CHANGE",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // The glowing tap card container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFFB74D).copy(alpha = 0.05f), Color.Transparent),
                                radius = 600f
                            )
                        )
                        .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                        .clickable {
                            currentSentenceCount++
                            sessionTotalCount++
                            scaleTrigger = 1.05f
                            floatingPlusOneList = floatingPlusOneList + System.currentTimeMillis()
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragAmount > 120f) {
                                        navigateToPrev()
                                    } else if (dragAmount < -120f) {
                                        navigateToNext()
                                    }
                                    dragAmount = 0f
                                },
                                onHorizontalDrag = { _, dragAmountValue ->
                                    dragAmount += dragAmountValue
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Statement text with breathing scale
                        Text(
                            text = currentAffirmation?.text ?: "",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp,
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = cardScale,
                                    scaleY = cardScale
                                )
                                .padding(horizontal = 12.dp)
                        )

                        // Progress inside active statement
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Current Reps",
                                color = DarkTextSecondary,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$currentSentenceCount",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp
                            )
                        }
                    }

                    // Floating rising text indicators (+1)
                    floatingPlusOneList.forEach { id ->
                        RisingIndicatorEffect(
                            key = id,
                            onFinished = {
                                floatingPlusOneList = floatingPlusOneList.filter { it != id }
                            }
                        )
                    }
                }

                // Sentence paging indicator dots
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    shuffledItems.forEachIndexed { idx, _ ->
                        val isSel = idx == currentIndex
                        Box(
                            modifier = Modifier
                                .size(if (isSel) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (isSel) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.15f))
                        )
                    }
                }
            }
        }

        // Footer buttons
        if (shuffledItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navigateToPrev() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.03f), CircleShape)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Affirmation", tint = Color.White)
                }

                Button(
                    onClick = {
                        saveCurrentSentenceLog()
                        onClose()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D), contentColor = Color.Black),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                ) {
                    Text("End Session", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                IconButton(
                    onClick = { navigateToNext() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.03f), CircleShape)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Affirmation", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun RisingIndicatorEffect(
    key: Long,
    onFinished: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    var offsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(key) {
        // Animate floating upwards
        val steps = 30
        for (i in 0..steps) {
            delay(15)
            offsetY -= 4f
        }
        isVisible = false
        onFinished()
    }

    if (isVisible) {
        Box(
            modifier = Modifier
                .offset(y = offsetY.dp)
                .background(Color(0xFFFFB74D).copy(alpha = 0.15f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "+1",
                color = Color(0xFFFFB74D),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// ==========================================
// AFFIRMATION REPETITION LOGS EXPORTER HELPER
// ==========================================

private fun exportAffirmationLogs(logs: List<AffirmationLog>): String {
    val sb = java.lang.StringBuilder()
    sb.append("=========================================\n")
    sb.append("AFFIRMATION REPETITIONS LOGS HISTORY\n")
    sb.append("=========================================\n")
    sb.append("Generated on: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())).append("\n\n")

    sb.append("Date & Time              | Category             | Affirmation Statement                               | Reps  | Duration\n")
    sb.append("--------------------------------------------------------------------------------------------------------------------------\n")

    logs.forEach { log ->
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestamp))
        val mins = log.durationSeconds / 60
        val secs = log.durationSeconds % 60
        val durationStr = String.format("%02d:%02d", mins, secs)
        
        sb.append(dateStr.padEnd(24)).append(" | ")
            .append(log.categoryName.take(20).padEnd(20)).append(" | ")
            .append(log.affirmationText.take(50).padEnd(50)).append(" | ")
            .append(log.tapCount.toString().padEnd(5)).append(" | ")
            .append(durationStr).append("\n")
    }
    sb.append("--------------------------------------------------------------------------------------------------------------------------\n")
    return sb.toString()
}


