package com.assisted.speech.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Backspace
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
        modifier = Modifier.fillMaxSize(),
        color = BackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // 1. TOP SENTENCE ACCUMULATOR BAR
            SentenceAccumulatorBar(
                sentenceTokens = uiState.sentenceTokens,
                onSpeak = { viewModel.speakCurrentSentence() },
                onBackspace = { viewModel.backspaceSentence() },
                onClear = { viewModel.clearSentence() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. MODE NAVIGATION TABS
            ModeTabs(
                currentMode = uiState.currentMode,
                onModeSelected = { viewModel.setMode(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. CURRENT VIEW BODY
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.currentMode) {
                    AppMode.PHRASES -> PhrasesModeView(
                        categories = uiState.categories,
                        selectedCategory = uiState.selectedCategory,
                        phrases = uiState.phrases.filter {
                            uiState.selectedCategory == "All" || it.category == uiState.selectedCategory
                        },
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF090D16), RoundedCornerShape(10.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (sentenceTokens.isEmpty()) "Tap phrases to compose sentence..." else sentenceTokens.joinToString(" "),
                    color = if (sentenceTokens.isEmpty()) TextMuted else Color(0xFF38BDF8),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action Buttons
            Button(
                onClick = onSpeak,
                modifier = Modifier.fillMaxHeight().width(115.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak", modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PLAY", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onBackspace,
                modifier = Modifier.fillMaxHeight().width(85.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Backspace, contentDescription = "Undo", modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onClear,
                modifier = Modifier.fillMaxHeight().width(85.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(24.dp))
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
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            AppMode.PHRASES to "💬 Phrases",
            AppMode.PREDICTIVE to "⌨ Predictive",
            AppMode.MANAGEMENT to "⚙ Management"
        ).forEach { (mode, label) ->
            val isSelected = currentMode == mode
            Button(
                onClick = { onModeSelected(mode) },
                modifier = Modifier.weight(1f).height(58.dp),
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
                    fontSize = 18.sp,
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
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF0284C7) else CardDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) Color(0xFF38BDF8) else BorderDark)
                ) {
                    Text(text = cat, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Large Button Phrases Grid (minimum 100dp height per target)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(phrases) { phrase ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .clickable { onSelectPhrase(phrase) },
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.5.dp,
                        if (phrase.type == PhraseType.FRAGMENT) Color(0xFF38BDF8) else Color(0xFF4ADE80)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = phrase.iconEmoji, fontSize = 28.sp)
                        Text(
                            text = phrase.text,
                            color = TextLight,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
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
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "RECOMMENDED NEXT WORDS",
            color = Color(0xFF38BDF8),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        // 6 Large Prediction Buttons
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(170.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(predictions.take(6)) { word ->
                Button(
                    onClick = { onPredictSelect(word) },
                    modifier = Modifier.fillMaxSize().height(75.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF3B82F6))
                ) {
                    Text(text = word, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        Text(
            text = "ALPHABET / SPELLING FALLBACK",
            color = TextMuted,
            fontSize = 15.sp,
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
                    modifier = Modifier.height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardDark),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Text(text = letter, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Space / Next Word Button
        Button(
            onClick = onSpace,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "␣ NEXT WORD / SPACE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add New Phrase / Vocabulary", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

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
                        Text("Complete Phrase")
                    }

                    Button(
                        onClick = { selectedType = PhraseType.FRAGMENT },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == PhraseType.FRAGMENT) AccentBlue else Color.DarkGray
                        )
                    ) {
                        Text("Sentence Fragment")
                    }
                }

                Button(
                    onClick = {
                        if (newText.isNotBlank()) {
                            onAddPhrase(newText, selectedCat, selectedType)
                            newText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Phrase", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("Saved Phrases (${phrases.size})", color = TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)

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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "[${item.category}]",
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { onDeletePhrase(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                    }
                }
            }
        }
    }
}
