# Task Flow and Modes

## Rule priority
- For all token budget, checkpoint, and memory-bank update rules, `.clinerules/10-memory-bank-workflow.en.md` has priority.
- If this file conflicts with `.clinerules/10-memory-bank-workflow.en.md`, follow `.clinerules/10-memory-bank-workflow.en.md`.

## What is a task
A task is an atomic change or enhancement to the system under development. It is limited by goal, criteria, and changes.

## Target task execution flow
1. Read mandatory documents from `10-memory-bank-workflow.en.md`.
2. Analyze the user's task. If the user request explicitly says to take a task from `memory-bank/tasks.json` — take it. Execute the task according to the guideline from the `agent_instructions` section of `memory-bank/tasks.json`.
3. Before starting task implementation, perform solution analysis and design using subagents.
4. Capture task boundaries, completion criteria, and risks. If a subagent is used, pass this task to it.
5. Create a work plan. If a subagent is used, get its analysis and form the work plan based on it. Capture full detailed context and the plan in `.clinerules/02-active-context.md`.
6. Perform implementation/changes after the user approves the plan.
7. If the Token Budget Gate rule triggers, follow the instructions for that case in `.clinerules/02-active-context.md`.
8. Upon FULL task completion, you must copy the entire active context from `memory-bank/02-active-context.md` into an artifact file for the completed task in the `memory-bank/task-artifacts` directory, and must add a link to the artifact in `memory-bank/05-task-execution-progress.en.md`.
9. To view full task information, you can use prepared Python scripts: `memory-bank/find_task.py` (example call: `python find_task.py TASK-001`) and `memory-bank/change_task_status.py` (example call: `python change_task_status.py TASK-001 done`, status options = pending, done, obsolete). Prefer these scripts over reading the full `memory-bank/tasks.json` file.

## Task analysis
1. Read mandatory memory-bank context.
2. Identify relevant PRD chapters.
3. Create an implementation plan.
4. Use subagents according to rules from `.clinerules/50-subagents-guidelines.en.md`.
5. Capture conclusions in `02-active-context.md`.

## Mandatory elements to think through before implementation
- Goal and expected result.
- Boundaries (what is included / not included).
- Requirement source (PRD/comments).
- Completion and verification criteria.
- Risks and dependencies.

## Rule for long tasks
- For long tasks and context limit overflow, apply the mandatory `Token Budget Gate` from `.clinerules/10-memory-bank-workflow.en.md`.
