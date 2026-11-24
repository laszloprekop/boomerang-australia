# Boomerang Australia, home exam

A re-engineering of `app/src/main/java/BoomerangAustralia.java` in Kotlin, for the course Z7005E.
The work is graded on design, and the author must be able to explain every line at an oral exam.

## Development process

Work proceeds through these stages. Do not skip stages. A human approves each stage.

1. Requirements
2. Architecture
3. Tests
4. Implementation
5. Review

## Sources of truth

- `docs/requirements.md` defines required behaviour.
- `docs/ARCHITECTURE.md` defines responsibilities and dependency rules.
- `app/src/test/kotlin/` contains executable specifications.

## Environment

- Work only inside this folder.
- Kotlin on JDK 21, Gradle wrapper. Verify with `./gradlew test`.
- Libraries: the Kotlin standard library and JUnit only. Ask before adding anything.
- `app/src/main/java/BoomerangAustralia.java` is the original. Never change it.
- Permissions: Claude Code runs here with auto-accept, its current default, so commands and edits are
  not approved one by one. The human approves each stage instead, and reads the diff of every round
  before the next one starts. Stop at the end of each stage or round and wait.

## General rules

- Prefer the simplest design that satisfies the requirements.
- No change of behaviour beyond what the requirements ask for.
- Game logic is independent of console, network and storage.
- An interface only where a requirement or a named future change needs one.
- Small rounds. After each round, list what changed and why.
- Run the full test suite before claiming a task is complete.

## Test policy

Implementation work must not modify, delete, skip or disable existing tests, or weaken assertions.
If a test appears wrong: stop, explain the conflict, and ask for human approval.

## Architecture policy

Implementation must respect `docs/ARCHITECTURE.md`. If the architecture appears insufficient:
explain the problem, propose a change, and never make the change silently.
