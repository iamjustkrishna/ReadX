# Android Product Rules

## Reader Experience

- Basic PDF reading must work without AI.
- Search, selection, highlights, and notes are core reader features.
- AI should be accessible but not interrupt reading.
- Permissions must be explained clearly and respectfully.

## Compose UI

- Follow existing Compose and Material 3 patterns.
- Keep screens dense enough for real use on mobile.
- Avoid decorative UI that makes reading harder.
- Use clear empty, loading, and error states.

## AI

- Cite page references when document context is used.
- Be clear when an AI answer is outside retrieved context.
- Never expose API keys in logs, UI, or tracked files.
- Prefer provider-agnostic flows where practical.
