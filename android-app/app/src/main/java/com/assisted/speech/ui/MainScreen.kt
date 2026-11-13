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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
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
                sentenceTokens = uiState.sentenceTokens,
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
                    AppMode.PHRASES -> PhrasesModeView(
                        categories = uiState.categories,
                        selectedCategory = uiState.selectedCategory,
                        phrases = uiState.phrases.filter { it.category == uiState.selectedCategory },
                        onSelectCategory = { viewModel.selectCategory(it) },
                        onSelectPhrase = { viewModel.onPhraseSelected(it) }
                    )

                    AppMode.PREDICTIVE -> PredictiveModeView(
                        predictions = uiState.predictions,
                        onPredictSelect = { viewModel.onPredictWordSelected(it) },
                        onLetterTyped = { viewModel.onLetterTyped(it) },
                        onSpace = { viewModel.onNextWordSpace() }
                    )

                    AppMode.MANAGEMENT -> ManagementModeView(
                        phrases = uiState.phrases,
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
    sentenceTokens: List<String>,
    onSpeak: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Scroll to the end when new words are added
    LaunchedEffect(sentenceTokens) {
        if (sentenceTokens.isNotEmpty()) {
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
                    text = if (sentenceTokens.isEmpty()) "Tap a starter or phrase..." else sentenceTokens.joinToString(" "),
                    color = if (sentenceTokens.isEmpty()) TextMuted else Color(0xFF38BDF8),
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

@Composable
fun PhrasesModeView(
    categories: List<String>,
    selectedCategory: String,
    phrases: List<PhraseItem>,
    onSelectCategory: (String) -> Unit,
    onSelectPhrase: (PhraseItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Categories Row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Button(
                    onClick = { onSelectCategory(cat) },
                    modifier = Modifier.height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF0284C7) else CardDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, if (isSelected) Color(0xFF38BDF8) else BorderDark)
                ) {
                    Text(
                        text = cat,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Large Button Phrases Grid (minimum 90dp height per target)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(phrases) { phrase ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clickable { onSelectPhrase(phrase) },
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.5.dp,
                        when (phrase.type) {
                            PhraseType.STARTER -> Color(0xFF38BDF8)
                            PhraseType.FRAGMENT -> Color(0xFFA855F7)
                            PhraseType.COMPLETE -> Color(0xFF4ADE80)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = phrase.iconEmoji, fontSize = 28.sp)
                        Text(
                            text = phrase.text,
                            color = TextLight,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
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
            text = "PREDICTED NEXT WORDS",
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
            items(predictions.take(6)) { word ->
                Button(
                    onClick = { onPredictSelect(word) },
                    modifier = Modifier.fillMaxSize().height(72.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF3B82F6))
                ) {
                    Text(text = word, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
                }
            }
        }

        Text(
            text = "ALPHABET / SPELLING",
            color = TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // Alphabet Grid
        val alphabet = ('A'..'Z').map { it.toString() }
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(alphabet) { letter ->
                Button(
                    onClick = { onLetterTyped(letter) },
                    modifier = Modifier.height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(text = letter, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Space / Next Word Button
        Button(
            onClick = onSpace,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "␣ NEXT WORD / SPACE", fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
    var selectedCat by remember { mutableStateOf("Basic Needs") }
    var selectedType by remember { mutableStateOf(PhraseType.COMPLETE) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add New Phrase / Fragment", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text("Phrase or word") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = BorderDark
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedType = PhraseType.COMPLETE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == PhraseType.COMPLETE) AccentBlue else Color.DarkGray
                        )
                    ) {
                        Text("Complete")
                    }

                    Button(
                        onClick = { selectedType = PhraseType.STARTER },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == PhraseType.STARTER) AccentBlue else Color.DarkGray
                        )
                    ) {
                        Text("Starter")
                    }

                    Button(
                        onClick = { selectedType = PhraseType.FRAGMENT },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == PhraseType.FRAGMENT) AccentBlue else Color.DarkGray
                        )
                    ) {
                        Text("Fragment")
                    }
                }

                Button(
                    onClick = {
                        if (newText.isNotBlank()) {
                            onAddPhrase(newText, selectedCat, selectedType)
                            newText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Phrase", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Saved Phrases (${phrases.size})", color = TextMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(phrases) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDark, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.text,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "[${item.category}]",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = { onDeletePhrase(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                    }
                }
            }
        }
    }
}
