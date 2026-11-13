package com.assisted.speech.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assisted.speech.data.*
import com.assisted.speech.tts.SpeechManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppMode {
    PHRASES,
    PREDICTIVE,
    MANAGEMENT
}

data class SpeechUiState(
    val currentMode: AppMode = AppMode.PHRASES,
    val selectedCategory: String = "All",
    val sentenceTokens: List<String> = emptyList(),
    val categories: List<String> = listOf("All"),
    val phrases: List<PhraseItem> = emptyList(),
    val predictions: List<String> = emptyList()
)

class SpeechViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val phraseDao = database.phraseDao()
    private val speechManager = SpeechManager(application)

    private val _uiState = MutableStateFlow(SpeechUiState())
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()

    init {
        // Observe categories
        viewModelScope.launch {
            phraseDao.getCategories().collect { cats ->
                val allCats = mutableListOf("All")
                allCats.addAll(cats)
                _uiState.update { it.copy(categories = allCats.distinct()) }
            }
        }

        // Observe phrases
        viewModelScope.launch {
            phraseDao.getAllPhrases().collect { list ->
                _uiState.update { it.copy(phrases = list) }
            }
        }

        // Initial predictions
        updatePredictions()
    }

    fun setMode(mode: AppMode) {
        _uiState.update { it.copy(currentMode = mode) }
        if (mode == AppMode.PREDICTIVE) {
            updatePredictions()
        }
    }

    fun selectCategory(cat: String) {
        _uiState.update { it.copy(selectedCategory = cat) }
    }

    fun onPhraseSelected(phrase: PhraseItem) {
        val newTokens = _uiState.value.sentenceTokens + phrase.text
        _uiState.update { it.copy(sentenceTokens = newTokens) }

        viewModelScope.launch {
            phraseDao.incrementUsage(phrase.id)
        }

        if (phrase.type == PhraseType.COMPLETE && newTokens.size == 1) {
            speakCurrentSentence()
        } else {
            updatePredictions()
        }
    }

    fun onPredictWordSelected(word: String) {
        val newTokens = _uiState.value.sentenceTokens + word
        _uiState.update { it.copy(sentenceTokens = newTokens) }
        updatePredictions()
    }

    fun onLetterTyped(letter: String) {
        val tokens = _uiState.value.sentenceTokens.toMutableList()
        if (tokens.isEmpty()) {
            tokens.add(letter)
        } else {
            val lastIdx = tokens.size - 1
            tokens[lastIdx] = tokens[lastIdx] + letter
        }
        _uiState.update { it.copy(sentenceTokens = tokens) }
        updatePredictions()
    }

    fun onNextWordSpace() {
        val tokens = _uiState.value.sentenceTokens + ""
        _uiState.update { it.copy(sentenceTokens = tokens) }
        updatePredictions()
    }

    fun backspaceSentence() {
        val tokens = _uiState.value.sentenceTokens
        if (tokens.isNotEmpty()) {
            _uiState.update { it.copy(sentenceTokens = tokens.dropLast(1)) }
            updatePredictions()
        }
    }

    fun clearSentence() {
        _uiState.update { it.copy(sentenceTokens = emptyList()) }
        updatePredictions()
    }

    fun speakCurrentSentence() {
        val sentence = _uiState.value.sentenceTokens.joinToString(" ")
        if (sentence.isNotBlank()) {
            speechManager.speak(sentence)
        }
    }

    private fun updatePredictions() {
        val preds = PredictionEngine.predictNextWords(_uiState.value.sentenceTokens)
        _uiState.update { it.copy(predictions = preds) }
    }

    fun addCustomPhrase(text: String, category: String, type: PhraseType) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val item = PhraseItem(
                text = text.trim(),
                category = category.trim(),
                type = type,
                iconEmoji = if (type == PhraseType.FRAGMENT) "🧩" else "💬"
            )
            phraseDao.insertPhrase(item)
        }
    }

    fun deletePhrase(phrase: PhraseItem) {
        viewModelScope.launch {
            phraseDao.deletePhrase(phrase)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }
}
