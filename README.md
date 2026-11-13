# Assisted Speech AAC Prototype

An AAC (Augmentative and Alternative Communication) assistive application designed specifically for individuals with mobility challenges and speech difficulties.

---

## 🌟 Key Features

1. **Mobility-First Large Button Interface**:
   - Touch targets are expanded (minimum 90–100dp height) with high visual contrast to accommodate tremors, limited dexterity, or switch-access users.
   - Persistent top Sentence Accumulator with **Speak (Play)**, **Undo (Backspace)**, and **Clear** controls.
2. **Mode 1: Common Phrases & Sentence Fragments**:
   - Quick one-tap standard phrases (*"Yes"*, *"No"*, *"Please"*, *"I need help"*, etc.).
   - Sentence builders/starters (*"I want"*, *"I feel"*, *"Can you"*) that chain directly into fragments (*"water"*, *"tired"*, *"cold"*).
3. **Mode 2: Predictive Keyboard**:
   - Bypasses tiny standard on-screen keyboards with an oversized matrix of **adaptive next-word predictions** (e.g., Selecting *"I"* suggests `["want", "need", "am", "feel", "like", "don't"]`).
   - Clean alphabet grid fallback for spelling unpredicted words.
4. **Mode 3: Management & Customization**:
   - Add new phrases, words, and fragments dynamically.
   - Organize by category (Basic Needs, Feelings, Starters, Questions).
5. **100% Offline Speech Synthesis**:
   - **Android**: Powered by the native Android `TextToSpeech` engine, configured for local on-device speech packs without requiring an active internet connection.
   - **Interactive Prototype**: Uses the browser's local SpeechSynthesis API.

---

## 📂 Project Structure

```
assisted-speech/
├── web-prototype/
│   └── index.html                # Fully functional, zero-install interactive prototype
│
└── android-app/                  # Complete native Jetpack Compose & Room project
    ├── app/
    │   ├── build.gradle.kts
    │   └── src/main/
    │       ├── AndroidManifest.xml
    │       └── java/com/assisted/speech/
    │           ├── MainActivity.kt
    │           ├── tts/
    │           │   └── SpeechManager.kt         # Offline Android TextToSpeech engine
    │           ├── data/
    │           │   ├── PhraseEntity.kt          # Room entities & DAOs
    │           │   ├── AppDatabase.kt           # Room DB with initial pre-populated seeds
    │           │   └── PredictionEngine.kt      # Offline Markov / N-gram word predictor
    │           └── ui/
    │               ├── SpeechViewModel.kt       # StateFlow MVVM manager
    │               └── MainScreen.kt            # Jetpack Compose UI (Large targets, all 3 modes)
    ├── gradle/
    │   └── libs.versions.toml                   # Modern Gradle Version Catalog
    ├── build.gradle.kts
    └── settings.gradle.kts
```

---

## 🚀 How to Run

### 1. Test the Prototype Immediately (Zero-Install)
Open [web-prototype/index.html](file:///c:/Users/graham.ECCLESTON/source/repos/assisted-speech/web-prototype/index.html) directly in any modern browser (Chrome, Edge). It has already been launched for you, allowing you to try sentence assembly, speech playback, predictions, and adding new phrases right away.

### 2. Android Native Build (Android Studio)
1. Open **Android Studio**.
2. Select **File > Open** and point to the `assisted-speech/android-app` folder.
3. Android Studio will resolve the Gradle version catalog (`libs.versions.toml`) and download dependencies.
4. Run on an Android device or tablet emulator.
