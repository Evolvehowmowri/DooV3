package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MomentumMaker
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentumMakerScreen(
    viewModel: DoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val momentumList by viewModel.momentumMakers.collectAsStateWithLifecycle()
    
    // Filters
    var selectedCategoryFilter by remember { mutableStateOf("Any") }
    var selectedTimeFilter by remember { mutableStateOf("Any") } // Any, ≤2m, ≤5m, ≤10m
    
    // Active Picked Task State
    var activeTask by remember { mutableStateOf<MomentumMaker?>(null) }
    var isRolling by remember { mutableStateOf(false) }
    
    // CRUD Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("Clean") }
    var customDuration by remember { mutableStateOf(5) }
    
    // Section Collapse State
    var showAllTasksSection by remember { mutableStateOf(false) }
    
    // Category Options
    val categories = listOf("Any", "Clean", "Mind", "Body", "Admin", "Social")
    val creationCategories = listOf("Clean", "Mind", "Body", "Admin", "Social")
    val timeFilters = listOf("Any", "≤ 2 min", "≤ 5 min", "≤ 10 min")
    
    // Filter helper function
    fun getFilteredList(): List<MomentumMaker> {
        return momentumList.filter { maker ->
            val matchesCategory = if (selectedCategoryFilter == "Any") {
                true
            } else {
                maker.category.lowercase() == selectedCategoryFilter.lowercase()
            }
            
            val matchesTime = when (selectedTimeFilter) {
                "Any" -> true
                "≤ 2 min" -> maker.durationMinutes <= 2
                "≤ 5 min" -> maker.durationMinutes <= 5
                "≤ 10 min" -> maker.durationMinutes <= 10
                else -> true
            }
            
            matchesCategory && matchesTime
        }
    }
    
    // Spin animation handler
    fun rollTask() {
        val filtered = getFilteredList()
        if (filtered.isEmpty()) {
            activeTask = null
            return
        }
        
        coroutineScope.launch {
            isRolling = true
            // Rolling animation effect
            if (filtered.size > 1) {
                repeat(10) { index ->
                    activeTask = filtered.filter { it != activeTask }.random()
                    delay(30L + (index * 15L)) // Gradually slows down
                }
            }
            activeTask = filtered.random()
            isRolling = false
        }
    }
    
    // Autoselect a default on first launch or change of filters
    LaunchedEffect(momentumList, selectedCategoryFilter, selectedTimeFilter) {
        val filtered = getFilteredList()
        if (filtered.isNotEmpty() && activeTask == null) {
            activeTask = filtered.random()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = BrightAmber
                        )
                        Text(
                            text = "Momentum Maker",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = DarkBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Ambient Background glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LavenderAccent.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            radius = 1200f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intro Text & Streak Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(BrightAmber.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = BrightAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "A Body in Motion Stays in Motion",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Overcome procrastination with instant, hyper-specific action steps.",
                                color = DarkTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Filters Section
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "CATEGORIES",
                            color = LavenderAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        // Category Horizontal list
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                val isSelected = selectedCategoryFilter == category
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategoryFilter = category
                                        activeTask = null // Reset so it chooses a new random matching task
                                    },
                                    label = { Text(category) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LavenderAccent.copy(alpha = 0.25f),
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkSurface,
                                        labelColor = DarkTextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = GlassBorder,
                                        selectedBorderColor = LavenderAccent
                                    )
                                )
                            }
                        }

                        Divider(color = GlassBorder, modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "TIME AVAILABLE",
                            color = LavenderAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        // Time Horizontal list
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timeFilters.forEach { filter ->
                                val isSelected = selectedTimeFilter == filter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTimeFilter = filter
                                        activeTask = null // Reset so it chooses a new random matching task
                                    },
                                    label = { Text(filter) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LavenderAccent.copy(alpha = 0.25f),
                                        selectedLabelColor = Color.White,
                                        containerColor = DarkSurface,
                                        labelColor = DarkTextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = GlassBorder,
                                        selectedBorderColor = LavenderAccent
                                    )
                                )
                            }
                        }
                    }
                }

                // Large Main Display Card
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, GlassBorderActive),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnimatedContent(
                            targetState = activeTask,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "ActiveTaskContent"
                        ) { task ->
                            if (task != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Tags Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Category Tag
                                        val catColor = when (task.category.lowercase()) {
                                            "clean" -> CategoryPersonal
                                            "mind" -> CategoryMind
                                            "body" -> CategoryHealth
                                            "admin" -> CategoryWork
                                            "social" -> CategoryCreative
                                            else -> CategoryGeneral
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(catColor.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                                                .border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = task.category.uppercase(),
                                                color = catColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        // Duration Tag
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(50.dp))
                                                .border(1.dp, GlassBorder, RoundedCornerShape(50.dp))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = null,
                                                    tint = DarkTextSecondary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "${task.durationMinutes} min",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Title Text
                                    Text(
                                        text = task.title,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = HighPriorityRed,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "No momentum tasks found",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Try clearing filters, adding custom tasks, or resetting default makers below.",
                                        color = DarkTextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large Action Button: Momentum Boost!
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isRolling) 0.95f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "BoostButtonScale"
                        )

                        Button(
                            onClick = { rollTask() },
                            enabled = !isRolling,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(BrightAmber, LavenderAccent)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                BrightAmber.copy(alpha = 0.2f),
                                                LavenderAccent.copy(alpha = 0.15f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = BrightAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = if (isRolling) "SELECTING..." else "MOMENTUM BOOST ⚡",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // Auxiliary Action Buttons (Roll another, Log completion)
                        if (activeTask != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Skip / Roll Another
                                TextButton(
                                    onClick = { rollTask() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = DarkTextSecondary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Roll Another", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Mark Completed
                                Button(
                                    onClick = {
                                        // Quick log or add as note
                                        activeTask?.let { task ->
                                            viewModel.addNote(
                                                title = "Momentum Boost Completed",
                                                content = "Completed: ${task.title} (${task.durationMinutes} min task from category: ${task.category})",
                                                category = "Mind",
                                                hashtags = "#momentumboost"
                                            )
                                            // Delightful Feedback
                                            Toast.makeText(context, "Logged completion to Notes space! 🌟", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LavenderAccent.copy(alpha = 0.15f),
                                        contentColor = LavenderAccent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, LavenderAccent.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Done!", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Collapsible section for all tasks (CRUD management)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAllTasksSection = !showAllTasksSection }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                    tint = LavenderAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Manage Tasks (${momentumList.size})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Icon(
                                imageVector = if (showAllTasksSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showAllTasksSection) "Collapse" else "Expand",
                                tint = DarkTextSecondary
                            )
                        }

                        AnimatedVisibility(
                            visible = showAllTasksSection,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Buttons Row inside manager
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Add Button
                                    Button(
                                        onClick = { showAddDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DarkSurfaceVariant
                                        ),
                                        border = BorderStroke(1.dp, GlassBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color.White
                                            )
                                            Text("Add Custom", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Reset Button
                                    Button(
                                        onClick = {
                                            viewModel.resetMomentumMakers()
                                            Toast.makeText(context, "Reset tasks to original defaults!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DarkSurfaceVariant
                                        ),
                                        border = BorderStroke(1.dp, GlassBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Restore,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = HighPriorityRed
                                            )
                                            Text("Reset List", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (momentumList.isEmpty()) {
                                    Text(
                                        text = "List is empty. Tap 'Add Custom' to add your momentum items!",
                                        color = DarkTextSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    // List of items
                                    momentumList.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    DarkSurface,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    val tagColor = when (item.category.lowercase()) {
                                                        "clean" -> CategoryPersonal
                                                        "mind" -> CategoryMind
                                                        "body" -> CategoryHealth
                                                        "admin" -> CategoryWork
                                                        "social" -> CategoryCreative
                                                        else -> CategoryGeneral
                                                    }
                                                    Text(
                                                        text = item.category.uppercase(),
                                                        color = tagColor,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "•",
                                                        color = DarkTextSecondary,
                                                        fontSize = 9.sp
                                                    )
                                                    Text(
                                                        text = "${item.durationMinutes} min",
                                                        color = DarkTextSecondary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteMomentumMaker(item.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete task",
                                                    tint = HighPriorityRed.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
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
    }

    // Add Custom Momentum Maker Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                customTitle = ""
            },
            containerColor = Color(0xFF1E1F22),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
            title = {
                Text(
                    text = "Add Momentum Maker",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("What needs to be done?") },
                        placeholder = { Text("e.g. Wipe down the kitchen counter") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = LavenderAccent,
                            unfocusedLabelColor = DarkTextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Selection
                    Text(
                        text = "Category",
                        color = LavenderAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        creationCategories.forEach { cat ->
                            val isSelected = customCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { customCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LavenderAccent.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurface,
                                    labelColor = DarkTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = GlassBorder,
                                    selectedBorderColor = LavenderAccent
                                )
                            )
                        }
                    }

                    // Duration Selection Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Time",
                            color = LavenderAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$customDuration min",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = customDuration.toFloat(),
                        onValueChange = { customDuration = it.toInt() },
                        valueRange = 1f..15f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = BrightAmber,
                            activeTrackColor = BrightAmber,
                            inactiveTrackColor = GlassBorder,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customTitle.isNotBlank()) {
                            viewModel.addMomentumMaker(
                                title = customTitle.trim(),
                                category = customCategory,
                                durationMinutes = customDuration
                            )
                            showAddDialog = false
                            customTitle = ""
                            Toast.makeText(context, "Added successfully! 🚀", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Add Task", color = LavenderAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        customTitle = ""
                    }
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}
