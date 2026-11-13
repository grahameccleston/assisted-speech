package com.assisted.speech.data

import kotlin.math.exp
import kotlin.math.ln

data class SmartShortcut(
    val id: String,
    val fullText: String,
    val iconEmoji: String,
    val badgeLabel: String,
    val conceptPath: List<String>
)

data class LetterHighlight(
    val letter: String,
    val isPossible: Boolean,
    val isTopLikely: Boolean
)

object PredictionEngine {

    private const val RECENT_CONTEXT_WINDOW_MS = 3 * 60 * 1000L // 3 minutes for conversational follow-up
    private const val HALF_LIFE_MS = 7 * 24 * 60 * 60 * 1000.0   // 7-day recency decay half-life

    /**
     * Computes the 4-signal priority score for a PhraseNodeEntity:
     * 1. Caregiver-Pinned Boost
     * 2. Frecency (Frequency + exponential recency decay)
     * 3. Time-of-Day Routine Affinity (Learned bucket counts + Cold-start defaultTimeAffinity)
     * 4. Conversational Follow-up Context (Matches followUpConceptIds of recently spoken utterances)
     */
    fun computeNodeScore(
        node: PhraseNodeEntity,
        currentBucket: TimeBucket,
        nowMs: Long,
        recentFollowUpConceptIds: Set<String>,
        includePinnedBoost: Boolean = true
    ): Double {
        // 1. Caregiver Pinned Priority Boost
        val pinnedScore = if (includePinnedBoost && node.isPinned) 25.0 else 0.0

        // 2. Frecency Score
        val freqTerm = ln(1.0 + node.usageCount.toDouble())
        val recencyMultiplier = if (node.lastUsedAt > 0L) {
            val ageMs = (nowMs - node.lastUsedAt).coerceAtLeast(0L).toDouble()
            0.5 + 0.5 * exp(-ageMs / HALF_LIFE_MS)
        } else {
            0.5
        }
        val frecencyScore = 8.0 * freqTerm * recencyMultiplier

        // 3. Time-of-Day Routine Affinity
        val bucketCount = node.getTimeBucketCount(currentBucket)
        val learnedTimeRatio = if (node.usageCount > 0) {
            (bucketCount.toDouble() + 0.5) / (node.usageCount.toDouble() + 2.0)
        } else {
            0.25
        }
        val coldStartTimeBonus = when (node.defaultTimeAffinity) {
            currentBucket.name -> 4.5
            "ANY" -> 1.0
            else -> 0.0
        }
        val timeScore = (learnedTimeRatio * 6.0) + coldStartTimeBonus

        // 4. Conversational Follow-up Context
        val contextScore = if (recentFollowUpConceptIds.contains(node.conceptId)) 14.0 else 0.0

        // Tie-breaker using inverse sortOrder so initial layout is deterministic
        val baseOrderTieBreaker = (100 - node.sortOrder.coerceIn(0, 100)) * 0.02

        return pinnedScore + frecencyScore + timeScore + contextScore + baseOrderTieBreaker
    }

    /**
     * Ranks nodes for the current navigation level.
     * - At Root: Keeps caregiver-pinned core starters/responses in their stable top positions
     *   for muscle memory, while dynamically sorting secondary items by the 4-signal score.
     * - Inside a Branch: Dynamically ranks options by the 4-signal score so the most likely
     *   continuation is on Page 1, Slot 1.
     */
    fun rankTierNodes(
        allNodes: List<PhraseNodeEntity>,
        parentConceptId: String?,
        recentUtterances: List<UtteranceLogEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): List<PhraseNodeEntity> {
        val currentBucket = TimeBucket.current(nowMs)
        val recentFollowUpIds = extractRecentFollowUpIds(allNodes, recentUtterances, nowMs)

        val tierNodes = allNodes.filter { it.parentConceptId == parentConceptId }

        return if (parentConceptId == null) {
            // Hybrid Root Layout: Pinned items keep stable muscle-memory order first,
            // followed by unpinned items ranked by adaptive score.
            val (pinned, unpinned) = tierNodes.partition { it.isPinned }
            val sortedPinned = pinned.sortedWith(compareBy<PhraseNodeEntity> { it.sortOrder }.thenBy { it.id })
            val sortedUnpinned = unpinned.sortedByDescending {
                computeNodeScore(it, currentBucket, nowMs, recentFollowUpIds, includePinnedBoost = false)
            }
            sortedPinned + sortedUnpinned
        } else {
            tierNodes.sortedByDescending {
                computeNodeScore(it, currentBucket, nowMs, recentFollowUpIds, includePinnedBoost = true)
            }
        }
    }

