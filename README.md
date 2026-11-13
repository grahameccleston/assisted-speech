# Assisted Speech AAC — Motor-Accessible & Adaptive Android App

An offline-first Augmentative and Alternative Communication (AAC) Android application engineered for individuals with motor control challenges (tremors, limited dexterity, fatigue) and speech difficulties.

---

## 🌟 Core Capabilities

1. **Minimal Presses & Zero-Scroll Motor Accessibility**:
   - **Fixed-Page Large-Target Grid**: Displays 6 oversized buttons per page (`2×3` in portrait, `3×2` in landscape) with dedicated `◀ PREV` / `MORE ▶` page buttons—completely eliminating error-prone drag scrolling.
   - **1-Tap Auto-Speak & Smart Shortcuts Strip**: Dedicated top strip of **2 adaptive 1-tap complete sentences** that speak immediately on a single press. When navigating inside any starter branch (e.g., *"Can you help me to…"*), the strip dynamically updates to show the top 2 full-sentence completions from that branch, turning 4-step paths into **2 presses total**.
   - **Tremor Debounce & Brush-Touch Protection** (`AccessibleTouch.kt`): Configurable screen-wide double-tap lockout (`150ms`–`650ms`), minimum press-hold threshold (`0ms`–`180ms`), and generous tremor slop margin so unsteady fingers trigger cleanly on release.
   - **Ergonomic Reach Zone**: Places `SPEAK / REPEAT`, `BACK`, `UNDO`, `MORE`, `SPELL`, and the `🔔 Attention Bell` in the easiest-to-reach zone (`Bottom Reach`, `Right Hand`, or `Left Hand`), while keeping the high-contrast sentence display at the top for unobstructed visibility.

2. **4-Signal On-Device Adaptation (`PredictionEngine.kt`)**:
   - **Caregiver-Pinned Priority Boost (`★`)**: Keeps critical starters and pinned phrases in stable muscle-memory slots on Page 1.
   - **Frecency**: Logarithmic usage frequency weighted by exponential recency decay.
   - **Time-of-Day Routines**: Automatically tracks usage across `MORNING`, `AFTERNOON`, `EVENING`, and `NIGHT` buckets to surface routine requests at the right hour.
   - **Conversational Follow-Up Context**: Boosts natural follow-up responses based on utterances spoken within the last 3 minutes.
   - **Personal N-Gram Learning**: Learns the user's custom word transitions (`UserNgramEntity`) and full sentences (`UtteranceLogEntity`) every time they speak.

3. **Unified Flow & Accessible Spelling**:
   - Seamlessly switch into `🔤 SPELL` at any point while building a phrase.
   - Replaces tiny 26-key keyboards with **4 oversized word predictions** + a **12-key paged large-button alphabet** that dynamically highlights the **Top 4 most likely next letters** in gold and dims impossible letters.

4. **Multi-Language Extensibility (`assets/lang/{locale}/pack.json`)**:
   - All vocabulary, concept hierarchies, N-grams, keyboard layouts, diacritics, and grammatical slot templates (`{grammar:...}`, `{gender:male|female|neutral}`) live in declarative JSON language packs.
   - Nodes are keyed by language-neutral **Concept IDs** (`c_help_sit_up`, `w_drk_water`) so learned user history and custom branches survive locale changes.

5. **3-Second Hold Caregiver Lock**:
   - Protects customization and motor settings behind a continuous 3-second hold button (`🔒 Caregiver (Hold 3s)`) so hand tremors can never accidentally delete phrases or alter settings.
   - Inside Caregiver mode, custom phrases can be attached to **any branch in the tree** or pinned to the main screen.

---

## 📂 Project Structure

```text
assisted-speech/
└── android-app/
    └── app/src/main/
        ├── assets/lang/en/
        │   └── pack.json                         # Externalized English Language Pack (Concept Tree, N-grams, Keyboard)
        └── java/com/assisted/speech/
            ├── MainActivity.kt
            ├── tts/
            │   └── SpeechManager.kt              # Offline Android TextToSpeech + Attention Bell Chime
            ├── data/
            │   ├── LanguagePack.kt               # JSON Language Pack loader & Grammatical Slot Composer
            │   ├── PhraseEntity.kt               # Unified Room Entities (PhraseNode, UtteranceLog, UserNgram)
            │   ├── AppDatabase.kt                # Room DB v4 seeded from Language Pack
            │   └── PredictionEngine.kt           # 4-Signal Scorer, Smart Shortcuts & Next-Letter Predictor
            └── ui/
                ├── AccessibleTouch.kt            # Tremor Debounce Gate, Hold-Threshold Button & 3s Caregiver Lock
                ├── SpeechViewModel.kt            # Reactive StateFlow & On-Device Learning Coordinator
                └── MainScreen.kt                 # Zero-Scroll Paged UI, Ergonomic Reach Dock & Caregiver Editor
```
