package com.assisted.speech.data

object PredictionEngine {

    // Ranked list of most common words in English + AAC communications (ordered by natural frequency)
    private val rankedGeneralVocabulary = listOf(
        // High frequency pronouns, verbs, prepositions
        "to", "the", "and", "a", "i", "it", "not", "he", "as", "you", "this", "but", "his", "they", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up", "out", "if", "about",
        "who", "get", "which", "go", "me", "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other", "than", "then", "now", "look",
        "only", "come", "its", "over", "think", "also", "back", "after", "use", "two", "how", "our", "work", "first",
        "well", "way", "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",

        // Essential AAC verbs & actions
        "sit", "stand", "lie", "walk", "move", "get", "reach", "find", "open", "close", "turn", "call", "wait",
        "eat", "drink", "sleep", "rest", "talk", "listen", "watch", "read", "wash", "brush",

        // Essential AAC nouns & states
        "toilet", "restroom", "bathroom", "table", "bed", "chair", "water", "food", "medicine", "doctor", "nurse",
        "help", "pain", "lights", "blanket", "pillow", "glasses", "phone", "family", "door", "window", "room",
        "happy", "sad", "cold", "hot", "warm", "better", "dizzy", "sick", "nauseous", "uncomfortable", "hungry", "thirsty",
        "please", "thank", "thanks", "hello", "goodbye", "yes", "sorry", "excuse"
    )

    // Context pattern table matching suffix window from longest (5-gram) down to 1-gram
    private val contextModel = mapOf(
        // 5-gram contexts
        "can you help me to" to listOf("sit up", "lie down", "stand up", "walk", "get up", "move", "go to", "eat", "drink"),
        "please can you help me" to listOf("to", "with", "sit up", "lie down", "walk"),
        "please can i go to" to listOf("the toilet", "the restroom", "the bathroom", "bed", "sleep", "the hospital"),

        // 4-gram contexts
        "you help me to" to listOf("sit up", "lie down", "stand up", "walk", "move", "go to", "get up"),
        "help me to" to listOf("sit up", "lie down", "stand up", "walk", "move", "get up", "reach", "use the restroom"),
        "can you help me" to listOf("to", "with", "sit up", "lie down", "walk", "now", "please"),
        "please can you" to listOf("help me", "turn on", "turn off", "open", "close", "call", "come here", "give me"),
        "please can i" to listOf("have", "go to", "get", "see", "ask"),
        "i want to go" to listOf("to the toilet", "to the restroom", "to bed", "outside", "home", "to sleep"),
        "i need to go" to listOf("to the toilet", "to the restroom", "to the bathroom", "to bed", "to sleep", "home"),
        "i would like to" to listOf("go to", "have", "sit up", "lie down", "rest", "watch tv", "eat", "drink"),
        "go to the" to listOf("toilet", "restroom", "bathroom", "table", "bed", "kitchen", "doctor", "hospital"),
        "turn on the" to listOf("lights", "tv", "television", "radio", "fan"),
        "turn off the" to listOf("lights", "tv", "television", "radio", "fan"),
        "where is the" to listOf("toilet", "restroom", "bathroom", "doctor", "nurse", "phone", "call button"),
        "where is my" to listOf("phone", "glasses", "medicine", "blanket", "family", "pillow"),

        // 3-gram contexts (trigrams)
        "help me" to listOf("to", "with", "sit up", "lie down", "walk", "now", "please"),
        "me to" to listOf("sit up", "lie down", "stand up", "walk", "move", "go to", "eat", "drink"),
        "can you" to listOf("help me", "come here", "turn on", "turn off", "open", "close", "call", "give me", "repeat"),
        "can i" to listOf("have", "go to", "get", "see", "ask", "sit"),
        "can i have" to listOf("water", "food", "medicine", "my glasses", "a tissue", "a drink"),
        "i want to" to listOf("go to", "have", "sleep", "eat", "drink", "watch", "sit up", "lie down", "rest"),
        "i need to" to listOf("go to", "use the toilet", "take my medicine", "see the doctor", "sleep", "rest", "call"),
        "i would like" to listOf("to", "some water", "some food", "to rest"),
        "i feel" to listOf("tired", "cold", "hot", "in pain", "sick", "dizzy", "uncomfortable", "better"),
        "i am" to listOf("tired", "cold", "hot", "hungry", "thirsty", "in pain", "ready", "fine"),
        "give me" to listOf("a", "some", "my", "the", "water", "food", "time", "my glasses"),
        "turn on" to listOf("the lights", "the tv", "the television", "the radio", "the fan"),
        "turn off" to listOf("the lights", "the tv", "the television", "the radio", "the fan"),
        "to the" to listOf("toilet", "restroom", "bathroom", "table", "bed", "kitchen", "doctor", "hospital"),
        "in the" to listOf("room", "bed", "chair", "morning", "bathroom", "kitchen"),
        "on the" to listOf("table", "bed", "chair", "lights", "tv"),
        "where is" to listOf("the toilet", "the restroom", "the bathroom", "my phone", "my glasses", "the doctor", "the nurse"),
        "what time" to listOf("is it", "is lunch", "is dinner", "is breakfast"),
        "thank you" to listOf("very much", "so much", "for your help", "again"),

        // 2-gram contexts (bigrams)
        "please" to listOf("can you", "can I", "help me", "give me", "call", "come", "turn on", "open"),
        "can" to listOf("you", "I", "we", "someone"),
        "i" to listOf("want", "need", "feel", "am", "would", "can't", "have"),
        "to" to listOf("the", "go", "be", "have", "sit", "lie", "stand", "sleep", "eat", "drink", "walk"),
        "the" to listOf("toilet", "restroom", "bathroom", "doctor", "nurse", "lights", "tv", "bed", "table", "door"),
        "my" to listOf("glasses", "phone", "medicine", "blanket", "pillow", "family", "doctor"),
        "want" to listOf("to", "water", "food", "help", "my glasses", "to rest"),
        "need" to listOf("to", "help", "water", "medicine", "my doctor", "a tissue"),
        "feel" to listOf("tired", "cold", "hot", "sick", "in pain", "dizzy", "better"),
        "am" to listOf("tired", "cold", "hot", "hungry", "thirsty", "in pain", "fine"),
        "help" to listOf("me", "us", "please", "with", "now"),
        "turn" to listOf("on", "off", "up", "down"),
        "call" to listOf("the doctor", "the nurse", "my family", "help"),
        "open" to listOf("the door", "the window", "this"),
        "close" to listOf("the door", "the window"),
        "where" to listOf("is the", "is my", "are you"),
        "how" to listOf("are you", "is it"),

        // 1-gram / sentence start
        "start" to listOf("I want", "I need", "Please", "Can you", "Can I", "Help me", "Where is", "Thank you")
    )

