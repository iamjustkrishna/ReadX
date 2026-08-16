# ReadX Agent System

This folder is the operating memory and workflow layer for ReadX.

Use it to keep agents aligned on:

- what the app is
- what exists in the codebase
- what rules matter
- what workflows to follow
- what marketing, pricing, and content strategy to use
- what changed recently

## Folder Map

- `agents/`: role definitions for specialist agents.
- `rules/`: durable rules every agent should follow.
- `skills/`: reusable strategy and content frameworks.
- `workflows/`: repeatable execution playbooks.
- `state/`: current project memory and decision history.

## Default Operating Rule

Before doing meaningful work, read:

1. `state/project.md`
2. `state/current.md`
3. relevant files in `rules/`
4. the agent or workflow file that matches the task

After meaningful work, update:

1. `state/current.md`
2. `state/decisions.md` if a lasting decision was made
3. `state/backlog.md` if priorities changed
4. `state/launch.md` if release or marketing status changed
