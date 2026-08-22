# ReadX Agent System

This project uses an agent persona and workflow framework located in `.agents/`.

## Active Rules & Operating Protocol
- Follow the rules defined in `.agents/rules/`.
- Before modifying a subsystem, consult the specialist agent persona in `.agents/agents/` as referenced in `.agents/rules/agent-roles.md`.
- Maintain project memory by updating `.agents/state/current.md` and `.agents/state/decisions.md` when completing significant milestones.
- Use standard skills indexed in `.agents/skills/`.

## Autonomous Execution
- Proactively run builds, tests, and device deployments without asking for manual confirmation beforehand.
- Automatically diagnose compile errors and iterate until verified.
