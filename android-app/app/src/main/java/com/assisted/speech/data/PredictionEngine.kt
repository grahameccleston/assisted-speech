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

        // Essential AAC nouns & verbs
        "toilet", "restroom", "bathroom", "table", "telephone", "tv", "television", "tissue", "tea", "tired", "thirsty",
        "water", "food", "medicine", "doctor", "nurse", "help", "pain", "bed", "chair", "sleep", "nap", "drink", "eat",
        "walk", "sit", "lie", "stand", "door", "window", "lights", "blanket", "pillow", "glasses", "phone", "family",
        "happy", "sad", "cold", "hot", "warm", "better", "dizzy", "sick", "nauseous", "uncomfortable", "hungry", "quiet",
        "outside", "inside", "home", "hospital", "room", "kitchen", "morning", "night", "today", "tomorrow", "yesterday",
        "please", "thank", "thanks", "hello", "goodbye", "yes", "sorry", "excuse", "again", "more", "done", "stop",
        "open", "close", "turn", "call", "listen", "watch", "read", "music", "radio", "book", "position", "care"
    )

    // Contextual N-Gram model mapping previous word(s) -> list of most likely follow-up words
    private val nGramMap = mapOf(
        // Start of sentence
        "start" to listOf("I", "Please", "Can", "Yes", "No", "Thank you", "Where", "Help"),

        // Single word contexts
        "please" to listOf("can", "help", "give", "call", "come", "turn", "open", "tell"),
        "can" to listOf("I", "you", "we", "someone"),
        "i" to listOf("want", "need", "feel", "am", "would", "can't", "have", "like"),
        "go" to listOf("to", "outside", "home", "back", "away", "in"),
        "want" to listOf("to", "some", "water", "food", "help", "my"),
        "need" to listOf("to", "help", "some", "water", "medicine", "my", "a"),
        "feel" to listOf("tired", "cold", "hot", "sick", "in pain", "dizzy", "better"),
        "am" to listOf("tired", "cold", "hot", "hungry", "thirsty", "in pain", "fine"),
        "have" to listOf("some", "a", "to", "my", "water", "food"),
        "give" to listOf("me", "us", "him", "her"),
        "help" to listOf("me", "us", "please", "with", "now"),
        "turn" to listOf("on", "off", "up", "down", "around"),
        "call" to listOf("the", "my", "a", "doctor", "nurse", "family"),
        "open" to listOf("the", "this", "my", "door", "window"),
        "close" to listOf("the", "this", "door", "window"),
        "where" to listOf("is", "are", "can", "do"),
        "what" to listOf("time", "is", "do", "happened"),
        "how" to listOf("are", "is", "much", "long"),
        "thank" to listOf("you", "God"),
        "good" to listOf("morning", "night", "afternoon", "day"),

        // Bigram contexts (2 preceding words)
        "please can" to listOf("I", "you", "we"),
        "can i" to listOf("have", "go", "get", "see", "ask", "sit"),
        "can you" to listOf("help", "come", "turn", "open", "close", "call", "give", "repeat"),
        "i want" to listOf("to", "water", "food", "help", "my glasses", "my phone"),
        "i need" to listOf("to", "help", "water", "medicine", "my doctor", "a tissue"),
        "i feel" to listOf("tired", "very", "cold", "hot", "in pain", "sick", "dizzy"),
        "i am" to listOf("tired", "very", "feeling", "ready", "in pain", "cold", "hungry"),
        "give me" to listOf("a", "some", "my", "the", "water", "food", "time"),
        "help me" to listOf("to", "sit up", "lie down", "walk", "with", "stand up"),
        "want to" to listOf("go", "have", "sleep", "eat", "drink", "watch", "listen", "see", "sit"),
        "need to" to listOf("go", "use", "take", "see", "sleep", "rest", "call", "have"),
        "go to" to listOf("the", "bed", "sleep", "school", "work", "hospital"),
        "to the" to listOf("toilet", "restroom", "bathroom", "table", "bed", "kitchen", "room", "doctor", "hospital"),
        "in the" to listOf("room", "bed", "chair", "morning", "bathroom", "kitchen"),
        "at the" to listOf("table", "moment", "hospital", "door"),
        "on the" to listOf("table", "bed", "lights", "tv", "television", "radio"),
        "off the" to listOf("lights", "tv", "television", "radio"),
        "turn on" to listOf("the", "lights", "tv", "television", "radio"),
        "turn off" to listOf("the", "lights", "tv", "television", "radio"),
        "where is" to listOf("the", "my", "a", "toilet", "restroom", "bathroom", "phone", "glasses", "doctor", "nurse"),
        "what time" to listOf("is", "does", "will"),
        "how are" to listOf("you", "things"),
        "thank you" to listOf("very much", "so much", "for", "again"),

        // Trigram contexts (3 preceding words)
        "please can i" to listOf("have", "go", "get", "see", "ask"),
        "can i go" to listOf("to", "outside", "home", "back"),
        "i want to" to listOf("go", "have", "sleep", "eat", "drink", "watch", "sit"),
        "i need to" to listOf("go", "use", "take", "see", "sleep", "rest"),
        "i go to" to listOf("the", "bed", "sleep", "hospital"),
        "go to the" to listOf("toilet", "restroom", "bathroom", "table", "bed", "kitchen", "doctor", "hospital"),
        "turn on the" to listOf("lights", "tv", "television", "radio", "fan"),
        "turn off the" to listOf("lights", "tv", "television", "radio", "fan"),
        "can you help" to listOf("me", "us"),
        "help me with" to listOf("this", "eating", "getting", "walking"),
        "where is the" to listOf("toilet", "restroom", "bathroom", "doctor", "nurse", "phone")
    )

    private val defaultFallbacks = listOf("toilet", "water", "help", "please", "now", "good")

    /**
     * Context-aware next word predictor:
     * - If typing a prefix: Checks Trigram -> Bigram -> Unigram context candidates first for words starting with prefix.
     *   Then fills remaining slots with ranked frequency-based vocabulary matching the prefix.
     * - If not typing: Returns the most probable grammatical follow-on words from Trigram/Bigram/Unigram models.
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
            return nGramMap["start"] ?: defaultFallbacks
        }

        val len = sentenceTokens.size
        val w1 = sentenceTokens[len - 1].trim().lowercase()
        val w2 = if (len >= 2) "${sentenceTokens[len - 2].trim().lowercase()} $w1" else ""
        val w3 = if (len >= 3) "${sentenceTokens[len - 3].trim().lowercase()} $w2" else ""

        val candidates = mutableListOf<String>()

        // 1. Check Trigram ("go to the" -> "toilet", "restroom", ...)
        if (w3.isNotBlank() && nGramMap.containsKey(w3)) {
            candidates.addAll(nGramMap[w3]!!)
        }

        // 2. Check Bigram ("to the" -> "toilet", "restroom", "table", ...)
        if (w2.isNotBlank() && nGramMap.containsKey(w2)) {
            candidates.addAll(nGramMap[w2]!!)
        }

        // 3. Check Unigram ("the" -> "toilet", "phone", ...)
        if (nGramMap.containsKey(w1)) {
            candidates.addAll(nGramMap[w1]!!)
        }

        if (candidates.isEmpty()) {
            candidates.addAll(defaultFallbacks)
        }

        return candidates.distinct()
    }
}
