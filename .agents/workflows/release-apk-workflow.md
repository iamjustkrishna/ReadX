# Release APK Workflow

## Steps

1. Read `state/launch.md`.
2. Check git status for unrelated changes.
3. Run `.\gradlew.bat assembleDebug`.
4. If preparing release, verify version name/code and signing expectations.
5. Smoke test core flows.
6. Record artifact path and blockers in `state/launch.md`.

## Smoke Checklist

- App launches.
- File picker opens a PDF.
- Device scan works when permission is granted.
- Search works.
- Text selection works.
- Highlight and note flow works.
- AI settings and one AI action work.
- Document chat works or shows a clear API-key error.
