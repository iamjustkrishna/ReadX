# Current State

Last updated: 2026-08-16

## Repo State

- `.agents` existed with empty `agents`, `rules`, `skills`, and `workflows` directories.
- `.agents/state` has now been introduced as the durable project memory area.
- No `.codex` folder exists in this repo.
- The app code already has uncommitted changes in Android, reader, AI, settings, and UI files. Treat them as user/project work unless explicitly asked to edit them.

## Current Product State

ReadX is a functional Android PDF reader with AI-assisted reading features in progress. The app appears to include PDF scanning, reading, search, selection, highlights, notes, AI actions, document chat, settings, and analytics.

## Current Agent System State

The agent system is now file-based under `.agents`.

Implemented docs:

- project memory
- current state
- backlog
- decisions
- launch state
- agent role definitions
- operating rules
- marketing and social rules
- workflow playbooks
- content and pricing frameworks

## Known Risks

- Some strings in existing app output appear to have encoding artifacts. Do not rewrite unrelated UI text unless the task is specifically about text cleanup.
- API model lists may become stale over time. Verify current model names before release.
- Direct social posting is not connected yet and requires platform-specific accounts, app credentials, API permissions, and explicit approval.
- Instagram publishing requires a professional account and Meta app setup.
- X posting requires X developer access and usage-based API access.
- Canva automation requires Canva Connect API auth/scopes or an MCP/plugin that exposes Canva actions.

## Next Useful Actions

- Review and refine agent files after first real use.
- Add a launch screenshot checklist once current UI is stable.
- Add a weekly marketing calendar before public launch.
- Decide the first paid tier limits after observing AI usage and cost.
