# System Patterns (Backend)

## Purpose
Capture backend architecture and code patterns so the agent makes consistent decisions across all tasks.

## Core patterns (draft)
- Architecture style: modular backend in Java (Spring Boot).
- Layers: `controller -> service -> repository -> model`.
- DTO/mapping: use dedicated DTOs and mappers, do not expose entities directly.
- Errors and validation: centralized handling via global exception handler.
- Database migrations: only through versioned scripts in `db/migration`.

## Change principles
- Minimal invasive changes (do not refactor unrelated code outside task scope).
- Analyze impact across layers and dependencies first.
- Preserve backward API compatibility unless otherwise agreed in PRD.

## TODO for clarification
- Clarify target DDD module boundaries.
- Clarify transactionality and idempotency rules by scenario.
