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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                    tint = LavenderAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Instant Mind Dump",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Help prebuilt tags row toggle
                            TextButton(
                                onClick = { isAdvancedExpanded = !isAdvancedExpanded },
                                colors = ButtonDefaults.textButtonColors(contentColor = LavenderAccent)
                            ) {
                                Icon(
                                    imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAdvancedExpanded) "Simple Mode" else "Categories", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Got something on your mind? Capture it instantly before it slips away. Type '#' to auto-suggest categories.",
                            color = DarkTextSecondary,
                            fontSize = 11.sp
                        )

                        // General Quick Tag Helper Row
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Prebuilt Categories (tap to insert):",
                                color = DarkTextSecondary.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(prebuiltTags) { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                                            .clickable { onTagSelectedInContent(tag) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("#$tag", color = LavenderAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = quickTitle,
                            onValueChange = { quickTitle = it },
                            placeholder = { Text("Topic / Title (Optional)", color = DarkTextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = LavenderAccent,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("quick_note_title")
                        )

                        // Title hashtag suggestion drop list
                        if (titleMatchingTags.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkBg),
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
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
                                                .background(LavenderAccent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                .clickable { onTagSelectedInTitle(tag) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = quickContent,
                            onValueChange = { quickContent = it },
                            placeholder = { Text("Pour your thoughts here...", color = DarkTextSecondary, fontSize = 12.sp) },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = LavenderAccent,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("quick_note_content")
                        )

                        // Content hashtag suggestion drop list
                        if (contentMatchingTags.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkBg),
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
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
                                                .background(LavenderAccent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                .clickable { onTagSelectedInContent(tag) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Advanced inputs (explicit category and subcategory settings)
                        AnimatedVisibility(visible = isAdvancedExpanded) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = quickCategoryInput,
                                        onValueChange = { quickCategoryInput = it },
                                        placeholder = { Text("Category (Optional)", color = DarkTextSecondary, fontSize = 11.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = LavenderAccent,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedContainerColor = DarkSurfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = quickSubCategoryInput,
                                        onValueChange = { quickSubCategoryInput = it },
                                        placeholder = { Text("Sub-category (Optional)", color = DarkTextSecondary, fontSize = 11.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = LightBlueAccent,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedContainerColor = DarkSurfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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
                                        parsedTags[0]
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
                                containerColor = LavenderAccent,
                                contentColor = Color.Black,
                                disabledContainerColor = DarkSurfaceVariant,
                                disabledContentColor = DarkTextSecondary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("save_quick_note")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Free My Mind", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
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
                    modifier = Modifier.fillMaxWidth()
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
                                .background(LavenderAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, LavenderAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📁 #${note.category}",
                                color = LavenderAccent,
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
            
            // Inline Export Actions row for ultimate accessibility
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.textButtonColors(contentColor = LavenderAccent),
                    modifier = Modifier.height(32.dp).padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onShareTxt,
                    colors = ButtonDefaults.textButtonColors(contentColor = LightBlueAccent),
                    modifier = Modifier.height(32.dp).padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TXT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onShareMd,
                    colors = ButtonDefaults.textButtonColors(contentColor = LavenderAccent),
                    modifier = Modifier.height(32.dp).padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
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
