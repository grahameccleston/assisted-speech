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
    val selectedCategory: String = "Starters",
    val sentenceTokens: List<String> = emptyList(),
    val currentTypingWord: String = "", // Partial word being typed via alphabet keys
    val categories: List<String> = listOf("Starters", "Quick Chat", "Basic Needs", "Feelings", "Requests", "Activities", "People & Places"),
    val phrases: List<PhraseItem> = emptyList(),
    val predictions: List<String> = emptyList()
) {
    /**
     * The full sentence to display and speak: completed tokens plus any currently typed word.
     */
    val displaySentence: String
        get() {
            val allParts = if (currentTypingWord.isNotBlank()) sentenceTokens + currentTypingWord else sentenceTokens
            return allParts.joinToString(" ").trim()
        }
}

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
                if (cats.isNotEmpty()) {
                    val preferredOrder = listOf("Starters", "Quick Chat", "Basic Needs", "Feelings", "Requests", "Activities", "People & Places")
                    val sortedCats = preferredOrder.filter { cats.contains(it) } + cats.filter { !preferredOrder.contains(it) }
                    _uiState.update { current ->
                        val initialCat = if (current.selectedCategory in sortedCats) current.selectedCategory else sortedCats.first()
                        current.copy(categories = sortedCats, selectedCategory = initialCat)
                    }
                }
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
        updatePredictions()
    }

    fun selectCategory(cat: String) {
        _uiState.update { it.copy(selectedCategory = cat) }
    }

    /**
     * Tapping a tile in Phrases mode commits it as a distinct word/phrase token.
     */
    fun onPhraseSelected(phrase: PhraseItem) {
        // Commit any partial word first
        val currentTokens = _uiState.value.sentenceTokens.toMutableList()
        if (_uiState.value.currentTypingWord.isNotBlank()) {
            currentTokens.add(_uiState.value.currentTypingWord)
        }
        currentTokens.add(phrase.text)

        val nextCategory = phrase.followUpCategory ?: _uiState.value.selectedCategory

        _uiState.update {
            it.copy(
                sentenceTokens = currentTokens,
                currentTypingWord = "",
                selectedCategory = nextCategory
            )
        }

        updatePredictions()
    }

    /**
     * Tapping a predicted word replaces whatever prefix was being typed with the chosen word and completes the token with space separation.
     */
    fun onPredictWordSelected(word: String) {
        val newTokens = _uiState.value.sentenceTokens + word
        _uiState.update {
            it.copy(
                sentenceTokens = newTokens,
                currentTypingWord = ""
            )
        }
        updatePredictions()
    }

    /**
     * Typing a letter appends to the current prefix and immediately updates prediction options based on preceding tokens + this prefix.
     */
    fun onLetterTyped(letter: String) {
        val updatedWord = _uiState.value.currentTypingWord + letter
        _uiState.update { it.copy(currentTypingWord = updatedWord) }
        updatePredictions()
    }

    /**
     * Commits the current typing word as a token and advances to next word.
     */
    fun onNextWordSpace() {
        if (_uiState.value.currentTypingWord.isNotBlank()) {
            val newTokens = _uiState.value.sentenceTokens + _uiState.value.currentTypingWord
            _uiState.update {
                it.copy(
                    sentenceTokens = newTokens,
                    currentTypingWord = ""
                )
            }
        }
        updatePredictions()
    }

    /**
     * Backspace deletes one character of the partial word if present, otherwise removes the last full token.
     */
    fun backspaceSentence() {
        val state = _uiState.value
        if (state.currentTypingWord.isNotEmpty()) {
            val shorter = state.currentTypingWord.dropLast(1)
            _uiState.update { it.copy(currentTypingWord = shorter) }
        } else if (state.sentenceTokens.isNotEmpty()) {
            _uiState.update { it.copy(sentenceTokens = it.sentenceTokens.dropLast(1)) }
        }
        updatePredictions()
    }

    fun clearSentence() {
        _uiState.update {
            it.copy(
                sentenceTokens = emptyList(),
                currentTypingWord = "",
                selectedCategory = "Starters"
            )
        }
        updatePredictions()
    }

    fun speakCurrentSentence() {
        val sentence = _uiState.value.displaySentence
        if (sentence.isNotBlank()) {
            speechManager.speak(sentence)
        }
    }

    private fun updatePredictions() {
        val state = _uiState.value
        val preds = PredictionEngine.predict(state.sentenceTokens, state.currentTypingWord)
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
