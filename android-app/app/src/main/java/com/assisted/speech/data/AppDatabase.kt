package com.assisted.speech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [PhraseItem::class], version = 2, exportSchema = false)
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
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
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
                    // 1. STARTERS (with target follow-up category)
                    PhraseItem(text = "I want", category = "Starters", type = PhraseType.STARTER, iconEmoji = "👉", followUpCategory = "Basic Needs"),
                    PhraseItem(text = "I need", category = "Starters", type = PhraseType.STARTER, iconEmoji = "🙋", followUpCategory = "Basic Needs"),
                    PhraseItem(text = "I feel", category = "Starters", type = PhraseType.STARTER, iconEmoji = "💭", followUpCategory = "Feelings"),
                    PhraseItem(text = "Can you", category = "Starters", type = PhraseType.STARTER, iconEmoji = "🤝", followUpCategory = "Requests"),
                    PhraseItem(text = "I am", category = "Starters", type = PhraseType.STARTER, iconEmoji = "🧍", followUpCategory = "Feelings"),
                    PhraseItem(text = "Please give me", category = "Starters", type = PhraseType.STARTER, iconEmoji = "🤲", followUpCategory = "Basic Needs"),

                    // 2. BASIC NEEDS & FRAGMENTS
                    PhraseItem(text = "Water", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "💧"),
                    PhraseItem(text = "Food", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "🍲"),
                    PhraseItem(text = "Restroom", category = "Basic Needs", type = PhraseType.COMPLETE, iconEmoji = "🚻"),
                    PhraseItem(text = "Medicine", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "💊"),
                    PhraseItem(text = "My glasses", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "👓"),
                    PhraseItem(text = "To rest", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "🛏️"),
                    PhraseItem(text = "To go outside", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "🚪"),

                    // 3. FEELINGS & STATES
                    PhraseItem(text = "Happy", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "😊"),
                    PhraseItem(text = "Tired", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "😴"),
                    PhraseItem(text = "In pain", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🩹"),
                    PhraseItem(text = "Cold", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🥶"),
                    PhraseItem(text = "Hot", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🥵"),
                    PhraseItem(text = "Thirsty", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🥤"),
                    PhraseItem(text = "Hungry", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🍽️"),
                    PhraseItem(text = "Uncomfortable", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "😣"),

                    // 4. COMMON QUICK RESPONSES (STANDALONE COMPLETE)
                    PhraseItem(text = "Yes", category = "Quick", type = PhraseType.COMPLETE, iconEmoji = "✅"),
                    PhraseItem(text = "No", category = "Quick", type = PhraseType.COMPLETE, iconEmoji = "❌"),
                    PhraseItem(text = "Please", category = "Quick", type = PhraseType.COMPLETE, iconEmoji = "🙏"),
                    PhraseItem(text = "Thank you", category = "Quick", type = PhraseType.COMPLETE, iconEmoji = "🙌"),
                    PhraseItem(text = "I need help", category = "Quick", type = PhraseType.COMPLETE, iconEmoji = "🆘"),
                    PhraseItem(text = "Hello", category = "Quick", type = PhraseType.COMPLETE, iconEmoji = "👋"),
                    PhraseItem(text = "Goodbye", category = "Quick", type = PhraseType.COMPLETE, iconEmoji = "👋"),

                    // 5. REQUESTS / ACTIONS
                    PhraseItem(text = "Help me", category = "Requests", type = PhraseType.FRAGMENT, iconEmoji = "🆘"),
                    PhraseItem(text = "Come here", category = "Requests", type = PhraseType.FRAGMENT, iconEmoji = "👉"),
                    PhraseItem(text = "Turn on lights", category = "Requests", type = PhraseType.FRAGMENT, iconEmoji = "💡"),
                    PhraseItem(text = "Turn off lights", category = "Requests", type = PhraseType.FRAGMENT, iconEmoji = "🌑"),
                    PhraseItem(text = "Open this", category = "Requests", type = PhraseType.FRAGMENT, iconEmoji = "📦"),
                    PhraseItem(text = "Call family", category = "Requests", type = PhraseType.FRAGMENT, iconEmoji = "📞")
                )
                seeds.forEach { dao.insertPhrase(it) }
            }
        }
    }
}
