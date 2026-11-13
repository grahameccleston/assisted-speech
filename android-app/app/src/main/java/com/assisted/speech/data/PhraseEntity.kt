package com.assisted.speech.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

enum class NodeType {
    STARTER_BRANCH,
    QUICK_PHRASE,
    BRANCH_OPTION,
    LEAF_PHRASE
}

enum class TimeBucket {
    MORNING,    // 05:00 - 11:59
    AFTERNOON,  // 12:00 - 16:59
    EVENING,    // 17:00 - 21:59
    NIGHT;      // 22:00 - 04:59

    companion object {
        fun current(timestampMs: Long = System.currentTimeMillis()): TimeBucket {
            val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
            return when (cal.get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> MORNING
                in 12..16 -> AFTERNOON
                in 17..21 -> EVENING
                else -> NIGHT
            }
        }
    }
}

@Entity(
    tableName = "phrase_nodes",
    indices = [
        Index(value = ["locale", "conceptId"], unique = true),
        Index(value = ["locale", "parentConceptId"])
    ]
)
data class PhraseNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceptId: String,
    val parentConceptId: String? = null,
    val locale: String = "en",
    val text: String,
    val iconEmoji: String = "💬",
    val nodeType: NodeType = NodeType.LEAF_PHRASE,
    val isAutoSpeak: Boolean = true,
    val isPinned: Boolean = false,
    val isCustom: Boolean = false,
    val sortOrder: Int = 100,
    val defaultTimeAffinity: String = "ANY",
    val followUpConceptIds: String = "",
    val usageCount: Int = 0,
    val lastUsedAt: Long = 0L,
    val morningCount: Int = 0,
    val afternoonCount: Int = 0,
    val eveningCount: Int = 0,
    val nightCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFollowUpIdsList(): List<String> =
        if (followUpConceptIds.isBlank()) emptyList()
        else followUpConceptIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun getTimeBucketCount(bucket: TimeBucket): Int = when (bucket) {
        TimeBucket.MORNING -> morningCount
        TimeBucket.AFTERNOON -> afternoonCount
        TimeBucket.EVENING -> eveningCount
        TimeBucket.NIGHT -> nightCount
    }
}

@Entity(
    tableName = "utterance_logs",
    indices = [Index(value = ["locale", "timestamp"])]
)
data class UtteranceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locale: String = "en",
    val spokenText: String,
    val rootConceptId: String? = null,
    val terminalConceptId: String? = null,
    val conceptPath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val timeBucket: String = TimeBucket.current().name
)

@Entity(
    tableName = "user_ngrams",
    indices = [Index(value = ["locale", "contextKey", "nextWord"], unique = true)]
)
data class UserNgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locale: String = "en",
    val contextKey: String,
    val nextWord: String,
    val usageCount: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis()
)

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrase_nodes WHERE locale = :locale ORDER BY sortOrder ASC, id ASC")
    fun observeNodesForLocale(locale: String): Flow<List<PhraseNodeEntity>>

    @Query("SELECT * FROM phrase_nodes WHERE locale = :locale ORDER BY sortOrder ASC, id ASC")
    suspend fun getNodesForLocale(locale: String): List<PhraseNodeEntity>

    @Query("SELECT COUNT(*) FROM phrase_nodes WHERE locale = :locale")
    suspend fun getNodeCount(locale: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNodesIgnore(nodes: List<PhraseNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNode(node: PhraseNodeEntity): Long

    @Delete
    suspend fun deleteNode(node: PhraseNodeEntity)

    @Query("UPDATE phrase_nodes SET isPinned = :isPinned WHERE conceptId = :conceptId")
    suspend fun setConceptPinnedAllLocales(conceptId: String, isPinned: Boolean)

    /**
     * Updates usage & time-of-day counters across ALL locales sharing the same Concept ID,
     * so user adaptation and routines transfer seamlessly across languages.
     */
    @Query(
        """
        UPDATE phrase_nodes SET 
            usageCount = usageCount + 1,
            lastUsedAt = :timestamp,
            morningCount = morningCount + CASE WHEN :bucket = 'MORNING' THEN 1 ELSE 0 END,
            afternoonCount = afternoonCount + CASE WHEN :bucket = 'AFTERNOON' THEN 1 ELSE 0 END,
            eveningCount = eveningCount + CASE WHEN :bucket = 'EVENING' THEN 1 ELSE 0 END,
            nightCount = nightCount + CASE WHEN :bucket = 'NIGHT' THEN 1 ELSE 0 END
        WHERE conceptId = :conceptId
        """
    )
    suspend fun recordConceptUsageAcrossLocales(
        conceptId: String,
        timestamp: Long,
        bucket: String
    )

    @Query(
        """
        UPDATE phrase_nodes SET
            isPinned = :isPinned,
            usageCount = :usageCount,
            lastUsedAt = :lastUsedAt,
            morningCount = :morningCount,
            afternoonCount = :afternoonCount,
            eveningCount = :eveningCount,
            nightCount = :nightCount
        WHERE conceptId = :conceptId
        """
    )
    suspend fun restoreConceptStats(
        conceptId: String,
        isPinned: Boolean,
        usageCount: Int,
        lastUsedAt: Long,
        morningCount: Int,
        afternoonCount: Int,
        eveningCount: Int,
        nightCount: Int
    )

    // Utterance logs for full-sentence shortcuts & conversational context
    @Insert
    suspend fun insertUtteranceLog(log: UtteranceLogEntity)

    @Query("SELECT * FROM utterance_logs WHERE locale = :locale ORDER BY timestamp DESC LIMIT 200")
    fun observeRecentUtterances(locale: String): Flow<List<UtteranceLogEntity>>

    // Learned User N-grams
    @Query("SELECT * FROM user_ngrams WHERE locale = :locale ORDER BY usageCount DESC, lastUsedAt DESC")
    fun observeUserNgrams(locale: String): Flow<List<UserNgramEntity>>

    @Query("SELECT * FROM user_ngrams WHERE locale = :locale AND contextKey = :contextKey AND nextWord = :nextWord LIMIT 1")
    suspend fun getUserNgram(locale: String, contextKey: String, nextWord: String): UserNgramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserNgram(ngram: UserNgramEntity)
}
