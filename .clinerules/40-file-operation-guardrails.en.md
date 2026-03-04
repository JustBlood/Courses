# File Operation Guardrails

## General restrictions
- Do not modify files outside task scope without explicit necessity.
- Do not edit binary/media files.
- Do not use mass auto-refactoring across the repository without a request.

## Security preferences
- For potentially risky changes, first perform impact analysis.
- Change configs and migrations only when there is an explicit requirement.
- If requirements are not clarified, do not make changes and request explicit clarification.

## Working with context-noisy files
- Everything not needed for analysis and task implementation is excluded via `.clineignore`.
- Read excluded files manually only when directly necessary.
