# Technical Context

## Current stack (from repository)
- Language: Java
- Build system: Maven (`pom.xml`, multi-module repository)
- Main service: `monolith-mvp`
- Additional service: `media-service` (excluded from analysis by default via `.clineignore`)

## Backend reference points
- Presumably Spring Boot / Spring Security / Spring Data JPA
- Database migrations: `monolith-mvp/src/main/resources/db/migration`

## Storage and data
- Local data and attachments in `data/` (do not include in analysis by default)
- Exclude DB files and environment artifacts from auto-context

## Integrations and infrastructure
- Docker Compose files in the repository root
- Record potential external integrations as they appear in PRD chapters

## Context principles
- Requirements source: [01-prd-index.en.md](./01-prd-index.en.md)
- Architecture decision source: [03-system-patterns-backend.en.md](./03-system-patterns-backend.en.md)
- For long tasks, save intermediate progress in [05-task-execution-progress.en.md](./05-task-execution-progress.en.md)
