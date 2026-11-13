package com.assisted.speech.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class PhraseType {
    COMPLETE,  // Standalone full phrase (e.g., "Yes", "No", "Thank you")
    STARTER,   // Sentence starter (e.g., "I want", "I need", "I feel", "Can you")
    FRAGMENT   // Predicate / Object continuation (e.g., "Water", "Food", "Help", "Tired")
}

@Entity(tableName = "phrases")
data class PhraseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val category: String,
    val type: PhraseType,
    val iconEmoji: String = "💬",
    val usageCount: Int = 0,
    // If set, selecting a starter will filter or jump to follow-up category
    val followUpCategory: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrases ORDER BY id ASC")
    fun getAllPhrases(): Flow<List<PhraseItem>>

    @Query("SELECT * FROM phrases WHERE category = :category ORDER BY id ASC")
    fun getPhrasesByCategory(category: String): Flow<List<PhraseItem>>

    @Query("SELECT DISTINCT category FROM phrases")
    fun getCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: PhraseItem): Long

    @Delete
    suspend fun deletePhrase(phrase: PhraseItem)

    @Query("UPDATE phrases SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)
}
