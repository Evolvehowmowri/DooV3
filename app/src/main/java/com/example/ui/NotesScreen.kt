package com.example.ui

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Note
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: DoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    // Quick Add thought state at the top (Mind Dump Canvas)
    var quickTitle by remember { mutableStateOf("") }
    var quickContent by remember { mutableStateOf("") }
    var quickCategoryInput by remember { mutableStateOf("") }
    var quickSubCategoryInput by remember { mutableStateOf("") }
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    
    // Dialog and active note edit states
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    // Export and Delete Confirm States
    var notesToDeleteAfterExport by remember { mutableStateOf<List<Note>>(emptyList()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Tag and Category auto-completion database
    val prebuiltTags = remember {
        listOf("Personal", "Work", "Idea", "Health", "Study", "ToDo", "Someday", "Reflection")
    }
    
    val allExistingTags = remember(notes) {
        notes.flatMap { note ->
            val tags = mutableListOf<String>()
            if (note.category.isNotBlank() && note.category != "General") tags.add(note.category)
            if (note.subCategory.isNotBlank()) tags.add(note.subCategory)
            if (note.hashtags.isNotBlank()) {
                tags.addAll(note.hashtags.split(",").map { it.trim() }.filter { it.isNotBlank() })
            }
            // Scan text also
            val regex = Regex("#([a-zA-Z0-9_\\-]+)")
            regex.findAll("${note.title} ${note.content}").forEach {
                tags.add(it.groupValues[1])
            }
            tags
        }.distinct().filter { it.isNotBlank() }
    }
    
    val availableTags = remember(allExistingTags) {
        (prebuiltTags + allExistingTags).distinct().sorted()
    }
    
    // Filter chip list state
    var selectedTagFilter by remember { mutableStateOf("All") }
    
    val activeFilterTags = remember(notes) {
        val tags = mutableListOf<String>()
        notes.forEach { note ->
            if (note.category.isNotBlank() && note.category != "General") tags.add(note.category)
            if (note.subCategory.isNotBlank()) tags.add(note.subCategory)
            if (note.hashtags.isNotBlank()) {
                tags.addAll(note.hashtags.split(",").map { it.trim() }.filter { it.isNotBlank() })
            }
        }
        notes.forEach { note ->
            val regex = Regex("#([a-zA-Z0-9_\\-]+)")
            regex.findAll("${note.title} ${note.content}").forEach {
                tags.add(it.groupValues[1])
            }
        }
        (listOf("All") + tags.distinct().sorted()).toList()
    }
    
    val filteredNotes = remember(notes, searchQuery, selectedTagFilter) {
        val afterSearch = if (searchQuery.isBlank()) {
            notes
        } else {
            notes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.subCategory.contains(searchQuery, ignoreCase = true) ||
                it.hashtags.contains(searchQuery, ignoreCase = true)
            }
        }
        
        if (selectedTagFilter == "All") {
            afterSearch
        } else {
            afterSearch.filter {
                it.category.equals(selectedTagFilter, ignoreCase = true) ||
                it.subCategory.equals(selectedTagFilter, ignoreCase = true) ||
                it.hashtags.split(",").map { it.trim() }.any { t -> t.equals(selectedTagFilter, ignoreCase = true) } ||
                it.title.contains("#$selectedTagFilter", ignoreCase = true) ||
                it.content.contains("#$selectedTagFilter", ignoreCase = true)
            }
        }
    }

    // Typing tag detection for Content
    val contentLastWord = quickContent.split(Regex("\\s+")).lastOrNull() ?: ""
    val isContentTypingHashtag = contentLastWord.startsWith("#")
    val contentHashtagQuery = if (isContentTypingHashtag) contentLastWord.substring(1) else ""
    val contentMatchingTags = remember(contentHashtagQuery, availableTags) {
        if (isContentTypingHashtag) {
            availableTags.filter { it.contains(contentHashtagQuery, ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    // Typing tag detection for Title
    val titleLastWord = quickTitle.split(Regex("\\s+")).lastOrNull() ?: ""
    val isTitleTypingHashtag = titleLastWord.startsWith("#")
    val titleHashtagQuery = if (isTitleTypingHashtag) titleLastWord.substring(1) else ""
    val titleMatchingTags = remember(titleHashtagQuery, availableTags) {
        if (isTitleTypingHashtag) {
            availableTags.filter { it.contains(titleHashtagQuery, ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    val onTagSelectedInContent = { tag: String ->
        val words = quickContent.split(" ").toMutableList()
        if (words.isNotEmpty() && words.last().startsWith("#")) {
            words[words.lastIndex] = "#$tag"
            quickContent = words.joinToString(" ") + " "
        } else {
            quickContent = if (quickContent.endsWith(" ") || quickContent.isEmpty()) {
                quickContent + "#$tag "
            } else {
                quickContent + " #$tag "
            }
        }
    }

    val onTagSelectedInTitle = { tag: String ->
        val words = quickTitle.split(" ").toMutableList()
        if (words.isNotEmpty() && words.last().startsWith("#")) {
            words[words.lastIndex] = "#$tag"
            quickTitle = words.joinToString(" ") + " "
        } else {
            quickTitle = if (quickTitle.endsWith(" ") || quickTitle.isEmpty()) {
                quickTitle + "#$tag "
            } else {
                quickTitle + " #$tag "
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Mind Dump & Notes",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        Text(
            text = "Pour out your thoughts, save notes, and keep your head clear.",
            color = DarkTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Section 1: The Quick Mind Dump Canvas
            item {
                val categorySpecs = remember {
                    listOf(
                        NoteCategorySpec("Personal", "🏠", Color(0xFFFFB74D), "Personal thoughts & life"),
                        NoteCategorySpec("Work", "💼", Color(0xFFD0BCFF), "Tasks, projects & professional"),
                        NoteCategorySpec("Idea", "💡", Color(0xFFFFD740), "Inspirations & brainstorms"),
                        NoteCategorySpec("Health", "🌱", Color(0xFFA8C7FA), "Fitness, wellness & habits"),
                        NoteCategorySpec("Study", "🎓", Color(0xFFFF8A65), "Learning & self-improvement"),
                        NoteCategorySpec("ToDo", "✅", Color(0xFFF2B8B5), "Action items & quick lists"),
                        NoteCategorySpec("Someday", "⏳", Color(0xFF4DD0E1), "Future dreams & backlogs"),
                        NoteCategorySpec("Reflection", "🧠", Color(0xFFE040FB), "Mindfulness & review")
                    )
                }

                val detectedHashtags = remember(quickTitle, quickContent) {
                    val allText = "$quickTitle $quickContent"
                    val regex = Regex("#([a-zA-Z0-9_\\-]+)")
                    regex.findAll(allText).map { it.groupValues[1] }.toList().distinct()
                }

                // Smart auto-categorization fallback
                LaunchedEffect(detectedHashtags) {
                    if (quickCategoryInput.isBlank() || prebuiltTags.contains(quickCategoryInput)) {
                        val matchedTag = prebuiltTags.find { tag ->
                            detectedHashtags.any { it.equals(tag, ignoreCase = true) }
                        }
                        if (matchedTag != null && !quickCategoryInput.equals(matchedTag, ignoreCase = true)) {
                            quickCategoryInput = matchedTag
                        }
                    }
                }

                val selectedSpec = remember(quickCategoryInput) {
                    categorySpecs.find { it.name.equals(quickCategoryInput, ignoreCase = true) }
                }
                val activeCategoryColor = selectedSpec?.color ?: LavenderAccent
                val cardBorderColor = if (selectedSpec != null) activeCategoryColor.copy(alpha = 0.5f) else GlassBorder
                val cardBgColor = if (selectedSpec != null) activeCategoryColor.copy(alpha = 0.05f) else Color.Transparent

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .background(cardBgColor)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header block
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Quick Dump",
                                    tint = activeCategoryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Instant Mind Dump",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Clear your mental workspace instantly",
                                        color = DarkTextSecondary.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            // Help prebuilt tags row toggle
                            TextButton(
                                onClick = { isAdvancedExpanded = !isAdvancedExpanded },
                                colors = ButtonDefaults.textButtonColors(contentColor = activeCategoryColor),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAdvancedExpanded) "Simple Details" else "Add Tags / Sub",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Category visual selector
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MAIN CATEGORY",
                                    color = DarkTextSecondary.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                if (quickCategoryInput.isNotBlank()) {
                                    Text(
                                        text = "Selected: ${selectedSpec?.icon ?: "📁"} $quickCategoryInput",
                                        color = activeCategoryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(categorySpecs) { spec ->
                                    val isSelected = quickCategoryInput.equals(spec.name, ignoreCase = true)
                                    val chipBg = if (isSelected) spec.color.copy(alpha = 0.25f) else DarkSurfaceVariant
                                    val chipBorder = if (isSelected) spec.color else GlassBorder
                                    val textColor = if (isSelected) Color.White else DarkTextSecondary
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(chipBg)
                                            .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                            .clickable {
                                                quickCategoryInput = if (isSelected) "" else spec.name
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(spec.icon, fontSize = 12.sp)
                                            Text(
                                                text = spec.name,
                                                color = textColor,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Title / Topic input field
                        OutlinedTextField(
                            value = quickTitle,
                            onValueChange = { quickTitle = it },
                            placeholder = { Text("Topic / Title (Optional)", color = DarkTextSecondary.copy(alpha = 0.7f), fontSize = 13.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Label,
                                    contentDescription = null,
                                    tint = activeCategoryColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (quickTitle.isNotEmpty()) {
                                    IconButton(onClick = { quickTitle = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear Title", tint = DarkTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = activeCategoryColor,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("quick_note_title")
                        )

                        // Title matching hashtags auto-complete dropdown
                        if (titleMatchingTags.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkBg),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                            ) {
                                LazyRow(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(titleMatchingTags) { tag ->
                                        Text(
                                            text = "#$tag",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .background(activeCategoryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .clickable { onTagSelectedInTitle(tag) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Thoughts Content input field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = quickContent,
                                onValueChange = { quickContent = it },
                                placeholder = { Text("Pour your thoughts here...", color = DarkTextSecondary.copy(alpha = 0.7f), fontSize = 13.sp) },
                                minLines = 3,
                                maxLines = 6,
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier.size(48.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Create,
                                            contentDescription = null,
                                            tint = activeCategoryColor.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(top = 16.dp).size(18.dp)
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (quickContent.isNotEmpty()) {
                                        IconButton(onClick = { quickContent = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Content", tint = DarkTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = activeCategoryColor,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("quick_note_content")
                            )
                            
                            // Character count and auto-detection tip row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (quickContent.isBlank()) "Type '#' to automatically suggest and parse hashtags" else "Pro-tip: type #tags directly",
                                    color = DarkTextSecondary.copy(alpha = 0.4f),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "${quickContent.length} chars",
                                    color = if (quickContent.length > 300) activeCategoryColor else DarkTextSecondary.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Content matching hashtags auto-complete dropdown
                        if (contentMatchingTags.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkBg),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                            ) {
                                LazyRow(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(contentMatchingTags) { tag ->
                                        Text(
                                            text = "#$tag",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .background(activeCategoryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .clickable { onTagSelectedInContent(tag) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Advanced Attributes (Sub-category and Hashtags)
                        AnimatedVisibility(
                            visible = isAdvancedExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = quickSubCategoryInput,
                                        onValueChange = { quickSubCategoryInput = it },
                                        placeholder = { Text("Sub-category (Optional)", color = DarkTextSecondary.copy(alpha = 0.6f), fontSize = 12.sp) },
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.BookmarkBorder,
                                                contentDescription = null,
                                                tint = LightBlueAccent.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = LightBlueAccent,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedContainerColor = DarkSurfaceVariant,
                                            unfocusedContainerColor = DarkSurfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    OutlinedTextField(
                                        value = quickCategoryInput,
                                        onValueChange = { quickCategoryInput = it },
                                        placeholder = { Text("Custom Category", color = DarkTextSecondary.copy(alpha = 0.6f), fontSize = 12.sp) },
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = activeCategoryColor.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = activeCategoryColor,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedContainerColor = DarkSurfaceVariant,
                                            unfocusedContainerColor = DarkSurfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                // Real-time parsed hashtags visualization
                                if (detectedHashtags.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "AUTO-DETECTED HASHTAGS:",
                                            color = DarkTextSecondary.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(detectedHashtags) { hashtag ->
                                                Box(
                                                    modifier = Modifier
                                                        .background(activeCategoryColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                        .border(0.5.dp, activeCategoryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text("#", color = activeCategoryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        Text(hashtag, color = Color.White, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Lower actions panel (Free My Mind & Clear Draft)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Clear Draft button if content exists
                            if (quickContent.isNotBlank() || quickTitle.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        quickTitle = ""
                                        quickContent = ""
                                        quickCategoryInput = ""
                                        quickSubCategoryInput = ""
                                        isAdvancedExpanded = false
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = HighPriorityRed
                                    ),
                                    border = BorderStroke(1.dp, HighPriorityRed.copy(alpha = 0.3f)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear draft", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", fontSize = 13.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    if (quickContent.isNotBlank()) {
                                        val title = if (quickTitle.isBlank()) "Untitled Dump" else quickTitle.trim()
                                        
                                        // Parse tags automatically from title and content
                                        val allText = "$title ${quickContent.trim()}"
                                        val regex = Regex("#([a-zA-Z0-9_\\-]+)")
                                        val parsedTags = regex.findAll(allText).map { it.groupValues[1] }.toList().distinct()
                                        
                                        val finalCategory = if (quickCategoryInput.isNotBlank()) {
                                            quickCategoryInput.trim()
                                        } else if (parsedTags.isNotEmpty()) {
                                            parsedTags[0].replaceFirstChar { it.uppercase() }
                                        } else {
                                            "General"
                                        }

                                        val finalSubCategory = if (quickSubCategoryInput.isNotBlank()) {
                                            quickSubCategoryInput.trim()
                                        } else if (parsedTags.size > 1) {
                                            parsedTags[1]
                                        } else {
                                            ""
                                        }

                                        val finalHashtags = parsedTags.joinToString(", ")

                                        viewModel.addNote(
                                            title = title,
                                            content = quickContent.trim(),
                                            category = finalCategory,
                                            subCategory = finalSubCategory,
                                            hashtags = finalHashtags
                                        )
                                        
                                        quickTitle = ""
                                        quickContent = ""
                                        quickCategoryInput = ""
                                        quickSubCategoryInput = ""
                                        isAdvancedExpanded = false
                                    }
                                },
                                enabled = quickContent.isNotBlank(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeCategoryColor,
                                    contentColor = Color.Black,
                                    disabledContainerColor = DarkSurfaceVariant,
                                    disabledContentColor = DarkTextSecondary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("save_quick_note")
                            ) {
                                Icon(
                                    imageVector = if (selectedSpec != null) Icons.Default.OfflineBolt else Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (quickCategoryInput.isNotBlank()) "Dump to $quickCategoryInput" else "Free My Mind",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Section 1.5: Note Export & Organisation Hub
            item {
                NoteExportHubCard(
                    notes = notes,
                    viewModel = viewModel
                )
            }

            // Section 2: Search & Filter past thoughts
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search your mind dumps...", color = DarkTextSecondary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LavenderAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notes_search_input"),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = DarkTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )
                    
                    // Filter tag chips bar
                    if (activeFilterTags.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeFilterTags) { tag ->
                                val isSelected = tag == selectedTagFilter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) LavenderAccent else DarkSurface)
                                        .border(1.dp, if (isSelected) LavenderAccent else GlassBorder, RoundedCornerShape(10.dp))
                                        .clickable { selectedTagFilter = tag }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (tag == "All") "All" else "#$tag",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Notes List Header
            item {
                Text(
                    text = if (selectedTagFilter == "All") "Saved Mind Dumps (${filteredNotes.size})" else "Notes tag: #$selectedTagFilter (${filteredNotes.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Section 4: Grid or List of Notes
            if (filteredNotes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No thoughts captured yet. Pour your mind above!" else "No matching thoughts found.",
                            color = DarkTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredNotes) { note ->
                    NoteCard(
                        note = note,
                        onEdit = {
                            noteToEdit = note
                            showEditDialog = true
                        },
                        onDelete = {
                            viewModel.deleteNote(note.id)
                        },
                        onShareTxt = {
                            val txt = formatNoteContentForSharing(note, "txt", context)
                            NoteExporter.shareNoteAsFile(context, note.title, txt, "txt")
                            notesToDeleteAfterExport = listOf(note)
                            showDeleteConfirmDialog = true
                        },
                        onShareMd = {
                            val md = formatNoteContentForSharing(note, "md", context)
                            NoteExporter.shareNoteAsFile(context, note.title, md, "md")
                            notesToDeleteAfterExport = listOf(note)
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
    }

    // Export Deletion Confirmation Dialog
    if (showDeleteConfirmDialog && notesToDeleteAfterExport.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                notesToDeleteAfterExport = emptyList()
            },
            containerColor = Color(0xFF1E1F22),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(28.dp)),
            title = {
                Text(
                    text = "Delete Exported Notes?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Do you want to delete the exported notes really? Only the successfully exported note(s) will be deleted. This action cannot be undone.",
                    color = DarkTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        notesToDeleteAfterExport.forEach { n ->
                            viewModel.deleteNote(n.id)
                        }
                        showDeleteConfirmDialog = false
                        notesToDeleteAfterExport = emptyList()
                    }
                ) {
                    Text("Yes, Delete Them", color = HighPriorityRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        notesToDeleteAfterExport = emptyList()
                    }
                ) {
                    Text("No, Keep Them", color = Color.White)
                }
            }
        )
    }

    // Edit/Update Dialog
    if (showEditDialog && noteToEdit != null) {
        var editTitle by remember { mutableStateOf(noteToEdit!!.title) }
        var editContent by remember { mutableStateOf(noteToEdit!!.content) }
        var editCategory by remember { mutableStateOf(noteToEdit!!.category) }
        var editSubCategory by remember { mutableStateOf(noteToEdit!!.subCategory) }
        var editHashtags by remember { mutableStateOf(noteToEdit!!.hashtags) }

        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                noteToEdit = null
            },
            containerColor = Color(0xFF1E1F22),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(28.dp)),
            title = {
                Text(
                    text = "Refine Dumped Thought",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title / Topic", color = DarkTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LavenderAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Content / Thoughts", color = DarkTextSecondary) },
                        minLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LavenderAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Category", color = DarkTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LavenderAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DarkBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editSubCategory,
                        onValueChange = { editSubCategory = it },
                        label = { Text("Sub-category", color = DarkTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LightBlueAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DarkBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editHashtags,
                        onValueChange = { editHashtags = it },
                        label = { Text("Tags (comma separated)", color = DarkTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LavenderAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DarkBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editContent.isNotBlank()) {
                            viewModel.updateNote(
                                noteToEdit!!.copy(
                                    title = editTitle.trim(),
                                    content = editContent.trim(),
                                    category = if (editCategory.isBlank()) "General" else editCategory.trim(),
                                    subCategory = editSubCategory.trim(),
                                    hashtags = editHashtags.trim()
                                )
                            )
                        }
                        showEditDialog = false
                        noteToEdit = null
                    },
                    enabled = editContent.isNotBlank()
                ) {
                    Text("Save Changes", color = LavenderAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        noteToEdit = null
                    }
                ) {
                    Text("Discard", color = DarkTextSecondary)
                }
            }
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShareTxt: () -> Unit,
    onShareMd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val formattedTime = remember(note.createdAt) {
        DateUtils.formatDateTime(
            context,
            note.createdAt,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTime,
                        color = DarkTextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
                
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Note Options",
                            tint = DarkTextSecondary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E1F22)).border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Note", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as .md", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onShareMd()
                            },
                            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as .txt", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onShareTxt()
                            },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = LightBlueAccent, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Note", color = HighPriorityRed, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = HighPriorityRed, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            // Dynamic Category Color & Icon
            val categoryColor = remember(note.category) {
                when (note.category.trim().lowercase()) {
                    "personal" -> Color(0xFFFFB74D)
                    "work" -> Color(0xFFD0BCFF)
                    "idea" -> Color(0xFFFFD740)
                    "health" -> Color(0xFFA8C7FA)
                    "study" -> Color(0xFFFF8A65)
                    "todo" -> Color(0xFFF2B8B5)
                    "someday" -> Color(0xFF4DD0E1)
                    "reflection" -> Color(0xFFE040FB)
                    else -> LavenderAccent
                }
            }
            val categoryIcon = remember(note.category) {
                when (note.category.trim().lowercase()) {
                    "personal" -> "🏠"
                    "work" -> "💼"
                    "idea" -> "💡"
                    "health" -> "🌱"
                    "study" -> "🎓"
                    "todo" -> "✅"
                    "someday" -> "⏳"
                    "reflection" -> "🧠"
                    else -> "📁"
                }
            }

            // Beautiful Category and Subcategory Badges
            if ((note.category.isNotBlank() && note.category != "General") || note.subCategory.isNotBlank() || note.hashtags.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    if (note.category.isNotBlank() && note.category != "General") {
                        Box(
                            modifier = Modifier
                                .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, categoryColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$categoryIcon #${note.category}",
                                color = categoryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (note.subCategory.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(LightBlueAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, LightBlueAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🔖 ${note.subCategory}",
                                color = LightBlueAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Hashtags badges
                    if (note.hashtags.isNotBlank()) {
                        val tags = note.hashtags.split(",").map { it.trim() }.filter { it.isNotBlank() && it != note.category && it != note.subCategory }
                        tags.take(3).forEach { t ->
                            Box(
                                modifier = Modifier
                                    .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                                    .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "#$t",
                                    color = DarkTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Text(
                    text = note.content,
                    color = DarkTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.content.length > 150) {
                Text(
                    text = if (isExpanded) "Show Less" else "Read Full Dump...",
                    color = LavenderAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(top = 2.dp)
                )
            }
            

        }
    }
}

/**
 * Highly functional and sophisticated Markdown / TXT Exporter Format
 * Matches exactly:
 * hashtag or categories
 * sub category 
 * date time 
 * topic
 */
fun formatNoteContentForSharing(note: Note, format: String, context: Context): String {
    val formattedTime = DateUtils.formatDateTime(
        context,
        note.createdAt,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
    )
    val cat = if (note.category.isNotBlank()) note.category else "General"
    val sub = note.subCategory
    val hash = note.hashtags

    return if (format == "md") {
        buildString {
            appendLine("# #$cat")
            if (sub.isNotBlank()) {
                appendLine("## $sub")
            } else {
                appendLine("## Uncategorized Sub")
            }
            appendLine("### $formattedTime")
            appendLine("#### Topic: ${note.title}")
            if (hash.isNotBlank()) {
                appendLine("##### Tags: $hash")
            }
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(note.content)
        }
    } else {
        buildString {
            appendLine("#$cat")
            if (sub.isNotBlank()) {
                appendLine(sub)
            } else {
                appendLine("Uncategorized Sub")
            }
            appendLine(formattedTime)
            appendLine(note.title)
            if (hash.isNotBlank()) {
                appendLine("Tags: $hash")
            }
            appendLine()
            appendLine("========================================")
            appendLine()
            appendLine(note.content)
        }
    }
}

data class NoteCategorySpec(
    val name: String,
    val icon: String,
    val color: Color,
    val description: String
)

@Composable
fun NoteExportHubCard(
    notes: List<Note>,
    viewModel: DoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    
    // Filters State
    var selectedDateRange by remember { mutableStateOf("All Time") }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var searchTopicQuery by remember { mutableStateOf("") }
    
    // Strategy & Format State
    var compileStrategy by remember { mutableStateOf("ZIP") } // "ZIP" or "SINGLE"
    var format by remember { mutableStateOf("md") } // "md" or "txt"
    var singleGrouping by remember { mutableStateOf("Category") } // "Category", "Date", "None"
    
    // Post export state
    var deleteAfterExport by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var notesPendingDeletion by remember { mutableStateOf<List<Note>>(emptyList()) }

    // Dynamic Filter Discovery
    val allDiscoveredCategories = remember(notes) {
        notes.map { it.category.trim().ifBlank { "General" } }.distinct().sorted()
    }
    val allDiscoveredTags = remember(notes) {
        notes.flatMap { note ->
            val list = mutableListOf<String>()
            if (note.hashtags.isNotBlank()) {
                list.addAll(note.hashtags.split(",").map { it.trim() }.filter { it.isNotBlank() })
            }
            list
        }.distinct().sorted()
    }

    // Active matching notes based on filters
    val filteredExportNotes = remember(notes, selectedDateRange, selectedCategories, selectedTags, searchTopicQuery) {
        notes.filter { note ->
            // 1. Date filter
            val dateMatches = when (selectedDateRange) {
                "Today" -> android.text.format.DateUtils.isToday(note.createdAt)
                "Last 7 Days" -> note.createdAt >= System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                "Last 30 Days" -> note.createdAt >= System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
                else -> true
            }

            // 2. Category filter
            val categoryMatches = if (selectedCategories.isEmpty()) {
                true
            } else {
                selectedCategories.contains(note.category.trim().ifBlank { "General" })
            }

            // 3. Tag filter
            val tagMatches = if (selectedTags.isEmpty()) {
                true
            } else {
                val noteTags = note.hashtags.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
                selectedTags.any { tag -> noteTags.contains(tag.lowercase()) }
            }

            // 4. Topic query filter
            val topicMatches = if (searchTopicQuery.isBlank()) {
                true
            } else {
                note.title.contains(searchTopicQuery, ignoreCase = true) ||
                note.content.contains(searchTopicQuery, ignoreCase = true)
            }

            dateMatches && categoryMatches && tagMatches && topicMatches
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row (Clickable to toggle expand)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(LightBlueAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "Export Hub",
                        tint = LightBlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conscious Mind Exporter Hub",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sort, filter, compile, or export folder-structured vaults",
                        color = DarkTextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Filter count badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (filteredExportNotes.isNotEmpty()) LavenderAccent.copy(alpha = 0.2f) else DarkSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${filteredExportNotes.size} Dumps Selected",
                        color = if (filteredExportNotes.isNotEmpty()) LavenderAccent else DarkTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Panel",
                    tint = DarkTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GlassBorder)
                    )

                    if (notes.isEmpty()) {
                        Text(
                            text = "No thoughts in memory yet. Capture some mind dumps above to begin organizing your vault!",
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // SECTION 1: REFINEMENT FILTERS
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "1. REFINEMENT FILTERS",
                                color = LightBlueAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            // Date filter chip row
                            Text(text = "Date Interval", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                listOf("All Time", "Today", "Last 7 Days", "Last 30 Days").forEach { range ->
                                    val isSelected = selectedDateRange == range
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) LightBlueAccent.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                            .border(1.dp, if (isSelected) LightBlueAccent else GlassBorder, RoundedCornerShape(8.dp))
                                            .clickable { selectedDateRange = range }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = range, color = if (isSelected) Color.White else DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            // Dynamic Categories Multi-Select
                            if (allDiscoveredCategories.isNotEmpty()) {
                                Text(text = "Filter by Category (${selectedCategories.size} selected)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    allDiscoveredCategories.forEach { cat ->
                                        val isSelected = selectedCategories.contains(cat)
                                        val count = notes.count { it.category.trim().ifBlank { "General" } == cat }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) LavenderAccent.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                                .border(1.dp, if (isSelected) LavenderAccent else GlassBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedCategories = if (isSelected) {
                                                        selectedCategories - cat
                                                    } else {
                                                        selectedCategories + cat
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(10.dp))
                                                }
                                                Text(text = "$cat ($count)", color = if (isSelected) Color.White else DarkTextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Dynamic Hashtags Multi-Select
                            if (allDiscoveredTags.isNotEmpty()) {
                                Text(text = "Filter by Hashtags (${selectedTags.size} selected)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    allDiscoveredTags.forEach { tag ->
                                        val isSelected = selectedTags.contains(tag)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFFFFB74D).copy(alpha = 0.2f) else DarkSurfaceVariant)
                                                .border(1.dp, if (isSelected) Color(0xFFFFB74D) else GlassBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedTags = if (isSelected) {
                                                        selectedTags - tag
                                                    } else {
                                                        selectedTags + tag
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(text = "#$tag", color = if (isSelected) Color.White else DarkTextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Search Query Refinement Input
                            OutlinedTextField(
                                value = searchTopicQuery,
                                onValueChange = { searchTopicQuery = it },
                                placeholder = { Text("Filter keywords in title/content...", color = DarkTextSecondary.copy(alpha = 0.6f), fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = LightBlueAccent,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(16.dp)) },
                                trailingIcon = {
                                    if (searchTopicQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchTopicQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = DarkTextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // SECTION 2: EXPORT ARCHITECTURE
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "2. EXPORT ARCHITECTURE",
                                color = LavenderAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Text(text = "Target Structure Strategy", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            
                            // ZIP structured vs Combined single file cards
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // ZIP Strategy Button
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (compileStrategy == "ZIP") LavenderAccent.copy(alpha = 0.08f) else DarkSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color = if (compileStrategy == "ZIP") LavenderAccent else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { compileStrategy = "ZIP" }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(16.dp))
                                            Text("Folder ZIP Vault", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Creates structured zip folder hierarchy: Category / Subcategory / Date_Title.md. Great for Obsidian!",
                                            color = DarkTextSecondary,
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }

                                // Single Combined strategy Button
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (compileStrategy == "SINGLE") LightBlueAccent.copy(alpha = 0.08f) else DarkSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color = if (compileStrategy == "SINGLE") LightBlueAccent else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { compileStrategy = "SINGLE" }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Description, contentDescription = null, tint = LightBlueAccent, modifier = Modifier.size(16.dp))
                                            Text("Combined File", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Compiles all matching dumps into a single scrollable document with premium section dividers.",
                                            color = DarkTextSecondary,
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }

                            // Dynamic Grouping Configuration if "SINGLE"
                            if (compileStrategy == "SINGLE") {
                                Text(text = "Compile Grouping Hierarchy", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(
                                        Pair("Category", "By Category"),
                                        Pair("Date", "By Date"),
                                        Pair("None", "Chronological")
                                    ).forEach { (strat, label) ->
                                        val isSelected = singleGrouping == strat
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) LightBlueAccent.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                                .border(1.dp, if (isSelected) LightBlueAccent else GlassBorder, RoundedCornerShape(8.dp))
                                                .clickable { singleGrouping = strat }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = label, color = if (isSelected) Color.White else DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }

                            // Output File Format selector
                            Text(text = "Output Markdown or Plaintext", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    Pair("md", "📄 .MD (Markdown Standard)"),
                                    Pair("txt", "✏️ .TXT (Simple Plaintext)")
                                ).forEach { (fmt, label) ->
                                    val isSelected = format == fmt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) LavenderAccent.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                            .border(1.dp, if (isSelected) LavenderAccent else GlassBorder, RoundedCornerShape(10.dp))
                                            .clickable { format = fmt }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = label, color = if (isSelected) Color.White else DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // SECTION 3: AFTERCARE & MEMORY CONTROL
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Wipe after successful export",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Deletes successfully shared thoughts from local app memory immediately. Best for high-security journaling.",
                                        color = DarkTextSecondary,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                                Switch(
                                    checked = deleteAfterExport,
                                    onCheckedChange = { deleteAfterExport = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = HighPriorityRed,
                                        checkedTrackColor = HighPriorityRed.copy(alpha = 0.3f),
                                        uncheckedThumbColor = DarkTextSecondary,
                                        uncheckedTrackColor = DarkSurface
                                    )
                                )
                            }
                        }

                        // ACTION BUTTON
                        Button(
                            onClick = {
                                if (filteredExportNotes.isNotEmpty()) {
                                    val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                    if (compileStrategy == "ZIP") {
                                        val filesToZip = filteredExportNotes.map { note ->
                                            val cat = note.category.trim().ifBlank { "General" }
                                            val sub = note.subCategory.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                                            val cleanSubFolder = if (sub.isBlank()) "" else "$sub/"
                                            
                                            val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(note.createdAt))
                                            val cleanTitle = note.title.trim()
                                                .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
                                                .replace(Regex("\\s+"), "_")
                                                .take(30)
                                                .ifBlank { "MindDump" }
                                            
                                            val filename = "${formattedDate}_${cleanTitle}.$format"
                                            val relativePath = "$cat/$cleanSubFolder$filename"
                                            val content = formatNoteContentForSharing(note, format, context)
                                            
                                            Pair(relativePath, content)
                                        }
                                        NoteExporter.shareZipFile(context, "MindDumps_Vault_$dateStr", filesToZip)
                                        if (deleteAfterExport) {
                                            notesPendingDeletion = filteredExportNotes
                                            showDeleteConfirmation = true
                                        }
                                    } else {
                                        val combinedText = when (singleGrouping) {
                                            "Category" -> {
                                                filteredExportNotes.groupBy { it.category.trim().ifBlank { "General" } }
                                                    .map { (cat, catNotes) ->
                                                        val header = if (format == "md") "# Category: $cat\n\n" else "=== CATEGORY: $cat ===\n\n"
                                                        val notesBody = catNotes.joinToString("\n\n---\n\n") { note ->
                                                            formatNoteContentForSharing(note, format, context)
                                                        }
                                                        "$header$notesBody"
                                                    }.joinToString("\n\n========================================\n\n")
                                            }
                                            "Date" -> {
                                                filteredExportNotes.groupBy {
                                                    java.text.SimpleDateFormat("yyyy-MM-dd (EEEE)", java.util.Locale.getDefault()).format(java.util.Date(it.createdAt))
                                                }.map { (dateLabel, dateNotes) ->
                                                    val header = if (format == "md") "# Journal Date: $dateLabel\n\n" else "=== JOURNAL DATE: $dateLabel ===\n\n"
                                                    val notesBody = dateNotes.joinToString("\n\n---\n\n") { note ->
                                                        formatNoteContentForSharing(note, format, context)
                                                    }
                                                    "$header$notesBody"
                                                }.joinToString("\n\n========================================\n\n")
                                            }
                                            else -> {
                                                filteredExportNotes.joinToString("\n\n---\n\n") { note ->
                                                    formatNoteContentForSharing(note, format, context)
                                                }
                                            }
                                        }
                                        NoteExporter.shareNoteAsFile(context, "Compiled_Mind_Dumps_$dateStr", combinedText, format)
                                        if (deleteAfterExport) {
                                            notesPendingDeletion = filteredExportNotes
                                            showDeleteConfirmation = true
                                        }
                                    }
                                }
                            },
                            enabled = filteredExportNotes.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (compileStrategy == "ZIP") LavenderAccent else LightBlueAccent,
                                contentColor = Color.Black,
                                disabledContainerColor = DarkSurfaceVariant,
                                disabledContentColor = DarkTextSecondary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compile & Export ${filteredExportNotes.size} Thoughts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Memory Purge Safe Confirmation Dialog
    if (showDeleteConfirmation && notesPendingDeletion.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                notesPendingDeletion = emptyList()
            },
            containerColor = Color(0xFF1E1F22),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(28.dp)),
            title = {
                Text(
                    text = "Confirm Permanent Memory Purge",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "You requested to clear local app memory of these notes after exporting. This will permanently erase ${notesPendingDeletion.size} exported thoughts from local database storage. Do you wish to proceed?",
                    color = DarkTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        notesPendingDeletion.forEach { note ->
                            viewModel.deleteNote(note.id)
                        }
                        showDeleteConfirmation = false
                        notesPendingDeletion = emptyList()
                        android.widget.Toast.makeText(context, "Memory purged successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Yes, Erase Them", color = HighPriorityRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        notesPendingDeletion = emptyList()
                    }
                ) {
                    Text("Keep in Memory", color = Color.White)
                }
            }
        )
    }
}

