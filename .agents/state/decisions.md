# Decisions

Last updated: 2026-08-16

## Agent System

- Use a repo-local `.agents` folder as the source of truth for agent roles, rules, workflows, skills, and state.
- Keep state in Markdown files so humans and agents can read and update it without special tooling.
- Add a dedicated `state-keeper` agent instead of expecting every specialist to maintain memory perfectly.

## Product Positioning

- Position ReadX as an AI-powered PDF reader for understanding and studying, not as a generic file manager.
- Use the promise: read PDFs, understand faster, highlight smarter, and ask questions from the document.
- Keep the reader useful without AI so the product does not feel locked behind paid AI usage.

## Monetization

- Start free-first.
- Keep basic reading, search, highlights, notes, and limited AI available for free.
- Charge for repeated AI value, higher limits, full document chat, longer context, exports, sync, and power features.
- Avoid making themes or cosmetic features the core paid value.

## Marketing

- Show concrete app demos rather than abstract productivity claims.
- Focus emotion on relief from confusion, momentum while studying, and confidence while reading.
- Use transparent AI messaging: AI assists reading and cites context when possible.

## Social Automation

- Drafting and asset preparation are allowed by default.
- Direct posting must require explicit final user approval.
- Platform API posting is a later setup step because accounts, tokens, app review, and API access vary by platform.
