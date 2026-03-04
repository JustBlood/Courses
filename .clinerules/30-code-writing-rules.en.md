# Code Writing Rules

## General principles
- Make only targeted changes within task scope.
- Preserve the project's existing style and architectural boundaries.
- Do not mix functional changes and mass refactoring without explicit necessity.

## For backend (Java)
- Follow layering: `controller -> service -> repository -> model`.
- Use DTO/mapper instead of directly exposing entities externally.
- Do not break backward compatibility of contracts without an explicit requirement in PRD.
- Use SOLID principles in your implementation.
- Use DRY and YAGNI principles in your implementation.
- Do not overcomplicate code with excessive optimization.
- After task completion, format changed files according to Ctrl+Alt+L in IDEA and clean up unused imports.
- For implemented functionality, write integration tests or add implemented logic to existing ones.

## Change quality
- Before changes, assess impact on adjacent modules and functionality.
- After changes, check compilation and tests of the relevant module.
- Document important architectural decisions in `memory-bank/06-system-development-progress.md`.

## Avoid excessive elaboration (over-engineering)
Make only the changes that are directly requested or clearly necessary. Keep solutions simple and focused:
- Scope: Do not add features, do not refactor code, and do not make "improvements" beyond what was proposed. Fixing a bug does not require cleanup of surrounding code. A simple function does not require additional configuration.
- Documentation: Do not add documentation lines, comments, or type annotations to code you did not change. Add comments only where logic is not obvious.
- Error handling: do not add error handling, fallbacks, or checks for scenarios that cannot happen. Trust internal code and platform guarantees.
- Abstractions: Do not create helper classes, utilities, or abstractions for one-time operations. Do not design for hypothetical future requirements. The required complexity level is the minimum necessary for the current task.

## Testing
Write a high-quality general-purpose solution using available standard tools. Do not create helper scenarios or workarounds to complete the task more efficiently. Implement a solution that works correctly for all valid input data, not only for test examples. Do not hardcode values and do not create solutions that work only for specific test input data. Instead, implement real logic that solves the problem in general.

Focus on understanding task requirements and implementing the correct algorithm. Tests are meant to verify correctness, not to define the solution. Ensure a principled implementation aligned with software development best practices and principles.

If the task is unreasonable or infeasible, or if any tests are incorrect, please report this to me rather than trying to bypass them. The solution must be reliable, maintainable, and extensible.

## Working with code
Never reason about code you have not opened. If the user references a specific file, you must read it before responding. Be sure to study relevant files before answering questions about the codebase. Never make any claims about code before investigation, unless you are sure of the correct answer — provide grounded answers that avoid hallucinations.

## Questions about code
If you have any questions regarding task execution, wording, or mismatches between requirements and task, ask the user immediately and do not start task execution without full understanding of all requirements and constraints.
