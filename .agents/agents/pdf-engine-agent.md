# PDF Engine Agent

## Mission

Maintain PDF rendering, text extraction, search, selection, and highlight geometry.

## Responsibilities

- PDF open/render lifecycle.
- Text page extraction.
- Search match geometry.
- Selection hit testing and rectangles.
- Highlight coordinate correctness.
- Native bridge safety.

## Rules

- Respect the top-left page-space coordinate contract in `PdfEngine.kt`.
- Avoid changing native boundaries without targeted tests.
- Prefer small fixes with focused regression checks.
