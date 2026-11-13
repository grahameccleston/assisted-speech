package com.assisted.speech.data

object PredictionEngine {
    // Broad common vocabulary list for prefix matching
    private val commonVocabulary = listOf(
        "a", "about", "adjust", "after", "again", "all", "almost", "alone", "also", "always",
        "am", "and", "another", "any", "are", "arm", "around", "as", "ask", "at", "away",
        "back", "bad", "bath", "bathroom", "be", "because", "bed", "bedroom", "before", "better",
        "big", "blanket", "book", "both", "bother", "bring", "brush", "busy", "but", "by",
        "call", "can", "cannot", "can't", "care", "chair", "change", "clean", "close", "cold",
        "come", "comfortable", "cough", "cup",
        "day", "did", "different", "dizzy", "do", "doctor", "done", "don't", "door", "down", "drink",
        "ear", "early", "eat", "enough", "evening", "every", "everything", "excuse", "eye",
        "face", "family", "far", "fast", "feel", "few", "find", "fine", "first", "food", "for", "from",
        "get", "give", "glasses", "go", "good", "goodbye", "got", "great",
        "hand", "happy", "hard", "have", "he", "head", "hear", "hello", "help", "here", "high", "him", "his", "home", "hospital", "hot", "how", "hungry", "hurt",
        "i", "if", "in", "is", "it", "itch", "its",
        "just",
        "keep", "know",
        "later", "leave", "left", "leg", "let", "lie", "light", "lights", "like", "listen", "little", "look", "love",
        "make", "many", "maybe", "me", "medicine", "more", "morning", "mouth", "move", "much", "music", "my",
        "name", "nauseous", "need", "never", "new", "next", "night", "no", "not", "nothing", "now", "nurse",
        "of", "off", "okay", "old", "on", "once", "one", "open", "or", "other", "our", "out", "outside", "over",
        "pain", "painful", "people", "phone", "pillow", "place", "please", "position", "put",
        "question", "quiet",
        "read", "ready", "really", "requests", "rest", "restroom", "right", "room",
        "sad", "same", "say", "see", "she", "should", "sick", "side", "sit", "sleep", "slow", "small", "so", "some", "someone", "something", "sometimes", "soon", "sorry", "speak", "stand", "start", "stay", "stop",
        "take", "talk", "teeth", "tell", "television", "thank", "thanks", "that", "the", "their", "them", "then", "there", "these", "they", "thing", "think", "thirsty", "this", "time", "tired", "tissue", "to", "today", "tomorrow", "too", "turn", "tv",
        "uncomfortable", "under", "understand", "up", "us", "use",
        "very", "voice",
        "wait", "walk", "want", "warm", "was", "wash", "watch", "water", "way", "we", "well", "went", "what", "when", "where", "which", "who", "why", "will", "with", "work", "would",
        "yes", "yesterday", "you", "your", "you're"
    )

    // Contextual Markov / N-gram transitions (preceding word(s) -> likely next words)
    private val nGramMap = mapOf(
        "start" to listOf("I", "Yes", "No", "Please", "Can", "Help"),
        "i" to listOf("want", "need", "feel", "am", "would", "can't", "like", "don't"),
        "i want" to listOf("water", "food", "to rest", "to go", "help", "my glasses"),
        "i need" to listOf("help", "water", "medicine", "the restroom", "to sleep", "a break"),
        "i feel" to listOf("tired", "cold", "hot", "in pain", "happy", "uncomfortable"),
        "i am" to listOf("tired", "cold", "hot", "hungry", "thirsty", "in pain", "fine"),
        "please" to listOf("help", "give me", "call", "come", "turn", "open", "can you"),
        "please can" to listOf("you", "I", "we"),
        "can" to listOf("you", "I", "we", "someone", "please", "help"),
        "can you" to listOf("help me", "come here", "open this", "turn on", "turn off", "call doctor"),
        "can i" to listOf("have", "get", "go", "sleep", "see", "ask"),
        "can i have" to listOf("water", "food", "medicine", "my glasses", "a tissue"),
        "you" to listOf("help", "come", "are", "can", "know", "see"),
        "help" to listOf("me", "please", "now", "with this", "doctor"),
        "help me" to listOf("sit up", "lie down", "walk", "to the restroom", "please"),
        "turn" to listOf("on", "off", "up", "down", "around"),
        "turn on" to listOf("lights", "tv", "music", "fan"),
        "turn off" to listOf("lights", "tv", "music", "fan"),
        "call" to listOf("the doctor", "nurse", "family", "home", "help"),
        "where" to listOf("is", "are", "am I", "was"),
        "where is" to listOf("my glasses", "my phone", "the doctor", "the nurse", "family")
    )

    private val defaultFallbacks = listOf("please", "now", "more", "thank you", "help", "good")

    /**
     * Predicts next words considering:
     * 1. If user is currently typing a partial word (`currentPrefix`), filter and rank words starting with that prefix.
     * 2. If no partial word is being typed, use the preceding words (bigram/trigram context).
     */
    fun predict(sentenceTokens: List<String>, currentPrefix: String = ""): List<String> {
        val trimmedPrefix = currentPrefix.trim().lowercase()

        if (trimmedPrefix.isNotEmpty()) {
            // Contextual candidates matching the prefix first
            val contextCandidates = getNextWordCandidates(sentenceTokens)
            val matchedFromContext = contextCandidates.filter { it.lowercase().startsWith(trimmedPrefix) }

            // General vocabulary matching the prefix
            val matchedFromVocab = commonVocabulary.filter { it.startsWith(trimmedPrefix) }

            val combined = (matchedFromContext + matchedFromVocab).distinct()
            return if (combined.isNotEmpty()) combined.take(6) else defaultFallbacks
        }

        return getNextWordCandidates(sentenceTokens).take(6)
    }

    private fun getNextWordCandidates(sentenceTokens: List<String>): List<String> {
        if (sentenceTokens.isEmpty()) {
            return nGramMap["start"] ?: defaultFallbacks
        }

        val len = sentenceTokens.size
        val last1 = sentenceTokens[len - 1].trim().lowercase()
        val last2 = if (len >= 2) "${sentenceTokens[len - 2].trim().lowercase()} $last1" else ""

        if (last2.isNotBlank() && nGramMap.containsKey(last2)) {
            return nGramMap[last2]!!
        }
        if (nGramMap.containsKey(last1)) {
            return nGramMap[last1]!!
        }

        return defaultFallbacks
    }
}