    /**
     * Computes up to 2 high-value 1-tap Smart Shortcuts for the Smart Shortcuts Strip:
     * - When navigating inside a branch (e.g. ["Can you help me to"]), surfaces the top 2 complete
     *   sentences reachable from this branch so a 3-4 step path finishes in 1 more tap!
     * - When at Root, surfaces the top 2 complete sentences based on conversational follow-up,
     *   learned multi-word utterance history, and time-of-day affinity.
     */
    fun computeSmartShortcuts(
        allNodes: List<PhraseNodeEntity>,
        navigationStack: List<PhraseNodeEntity>,
        recentUtterances: List<UtteranceLogEntity>,
        metadata: LanguageMetadata,
        speakerGender: SpeakerGender,
        formality: Formality = Formality.POLITE,
        nowMs: Long = System.currentTimeMillis()
    ): List<SmartShortcut> {
        if (allNodes.isEmpty()) return emptyList()

        val currentBucket = TimeBucket.current(nowMs)
        val recentFollowUpIds = extractRecentFollowUpIds(allNodes, recentUtterances, nowMs)
        val nodesByParent = allNodes.groupBy { it.parentConceptId }
        val nodesByConcept = allNodes.associateBy { it.conceptId }

        // CASE 1: User is inside a branch -> surface top 2 full-sentence completions from this branch
        if (navigationStack.isNotEmpty()) {
            val prefixNodes = navigationStack
            val leafPaths = mutableListOf<List<PhraseNodeEntity>>()

            fun collectLeafPaths(currentPath: List<PhraseNodeEntity>, depth: Int) {
                if (depth > 5) return
                val lastNode = currentPath.last()
                val children = nodesByParent[lastNode.conceptId].orEmpty()
                if (children.isEmpty()) {
                    if (currentPath.size > prefixNodes.size) {
                        leafPaths.add(currentPath)
                    }
                } else {
                    children.forEach { child ->
                        collectLeafPaths(currentPath + child, depth + 1)
                    }
                }
            }

            collectLeafPaths(prefixNodes, 0)

            return leafPaths
                .map { path ->
                    val leaf = path.last()
                    val fullSentence = SentenceComposer.joinTokens(
                        tokens = path.map { it.text },
                        currentTypingWord = "",
                        metadata = metadata,
                        gender = speakerGender,
                        formality = formality
                    )
                    // Score path by leaf score + any matching utterance history
                    val leafScore = computeNodeScore(leaf, currentBucket, nowMs, recentFollowUpIds)
                    val pathKey = path.joinToString(">") { it.conceptId }
                    val historyMatches = recentUtterances.count { it.conceptPath == pathKey }
                    val totalScore = leafScore + (historyMatches * 6.0)
                    val badge = when {
                        historyMatches > 0 -> "★ Often Used"
                        leaf.defaultTimeAffinity == currentBucket.name -> "🕒 ${currentBucket.name.lowercase().replaceFirstChar { it.uppercase() }}"
                        else -> "⚡ 1-Tap Finish"
                    }
                    totalScore to SmartShortcut(
                        id = "branch_${leaf.conceptId}",
                        fullText = fullSentence,
                        iconEmoji = leaf.iconEmoji,
                        badgeLabel = badge,
                        conceptPath = path.map { it.conceptId }
                    )
                }
                .sortedByDescending { it.first }
                .take(2)
                .map { it.second }
        }

        // CASE 2: User is at Root -> combine conversational follow-ups, top learned full sentences, and time-of-day routines
        val candidates = mutableListOf<Pair<Double, SmartShortcut>>()

        // 2A: Conversational follow-up candidates (if user spoke within last 3 minutes)
        for (followId in recentFollowUpIds) {
            val node = nodesByConcept[followId] ?: continue
            // Build full path from root to this node if it's a leaf, or use its best child if it's a branch
            val fullPath = buildFullDefaultPath(node, nodesByConcept, nodesByParent, currentBucket, nowMs, recentFollowUpIds)
            val fullText = SentenceComposer.joinTokens(
                tokens = fullPath.map { it.text },
                currentTypingWord = "",
                metadata = metadata,
                gender = speakerGender,
                formality = formality
            )
            val leaf = fullPath.last()
            val score = 40.0 + computeNodeScore(leaf, currentBucket, nowMs, recentFollowUpIds)
            candidates.add(
                score to SmartShortcut(
                    id = "ctx_${leaf.conceptId}",
                    fullText = fullText,
                    iconEmoji = leaf.iconEmoji,
                    badgeLabel = "💬 Follow-Up",
                    conceptPath = fullPath.map { it.conceptId }
                )
            )
        }

        // 2B: Frequently spoken multi-token utterances from UtteranceLogEntity
        val groupedHistory = recentUtterances
            .filter { it.spokenText.isNotBlank() }
            .groupBy { it.spokenText.trim() }

        for ((text, logs) in groupedHistory) {
            val latest = logs.maxByOrNull { it.timestamp } ?: continue
            val count = logs.size
            val bucketMatches = logs.count { it.timeBucket == currentBucket.name }
            val ageMs = (nowMs - latest.timestamp).coerceAtLeast(0L).toDouble()
            val recency = exp(-ageMs / HALF_LIFE_MS)
            val score = 15.0 + (ln(1.0 + count) * 10.0 * recency) + (bucketMatches * 3.5)
            val pathList = latest.conceptPath.split(">").filter { it.isNotBlank() }
            val leafNode = pathList.lastOrNull()?.let { nodesByConcept[it] }
            candidates.add(
                score to SmartShortcut(
                    id = "hist_${text.hashCode()}",
                    fullText = text,
                    iconEmoji = leafNode?.iconEmoji ?: "🔊",
                    badgeLabel = if (bucketMatches > 0) "★ Routine" else "★ Frequent",
                    conceptPath = pathList
                )
            )
        }

        // 2C: Best Time-of-Day complete sentences from concept tree (cold-start & daily routine)
        val allLeaves = allNodes.filter { nodesByParent[it.conceptId].isNullOrEmpty() }
        for (leaf in allLeaves) {
            val ancestors = buildAncestorsToRoot(leaf, nodesByConcept)
            val fullText = SentenceComposer.joinTokens(
                tokens = ancestors.map { it.text },
                currentTypingWord = "",
                metadata = metadata,
                gender = speakerGender,
                formality = formality
            )
            // Avoid duplicating single-word Yes/No which are already on Page 1 root
            if (ancestors.size == 1 && leaf.isPinned) continue

            val score = computeNodeScore(leaf, currentBucket, nowMs, recentFollowUpIds, includePinnedBoost = false) +
                (if (ancestors.size > 1) 4.0 else 0.0)
            val badge = when {
                leaf.defaultTimeAffinity == currentBucket.name -> "🕒 ${currentBucket.name.lowercase().replaceFirstChar { it.uppercase() }}"
                leaf.usageCount > 0 -> "★ Suggested"
                else -> "⚡ Quick Speak"
            }
            candidates.add(
                score to SmartShortcut(
                    id = "time_${leaf.conceptId}",
                    fullText = fullText,
                    iconEmoji = leaf.iconEmoji,
                    badgeLabel = badge,
                    conceptPath = ancestors.map { it.conceptId }
                )
            )
        }

        return candidates
            .sortedByDescending { it.first }
            .distinctBy { it.second.fullText.lowercase() }
            .take(2)
            .map { it.second }
    }

