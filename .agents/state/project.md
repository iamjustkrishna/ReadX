# ReadX Project Overview

Last updated: 2026-08-16

## What ReadX Is

ReadX is an Android PDF reader with AI assistance. The product helps users read, understand, annotate, and ask questions about PDF documents on mobile.

Primary positioning:

> Read PDFs, understand faster, highlight smarter, and ask questions from the document.

## Target Users

- Students reading notes, textbooks, papers, and exam PDFs.
- Self-learners reading long technical or educational PDFs.
- Researchers who need quick explanations and document Q&A.
- Productivity-focused readers who want highlights, notes, and faster comprehension.

## Current App Capabilities

- Device PDF scanning.
- PDF file picker.
- PDF rendering.
- Text extraction.
- Text selection.
- Copy selected text.
- Search inside PDFs.
- Highlights and notes.
- Recent documents.
- Reading analytics.
- AI actions for selected text: simplify, explain, translate, custom prompt.
- AI document chat using retrieved page context.
- Multi-provider AI support: Groq, Gemini, OpenAI, Anthropic.
- Secure API key storage with encrypted preferences.

## Technical Shape

- Android app written in Kotlin.
- UI uses Jetpack Compose and Material 3.
- Gradle modules:
  - `:app`: Android UI, reader flow, AI integration, settings, analytics.
  - `:pdfengine`: PDF rendering, text extraction, search, selection geometry, native bridge.
- AI requests use raw OkHttp instead of provider SDKs.
- App package: `com.krishnajeena.readx`.

## Important Code Areas

- `app/src/main/java/com/krishnajeena/readx/MainActivity.kt`: main app shell and navigation state.
- `app/src/main/java/com/krishnajeena/readx/reader/ReaderViewModel.kt`: reader state, document lifecycle, selection, search, highlights, AI actions, chat.
- `app/src/main/java/com/krishnajeena/readx/ai/AiService.kt`: provider API calls.
- `app/src/main/java/com/krishnajeena/readx/ai/DocumentContextRetriever.kt`: document chat context retrieval.
- `app/src/main/java/com/krishnajeena/readx/data/SettingsRepository.kt`: AI provider settings and key storage.
- `pdfengine/src/main/java/com/krishnajeena/pdfengine/PdfEngine.kt`: public PDF engine contract and implementation.

## Product Principles

- The reader must remain useful without AI.
- AI should help understanding, not pretend to replace reading.
- Trust matters: cite document pages when possible and be clear when context is missing.
- Free users should experience the core product before being asked to pay.
- Paid value should come from repeated AI usefulness, higher limits, exports, sync, and power features.
