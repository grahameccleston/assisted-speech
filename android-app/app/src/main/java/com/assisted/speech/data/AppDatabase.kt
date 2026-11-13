package com.assisted.speech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [PhraseItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "assisted_speech_db"
                ).addCallback(DatabaseCallback(scope)).build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.phraseDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: PhraseDao) {
                val seeds = listOf(
                    // Basic Needs
                    PhraseItem(text = "Yes", category = "Basic Needs", type = PhraseType.COMPLETE, iconEmoji = "✅"),
                    PhraseItem(text = "No", category = "Basic Needs", type = PhraseType.COMPLETE, iconEmoji = "❌"),
                    PhraseItem(text = "Please", category = "Basic Needs", type = PhraseType.COMPLETE, iconEmoji = "🙏"),
                    PhraseItem(text = "Thank you", category = "Basic Needs", type = PhraseType.COMPLETE, iconEmoji = "🙌"),
                    PhraseItem(text = "I need help", category = "Basic Needs", type = PhraseType.COMPLETE, iconEmoji = "🆘"),
                    PhraseItem(text = "Water", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "💧"),
                    PhraseItem(text = "Food", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "🍲"),
                    PhraseItem(text = "Restroom", category = "Basic Needs", type = PhraseType.COMPLETE, iconEmoji = "🚻"),

                    // Sentence Starters
                    PhraseItem(text = "I want", category = "Starters", type = PhraseType.FRAGMENT, iconEmoji = "👉"),
                    PhraseItem(text = "I need", category = "Starters", type = PhraseType.FRAGMENT, iconEmoji = "🙋"),
                    PhraseItem(text = "I feel", category = "Starters", type = PhraseType.FRAGMENT, iconEmoji = "💭"),
                    PhraseItem(text = "Can you", category = "Starters", type = PhraseType.FRAGMENT, iconEmoji = "🤝"),

                    // Feelings
                    PhraseItem(text = "Happy", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "😊"),
                    PhraseItem(text = "Tired", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "😴"),
                    PhraseItem(text = "In pain", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🩹"),
                    PhraseItem(text = "Cold", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🥶"),
                    PhraseItem(text = "Hot", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🥵"),

                    // Conversation & Questions
                    PhraseItem(text = "Hello", category = "Conversation", type = PhraseType.COMPLETE, iconEmoji = "👋"),
                    PhraseItem(text = "Good morning", category = "Conversation", type = PhraseType.COMPLETE, iconEmoji = "🌅"),
                    PhraseItem(text = "How are you?", category = "Questions", type = PhraseType.COMPLETE, iconEmoji = "❓"),
                    PhraseItem(text = "What time is it?", category = "Questions", type = PhraseType.COMPLETE, iconEmoji = "⏰")
                )
                seeds.forEach { dao.insertPhrase(it) }
            }
        }
    }
}
