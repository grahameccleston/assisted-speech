package com.assisted.speech.data

object InitialPhrases {
    fun getSeeds(): List<PhraseItem> = listOf(
        // Quick responses
        PhraseItem(text = "Yes", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "✅"),
        PhraseItem(text = "No", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "❌"),
        PhraseItem(text = "Please", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "🙏"),
        PhraseItem(text = "Thank you", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "🙌"),
        PhraseItem(text = "I need help", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "🆘"),
        PhraseItem(text = "You're welcome", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "🤝"),
        PhraseItem(text = "Hello", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "👋"),
        PhraseItem(text = "Goodbye", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "👋"),
        PhraseItem(text = "Good morning", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "🌅"),
        PhraseItem(text = "Good night", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "🌙"),
        PhraseItem(text = "How are you?", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "❓"),
        PhraseItem(text = "I am fine", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "👍"),
        PhraseItem(text = "Excuse me", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "🗣️"),
        PhraseItem(text = "I don't know", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "🤷"),
        PhraseItem(text = "Maybe later", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "⏳"),
        PhraseItem(text = "I love you", category = "Quick Chat", type = PhraseType.COMPLETE, iconEmoji = "❤️"),

        // Needs & Care
        PhraseItem(text = "Water", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "💧"),
        PhraseItem(text = "Food", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "🍲"),
        PhraseItem(text = "Restroom", category = "Basic Needs", type = PhraseType.COMPLETE, iconEmoji = "🚻"),
        PhraseItem(text = "Medicine", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "💊"),
        PhraseItem(text = "Glasses", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "👓"),
        PhraseItem(text = "Blanket", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "🛏️"),
        PhraseItem(text = "Pillow", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "🛋️"),
        PhraseItem(text = "To rest", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "😴"),
        PhraseItem(text = "A tissue", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "🤧"),
        PhraseItem(text = "My phone", category = "Basic Needs", type = PhraseType.FRAGMENT, iconEmoji = "📱"),

        // Feelings
        PhraseItem(text = "Tired", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "😴"),
        PhraseItem(text = "In pain", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🩹"),
        PhraseItem(text = "Cold", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🥶"),
        PhraseItem(text = "Hot", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🥵"),
        PhraseItem(text = "Thirsty", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🥤"),
        PhraseItem(text = "Hungry", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🍽️"),
        PhraseItem(text = "Happy", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "😊"),
        PhraseItem(text = "Sad", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "😢"),
        PhraseItem(text = "Uncomfortable", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "😣"),
        PhraseItem(text = "Nauseous", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🤢"),
        PhraseItem(text = "Dizzy", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "💫"),
        PhraseItem(text = "Better", category = "Feelings", type = PhraseType.FRAGMENT, iconEmoji = "🌤️"),

        // Requests
        PhraseItem(text = "Help me sit up", category = "Requests", type = PhraseType.COMPLETE, iconEmoji = "🪑"),
        PhraseItem(text = "Help me lie down", category = "Requests", type = PhraseType.COMPLETE, iconEmoji = "🛏️"),
        PhraseItem(text = "Adjust my position", category = "Requests", type = PhraseType.COMPLETE, iconEmoji = "🔄"),
        PhraseItem(text = "Turn on lights", category = "Requests", type = PhraseType.COMPLETE, iconEmoji = "💡"),
        PhraseItem(text = "Turn off lights", category = "Requests", type = PhraseType.COMPLETE, iconEmoji = "🌑"),
        PhraseItem(text = "Open the door", category = "Requests", type = PhraseType.COMPLETE, iconEmoji = "🚪"),
        PhraseItem(text = "Close the door", category = "Requests", type = PhraseType.COMPLETE, iconEmoji = "🚪"),
        PhraseItem(text = "Come here please", category = "Requests", type = PhraseType.COMPLETE, iconEmoji = "👈"),
        PhraseItem(text = "Call the doctor", category = "Requests", type = PhraseType.COMPLETE, iconEmoji = "🩺"),
        PhraseItem(text = "Call family", category = "Requests", type = PhraseType.COMPLETE, iconEmoji = "📞")
    )
}
