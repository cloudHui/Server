---
name: maven-module-dependency
description: Diagnose and fix Maven multi-module dependency issues (missing deps, version conflicts, build failures across pom.xml files)
---

# Maven Multi-Module Dependency Fix

When the user reports Maven build failures, missing dependencies, or POM issues across the Server project's multi-module structure.

## Context

This project (`D:\code\Server`) is a multi-module Maven project with modules:
- `center/`, `game/`, `gate/`, `hall/`, `room/`, `robot/`, `tool/`

Each has its own `pom.xml`. A dependency change in one module may require changes in others.

## Procedure

1. **Read the error message** — extract the missing class, method, or artifact from the user's report or build output.

2. **Identify the source module** — `Grep` the project for the missing class/interface to find which module owns it.

3. **Check existing POMs** — `Read` the `pom.xml` of both the failing module and the source module. Look for:
   - Missing `<dependency>` entry
   - Wrong `<version>`
   - Missing `<scope>` (compile vs provided)
   - Parent POM `<dependencyManagement>` mismatches

4. **Fix systematically** — when adding a dependency to one module, check ALL sibling modules that also reference that source. Use `Grep` to find all `import` statements referencing the changed package across modules.

5. **Verify** — run `mvn compile -pl <module> -am` to build only the affected module and its dependencies. If the whole project is small, use `mvn compile` on the root.

## Key files

- Root POM: `D:\code\Server\pom.xml`
- Module POMs: `D:\code\Server\{center,game,gate,hall,room,robot,tool}\pom.xml`
- Druid config: `D:\code\Server\hall\src\main\resources\default_druid.xml`

## Common patterns seen

- `hall` module's `UserService`/`UserDao` changes require updating `hall/pom.xml` and potentially `game/pom.xml`
- `game` module's table/card classes are referenced by `room` — cross-module deps exist
- When adding MyBatis mappers, update both the XML mapper file AND the `default_druid.xml` registration