    /**
     * Predicts next words by combining:
     * 1. Learned User N-grams (from UserNgramEntity)
     * 2. Language Pack N-grams
     * 3. Language Pack Core Vocabulary + Custom Phrases
     * Supports accent-insensitive prefix filtering.
     */
    fun predictWords(
        sentenceTokens: List<String>,
        currentPrefix: String,
        pack: LanguagePackData,
        userNgrams: List<UserNgramEntity>,
        allNodes: List<PhraseNodeEntity>
    ): List<String> {
        val normalizedPrefix = SentenceComposer.normalizeForMatch(currentPrefix)
        val contextCandidates = getContextCandidates(sentenceTokens, pack, userNgrams)

        // Include words from custom nodes added by caregiver/user
        val customWords = allNodes
            .filter { it.isCustom }
            .flatMap { it.text.split("\\s+".toRegex()) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (normalizedPrefix.isNotEmpty()) {
            val matchedContext = contextCandidates.filter {
                SentenceComposer.normalizeForMatch(it).startsWith(normalizedPrefix)
            }
            val matchedCustom = customWords.filter {
                SentenceComposer.normalizeForMatch(it).startsWith(normalizedPrefix)
            }
            val matchedVocab = pack.coreVocabulary.filter {
                SentenceComposer.normalizeForMatch(it).startsWith(normalizedPrefix)
            }
            val combined = (matchedContext + matchedCustom + matchedVocab)
                .distinctBy { it.lowercase() }
            return if (combined.isNotEmpty()) combined.take(6) else listOf(currentPrefix)
        }

        return contextCandidates.take(6)
    }

    /**
     * Computes which letters on the keyboard can validly continue a word given the current prefix
     * and highlights the top 4 most probable next letters so a motor-impaired user can spot them immediately.
     */
    fun computeLetterHighlights(
        sentenceTokens: List<String>,
        currentPrefix: String,
        pack: LanguagePackData,
        userNgrams: List<UserNgramEntity>,
        allNodes: List<PhraseNodeEntity>
    ): Map<String, LetterHighlight> {
        val normalizedPrefix = SentenceComposer.normalizeForMatch(currentPrefix)
        val contextCandidates = getContextCandidates(sentenceTokens, pack, userNgrams)
        val customWords = allNodes.flatMap { it.text.split("\\s+".toRegex()) }

        // Weight candidate words higher if they come from current context
        val weightedPool = mutableMapOf<String, Int>()
        contextCandidates.forEachIndexed { idx, phrase ->
            val firstWord = phrase.trim().split("\\s+".toRegex()).firstOrNull().orEmpty()
            if (firstWord.isNotEmpty()) {
                val weight = (20 - idx).coerceAtLeast(5)
                weightedPool[firstWord.lowercase()] = (weightedPool[firstWord.lowercase()] ?: 0) + weight
            }
        }
        customWords.forEach { w ->
            val clean = w.trim().lowercase()
            if (clean.isNotEmpty()) {
                weightedPool[clean] = (weightedPool[clean] ?: 0) + 3
            }
        }
        pack.coreVocabulary.forEachIndexed { idx, w ->
            val clean = w.trim().lowercase()
            if (clean.isNotEmpty()) {
                val weight = if (idx < 25) 3 else 1
                weightedPool[clean] = (weightedPool[clean] ?: 0) + weight
            }
        }

        val letterScores = mutableMapOf<String, Int>()
        for ((word, weight) in weightedPool) {
            val normWord = SentenceComposer.normalizeForMatch(word)
            if (normWord.startsWith(normalizedPrefix) && normWord.length > normalizedPrefix.length) {
                val nextChar = normWord[normalizedPrefix.length].uppercaseChar().toString()
                letterScores[nextChar] = (letterScores[nextChar] ?: 0) + weight
            }
        }

        val top4Letters = letterScores.entries
            .sortedByDescending { it.value }
            .take(4)
            .map { it.key }
            .toSet()

        val allKeyboardLetters = pack.metadata.keyboardPages.flatten()
        return allKeyboardLetters.associateWith { key ->
            val normKey = SentenceComposer.normalizeForMatch(key).uppercase()
            val isPunctuation = key == "'" || key == "?"
            val isPossible = letterScores.isEmpty() || letterScores.containsKey(normKey) || isPunctuation
            val isTopLikely = top4Letters.contains(normKey)
            LetterHighlight(
                letter = key,
                isPossible = isPossible,
                isTopLikely = isTopLikely
            )
        }
    }

    private fun getContextCandidates(
        sentenceTokens: List<String>,
        pack: LanguagePackData,
        userNgrams: List<UserNgramEntity>
    ): List<String> {
        val words = sentenceTokens
            .flatMap { it.trim().lowercase().split("\\s+".toRegex()) }
            .filter { it.isNotBlank() }

        if (words.isEmpty()) {
            val learnedStarts = userNgrams
                .filter { it.contextKey == "start" }
                .sortedByDescending { it.usageCount }
                .map { it.nextWord }
            val packStarts = pack.ngrams["start"].orEmpty()
            return (learnedStarts + packStarts).distinctBy { it.lowercase() }
        }

        val results = mutableListOf<String>()
        val maxWindow = minOf(5, words.size)

        for (windowSize in maxWindow downTo 1) {
            val suffixKey = words.takeLast(windowSize).joinToString(" ")
            // 1. User's personal learned N-grams first
            val personalMatches = userNgrams
                .filter { it.contextKey == suffixKey }
                .sortedByDescending { it.usageCount }
                .map { it.nextWord }
            results.addAll(personalMatches)

            // 2. Language Pack static N-grams
            pack.ngrams[suffixKey]?.let { results.addAll(it) }

            if (windowSize >= 3 && results.size >= 5) break
        }

        if (results.isEmpty()) {
            results.addAll(pack.coreVocabulary.take(6))
        }

        return results.distinctBy { it.lowercase() }
    }

    private fun extractRecentFollowUpIds(
        allNodes: List<PhraseNodeEntity>,
        recentUtterances: List<UtteranceLogEntity>,
        nowMs: Long
    ): Set<String> {
        val recentLogs = recentUtterances.filter { (nowMs - it.timestamp) in 0..RECENT_CONTEXT_WINDOW_MS }
        if (recentLogs.isEmpty()) return emptySet()

        val nodesByConcept = allNodes.associateBy { it.conceptId }
        val followUps = mutableSetOf<String>()
        for (log in recentLogs) {
            val pathIds = log.conceptPath.split(">").filter { it.isNotBlank() }
            for (cid in pathIds) {
                nodesByConcept[cid]?.getFollowUpIdsList()?.let { followUps.addAll(it) }
            }
        }
        return followUps
    }

    private fun buildAncestorsToRoot(
        node: PhraseNodeEntity,
        nodesByConcept: Map<String, PhraseNodeEntity>
    ): List<PhraseNodeEntity> {
        val chain = mutableListOf(node)
        var currentParentId = node.parentConceptId
        var guard = 0
        while (currentParentId != null && guard < 6) {
            val parent = nodesByConcept[currentParentId] ?: break
            chain.add(0, parent)
            currentParentId = parent.parentConceptId
            guard++
        }
        return chain
    }

    private fun buildFullDefaultPath(
        node: PhraseNodeEntity,
        nodesByConcept: Map<String, PhraseNodeEntity>,
        nodesByParent: Map<String?, List<PhraseNodeEntity>>,
        currentBucket: TimeBucket,
        nowMs: Long,
        recentFollowUpIds: Set<String>
    ): List<PhraseNodeEntity> {
        val prefix = buildAncestorsToRoot(node, nodesByConcept).toMutableList()
        var current = node
        var guard = 0
        while (guard < 5) {
            val children = nodesByParent[current.conceptId].orEmpty()
            if (children.isEmpty()) break
            val bestChild = children.maxByOrNull {
                computeNodeScore(it, currentBucket, nowMs, recentFollowUpIds)
            } ?: break
            prefix.add(bestChild)
            current = bestChild
            guard++
        }
        return prefix
    }
}
