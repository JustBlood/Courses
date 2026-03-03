# Subagents Guidelines

## Subagent location
All subagents are located in `memory-bank/subagents`.

## Executor selection rule
- For system architecture elaboration and design: `memory-bank/subagents/system-architect.md`
- For backend architecture elaboration and design: `memory-bank/subagents/backend-architect.md`
- For prioritization, estimation, and organization of tasks: `memory-bank/subagents/sprint-prioritizer.md`
- For implementation of changes: the main agent in ACT MODE.

## When to use subagents
- When research is required before task execution.
- When you need to think through task implementation on your own.
- When sequential reading of a large volume of files overloads context.
- For preliminary research before edits.

## When NOT to use subagents
- When task requirements are not fully clear. In this case, you must ask clarifying questions to the user.
- When the best solution option is not fully clear. In this case, you need to provide solution options for the user to choose from.

## Launch rules
- You may launch subagents independently in any of your requests and are not limited in their usage.
- Formulate a narrow research question for each subagent.
- Before the initial subagent prompt, always insert the text from the invoked subagent file in `memory-bank/subagents/*`.
- Generate the main request prompt for the subagent yourself based on context provided by the user / obtained during task execution / obtained from other subagents.
- Return a concise synthesis of results to the main flow without distorting key conclusions, and include file references.

## Limitations and safety
- Consider that subagents are read-only and do not replace implementation.
- Do not run subagents for small local tasks where overhead is higher than benefit.
