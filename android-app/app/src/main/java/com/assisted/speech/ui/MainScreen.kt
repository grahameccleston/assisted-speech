package com.assisted.speech.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assisted.speech.data.PhraseItem
import com.assisted.speech.data.PhraseTreeData
import com.assisted.speech.data.PhraseType

val BackgroundDark = Color(0xFF0F172A)
val CardDark = Color(0xFF1E293B)
val BorderDark = Color(0xFF334155)
val AccentBlue = Color(0xFF2563EB)
val AccentGreen = Color(0xFF16A34A)
val AccentAmber = Color(0xFFD97706)
val AccentRed = Color(0xFFDC2626)
val TextLight = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)

@Composable
fun MainScreen(viewModel: SpeechViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = BackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // 1. TOP SENTENCE BAR: 2 rows to ensure text and action buttons are spacious
            SentenceAccumulatorBar(
                displayText = uiState.displaySentence,
                onSpeak = { viewModel.speakCurrentSentence() },
                onBackspace = { viewModel.backspaceSentence() },
                onClear = { viewModel.clearSentence() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. MODE NAVIGATION TABS
            ModeTabs(
                currentMode = uiState.currentMode,
                onModeSelected = { viewModel.setMode(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. CURRENT VIEW BODY
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.currentMode) {
                    AppMode.PHRASES -> HierarchicalPhrasesView(
                        nodes = uiState.currentTierNodes,
                        breadcrumbs = uiState.breadcrumbLabels,
                        isAtRoot = uiState.navigationStack.isEmpty(),
                        onNavigateUp = { viewModel.onTreeNavigateUp() },
                        onResetToRoot = { viewModel.onTreeResetToRoot() },
                        onSelectNode = { viewModel.onTreeNodeSelected(it) }
                    )

                    AppMode.PREDICTIVE -> PredictiveModeView(
                        predictions = uiState.predictions,
                        onPredictSelect = { viewModel.onPredictWordSelected(it) },
                        onLetterTyped = { viewModel.onLetterTyped(it) },
                        onSpace = { viewModel.onNextWordSpace() }
                    )

                    AppMode.MANAGEMENT -> ManagementModeView(
                        phrases = uiState.customPhrases,
                        onAddPhrase = { text, cat, type ->
                            viewModel.addCustomPhrase(text, cat, type)
                        },
                        onDeletePhrase = { viewModel.deletePhrase(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun SentenceAccumulatorBar(
    displayText: String,
    onSpeak: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Scroll to the end when text changes
    LaunchedEffect(displayText) {
        if (displayText.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Large Text Display with horizontal scrolling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xFF090D16), RoundedCornerShape(10.dp))
                    .border(1.5.dp, BorderDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (displayText.isEmpty()) "Tap a starter or word..." else displayText,
                    color = if (displayText.isEmpty()) TextMuted else Color(0xFF38BDF8),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.horizontalScroll(scrollState)
                )
            }

            // Row 2: Large Action Buttons (PLAY, BACKSPACE, CLEAR)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SPEAK / PLAY BUTTON
                Button(
                    onClick = onSpeak,
                    modifier = Modifier.weight(1.8f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak", modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SPEAK", fontSize = 20.sp, fontWeight = FontWeight.Black)
                }

                // BACKSPACE BUTTON
                Button(
                    onClick = onBackspace,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Undo", modifier = Modifier.size(26.dp))
                }

                // CLEAR ALL BUTTON
                Button(
                    onClick = onClear,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun ModeTabs(
    currentMode: AppMode,
    onModeSelected: (AppMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            AppMode.PHRASES to "💬 Phrases",
            AppMode.PREDICTIVE to "⌨ Predictor",
            AppMode.MANAGEMENT to "⚙ Manage"
        ).forEach { (mode, label) ->
            val isSelected = currentMode == mode
            Button(
                onClick = { onModeSelected(mode) },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) AccentBlue else CardDark
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (isSelected) Color(0xFF60A5FA) else BorderDark
                )
            ) {
                Text(
                    text = label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else TextMuted
                )
            }
        }
    }
}

/**
 * Deep multi-tier hierarchical sentence builder view with full grammatical branching.
 */
@Composable
fun HierarchicalPhrasesView(
    nodes: List<PhraseTreeData.PhraseNode>,
    breadcrumbs: List<String>,
    isAtRoot: Boolean,
    onNavigateUp: () -> Unit,
    onResetToRoot: () -> Unit,
    onSelectNode: (PhraseTreeData.PhraseNode) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation / Breadcrumb Header bar
        if (!isAtRoot) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onNavigateUp,
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BACK", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onResetToRoot,
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Start", modifier = Modifier.size(20.dp), tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("START", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }

                Text(
                    text = "➜ " + breadcrumbs.joinToString(" ➜ "),
                    color = Color(0xFF93C5FD),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        } else {
            Text(
                text = "CHOOSE A SENTENCE STARTER OR QUICK RESPONSE",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Large Button Phrases Grid (minimum 90dp height per target)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(nodes) { node ->
                val hasChildren = node.children.isNotEmpty()
                val borderColor = if (hasChildren) Color(0xFF38BDF8) else Color(0xFF4ADE80)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clickable { onSelectNode(node) },
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(2.5.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = node.iconEmoji, fontSize = 28.sp)
                        Text(
                            text = if (hasChildren) "${node.text} …" else node.text,
                            color = TextLight,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PredictiveModeView(
    predictions: List<String>,
    onPredictSelect: (String) -> Unit,
    onLetterTyped: (String) -> Unit,
    onSpace: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "PREDICTED NEXT WORDS (FULL CONTEXT)",
            color = Color(0xFF38BDF8),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // 6 Large Prediction Buttons
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(160.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(predictions) { word ->
                Button(
                    onClick = { onPredictSelect(word) },
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF60A5FA))
                ) {
                    Text(
                        text = word,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }

        Text(
            text = "ALPHABET / SPELLING",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // Large 4-Row Keyboard
        val keyboardRows = listOf(
            listOf("A", "B", "C", "D", "E", "F", "G"),
            listOf("H", "I", "J", "K", "L", "M", "N"),
            listOf("O", "P", "Q", "R", "S", "T", "U"),
            listOf("V", "W", "X", "Y", "Z")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            keyboardRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { letter ->
                        Button(
                            onClick = { onLetterTyped(letter) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = letter,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Large Spacebar / Next Word Button
        Button(
            onClick = onSpace,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, BorderDark)
        ) {
            Text("␣ NEXT WORD / SPACE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextLight)
        }
    }
}

@Composable
fun ManagementModeView(
    phrases: List<PhraseItem>,
    onAddPhrase: (String, String, PhraseType) -> Unit,
    onDeletePhrase: (PhraseItem) -> Unit
) {
    var newText by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Quick Chat") }
    var selectedType by remember { mutableStateOf(PhraseType.COMPLETE) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ADD NEW CUSTOM PHRASE", color = Color(0xFF38BDF8), fontSize = 15.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text("Phrase text") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = BorderDark
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Quick Chat", "Basic Needs", "Feelings", "Requests").forEach { cat ->
                        FilterChip(
                            selected = newCategory == cat,
                            onClick = { newCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (newText.isNotBlank()) {
                            onAddPhrase(newText, newCategory, selectedType)
                            newText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ ADD TO VOCABULARY", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Text("SAVED CUSTOM PHRASES", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(phrases) { phrase ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = phrase.text, color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = phrase.category, color = TextMuted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { onDeletePhrase(phrase) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                        }
                    }
                }
            }
        }
    }
}
