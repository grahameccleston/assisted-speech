package com.assisted.speech.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PhraseNodeEntity::class,
        UtteranceLogEntity::class,
        UserNgramEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun phraseDao(): PhraseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "assisted_speech_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                // Seed all available language packs ("en", "es", "fr", etc.) into Room
                scope.launch(Dispatchers.IO) {
                    val locales = LanguagePackLoader.getAvailableLocales(appContext)
                    locales.forEach { (code, _) ->
                        seedLocaleIfNeeded(appContext, instance.phraseDao(), code)
                    }
                }

                instance
            }
        }

        suspend fun seedLocaleIfNeeded(
            context: Context,
            dao: PhraseDao,
            locale: String
        ) {
            val pack = LanguagePackLoader.load(context, locale)
            dao.insertNodesIgnore(pack.concepts)
        }
    }
}