    private val defaultFallbacks = listOf("toilet", "water", "sit up", "lie down", "help", "please")

    /**
     * Context-aware next word predictor across the FULL context window.
     * Searches suffix patterns starting from the longest available match (5-gram -> 4-gram -> 3-gram -> 2-gram -> 1-gram).
     */
    fun predict(sentenceTokens: List<String>, currentPrefix: String = ""): List<String> {
        val trimmedPrefix = currentPrefix.trim().lowercase()

        if (trimmedPrefix.isNotEmpty()) {
            val matchedFromContext = getContextCandidates(sentenceTokens)
                .filter { it.lowercase().startsWith(trimmedPrefix) }

            val matchedFromVocab = rankedGeneralVocabulary
                .filter { it.startsWith(trimmedPrefix) }

            val results = (matchedFromContext + matchedFromVocab).distinct()
            return if (results.isNotEmpty()) results.take(6) else defaultFallbacks
        }

        return getContextCandidates(sentenceTokens).take(6)
    }

    private fun getContextCandidates(sentenceTokens: List<String>): List<String> {
        if (sentenceTokens.isEmpty()) {
            return contextModel["start"] ?: defaultFallbacks
        }

        // Clean tokens: flatten any spaces within tokens (e.g. "Can you" -> ["can", "you"])
        val words = sentenceTokens.flatMap { it.trim().lowercase().split("\\s+".toRegex()) }.filter { it.isNotBlank() }
        if (words.isEmpty()) {
            return contextModel["start"] ?: defaultFallbacks
        }

        val candidates = mutableListOf<String>()

        // Check longest matching context suffix first (from 5 words down to 1 word)
        val maxWindow = minOf(5, words.size)
        for (windowSize in maxWindow downTo 1) {
            val suffixKey = words.takeLast(windowSize).joinToString(" ")
            if (contextModel.containsKey(suffixKey)) {
                candidates.addAll(contextModel[suffixKey]!!)
                // If we found a high-order match (3+ words), prioritize it heavily
                if (windowSize >= 3 && candidates.size >= 4) {
                    break
                }
            }
        }

        if (candidates.isEmpty()) {
            candidates.addAll(defaultFallbacks)
        }

        return candidates.distinct()
    }
}
