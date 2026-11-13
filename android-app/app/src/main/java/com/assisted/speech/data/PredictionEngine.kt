package com.assisted.speech.data

object PredictionEngine {
    // Offline N-gram / Bigram / Trigram mapping for communication
    private val nGramMap = mapOf(
        "start" to listOf("I", "Yes", "No", "Please", "Can", "Help"),
        "i" to listOf("want", "need", "am", "feel", "like", "don't"),
        "i want" to listOf("water", "food", "to rest", "to go outside", "to speak", "help"),
        "i need" to listOf("help", "water", "medicine", "my doctor", "a break", "to sleep"),
        "i feel" to listOf("tired", "happy", "in pain", "cold", "hungry", "better"),
        "can" to listOf("you", "I", "we", "help", "please", "call"),
        "can you" to listOf("help me", "come here", "open this", "turn it off", "call family", "repeat that"),
        "you" to listOf("are", "can", "know", "look", "want", "have"),
        "please" to listOf("help", "wait", "give me", "listen", "call", "come"),
        "help" to listOf("me", "please", "now", "with this", "doctor")
    )

    private val defaultFallbacks = listOf("please", "now", "more", "thank you", "help", "good")

    fun predictNextWords(sentenceTokens: List<String>): List<String> {
        if (sentenceTokens.isEmpty()) {
            return nGramMap["start"] ?: defaultFallbacks
        }

        val len = sentenceTokens.size
        val last1 = sentenceTokens[len - 1].trim().lowercase()
        val last2 = if (len >= 2) "${sentenceTokens[len - 2].trim().lowercase()} $last1" else ""

        // Check Trigram/Bigram first, then unigram
        if (last2.isNotBlank() && nGramMap.containsKey(last2)) {
            return nGramMap[last2]!!
        }
        if (nGramMap.containsKey(last1)) {
            return nGramMap[last1]!!
        }

        return defaultFallbacks
    }
}
