package com.assisted.speech.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class PhraseType {
    COMPLETE,  // Standalone full phrase (e.g., "I need help", "Yes")
    FRAGMENT   // Sentence fragment (e.g., "I want", "Water")
}

@Entity(tableName = "phrases")
data class PhraseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val category: String,
    val type: PhraseType,
    val iconEmoji: String = "💬",
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrases ORDER BY usageCount DESC, id ASC")
    fun getAllPhrases(): Flow<List<PhraseItem>>

    @Query("SELECT * FROM phrases WHERE category = :category ORDER BY usageCount DESC, id ASC")
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
