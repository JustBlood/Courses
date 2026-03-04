# Memory Bank Workflow

## Token Budget Gate (mandatory)
- After EVERY action (tool call / reasoning step or code-writing step), the agent must:
    1. Check the current Context Window Usage value.
    2. If usage > 350000 tokens:
        - immediately pause implementation;
        - record the current context in full detail in `memory-bank/02-active-context.md`, so that when started, a new agent can fully understand the ongoing task and which files it needs to analyze;
        - end the iteration with the message "requires /newtask to continue".
- Continuing execution without this checkpoint when the limit is exceeded is forbidden.
- Whenever possible, optimize context usage as much as possible — do not read unnecessary files, do not fill context with unnecessary or not-yet-needed information; this is CRITICALLY important for your correct operation:
  - When running commands, do not analyze their full output — search only for the information you need. You can save output to a file and then search through it, or apply other optimizations that will preserve your context.

## Mandatory reading at the start of each task
1. all files from `.clinerules`, applying ALL files listed there
2. `memory-bank/00-project-brief.en.md`
3. `memory-bank/02-active-context.md`
4. `memory-bank/01-prd-index.en.md`
5. `memory-bank/05-task-execution-progress.en.md`

## Mandatory requirements for file updates
- After task analysis, you must update `memory-bank/02-active-context.md`.
- Upon FULL task completion, you must copy the entire active context from `memory-bank/02-active-context.md` into an artifact file for the completed task in the `memory-bank/task-artifacts` directory, and must add a link to the artifact in `memory-bank/05-task-execution-progress.en.md`.
- Upon FULL task completion, update `memory-bank/06-system-development-progress.md` if the task included architecturally significant changes.
- Full cleanup and editing of previous tasks in `memory-bank/05-task-execution-progress.en.md` is forbidden.
- After FULL task completion, creation of its artifact, and adding it to the index file `memory-bank/05-task-execution-progress.en.md`, you must clear `memory-bank/02-active-context.md`.

## For long tasks
1. Save intermediate context in `memory-bank/02-active-context.md`.
2. Continue work through a new task (`/newtask`) with these documents loaded.
