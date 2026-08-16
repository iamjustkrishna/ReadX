# ReadX

ReadX is a modern, fast, and serene Android PDF reader built from scratch with Jetpack Compose and native C++ rendering. It combines butter-smooth 60fps page rendering with conversational AI assistants that help you summarize, analyze, and query your documents in real time.

---

## Highlights and Features

### 1. Ultra-Fast Native PDF Rendering
- **Native Engine**: Powered by Google Pdfium compiled directly with CMake and JNI for crisp vector rendering and fast text extraction.
- **Fluid Reading Experience**: Smooth page transitions, zoom controls, inverted dark mode for comfortable night reading, and precise text selection.

### 2. Context-Aware Document AI Chat
- **Search-Augmented Keyword Retrieval**: Ask questions about any PDF regardless of its length. ReadX uses an intelligent in-memory keyword and proximity ranking engine to find the most relevant pages and feed them into the AI context without draining your battery or storage with heavy local vector databases.
- **Page Citations**: Answers cite exact page sources so you can jump directly to where the information lives.
- **Multi-Provider AI**: Connect with your choice of AI provider:
  - Groq (Default: Llama 3.3 70B, Llama 3.1 8B, DeepSeek R1 Distill 70B)
  - Google Gemini (Gemini 2.5 Flash, Gemini 2.5 Pro, and Gemini 2.0 Flash)
  - OpenAI (GPT-4o mini, GPT-4o, and o3-mini)
  - Anthropic (Claude 3.5 Sonnet, Claude 3.5 Haiku, and Claude 3 Opus)

### 3. Real Reading Analytics
- **Live Tracking**: Measures real time spent reading each day, tracking unique pages turned and completed books.
- **Weekly Progress**: Clean visual bar graphs and reading streaks to keep you motivated.

### 4. Smart Highlights and Notes
- Highlight text in multiple colors (Yellow, Green, Blue, Pink, Purple).
- Attach custom notes to any passage.
- Search and review all your notes in a single unified drawer.

### 5. Document Management and Library
- Auto-scans device storage for PDF documents.
- Cover thumbnail generation for fast visual browsing.
- Sort and filter by size (largest/smallest), date modified (newest/oldest), or name.
- Favorites system to bookmark your most important reads.

### 6. Dynamic Remote Model Configuration
- AI models evolve quickly. ReadX fetches an updated model configuration file from GitHub on startup.
- If an AI provider decommissions a model, the app automatically switches to the new active default without crashing or showing error codes.
- Works 100% offline using cached data and built-in fallbacks.
- Includes a weekly GitHub Actions workflow that keeps model IDs fresh automatically.

---

## Tech Stack and Architecture

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose with Material 3 design system
- **Architecture**: MVVM with unidirectional data flow (StateFlow / SharedFlow)
- **PDF Core**: C++ Pdfium with custom JNI bindings (`pdfengine` module)
- **Network**: OkHttp for lightweight direct REST API calls (no bloated SDK dependencies)
- **Security**: Android Jetpack Security (EncryptedSharedPreferences) for safe storage of API keys
- **Build System**: Gradle 8.5+ with CMake 3.22.1

---

## Project Structure

```
ReadX/
├── app/
│   ├── src/main/java/com/krishnajeena/readx/
│   │   ├── ai/               # Context retrieval engine and multi-provider AI client
│   │   ├── data/             # Analytics, PDF scanner, settings, and dynamic config repos
│   │   ├── reader/           # Compose reader screens, text selection, and gesture handling
│   │   ├── ui/               # Home, library, profile, AI chat, and bottom sheets
│   │   └── MainActivity.kt   # App entry point and navigation coordinator
├── pdfengine/                # Native module with C++ Pdfium wrapper and JNI code
├── scripts/                  # Python helper scripts for syncing model catalogs
├── .github/workflows/        # Automated weekly cron jobs for AI model updates
└── ai_models_config.json     # Remote model catalog file
```

---

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 34 / 35
- NDK and CMake (installed via Android Studio SDK Manager)
- JDK 17 or 21

### Building and Running
1. Clone the repository:
   ```bash
   git clone https://github.com/thekrishnajeena/ReadX.git
   cd ReadX
   ```

2. Open the project in Android Studio.

3. Build and install on your device or emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## License

This project is created for personal and open-source learning. Feel free to explore, fork, and adapt it for your own reader projects.
