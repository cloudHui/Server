---
name: java-tool-builder
description: Create a standalone Java tool/utility project with proper Maven structure, build config, and runnable JAR packaging
---

# Java Tool Builder

When the user asks to create a new standalone Java tool or utility (not part of the main Server modules).

## Context

Based on past sessions (deskcheck tool, 214 tool calls), creating a Java tool from scratch follows a repeatable pattern. The user typically wants:
- A standalone tool in a separate directory (e.g., `D:/code/<toolname>/`)
- Maven-based build with `pom.xml`
- Executable JAR packaging (not Spring Boot — plain `main()` with shade/assembly)
- GUI capability (AWT/Swing) when needed
- External library integration (OCR, image processing, etc.)

## Procedure

### 1. Project scaffolding
```
D:/code/<toolname>/
├── pom.xml
├── src/main/java/com/<toolname>/
│   ├── App.java          (entry point with main())
│   ├── ui/               (if GUI needed)
│   ├── core/             (business logic)
│   └── config/           (configuration)
├── src/main/resources/
│   └── config.properties
└── README.md
```

### 2. POM essentials
- Set `<packaging>jar</packaging>`
- Use `maven-shade-plugin` for fat JAR with `Main-Class` in manifest
- Set `<maven.compiler.source>8</maven.compiler.source>` for broad compatibility
- Add dependencies as needed (common ones: `opencv`, `tess4j`, `slf4j`)

### 3. Build & test cycle
- Build: `mvn package -DskipTests -q`
- Run: `java -jar target/<toolname>-<version>.jar`
- Check JAR contents: `jar -tf target/<toolname>.jar | grep -i <pattern>`

### 4. Iteration pattern (learned from deskcheck session)
The user will iterate heavily. Expect:
- Multiple rounds of build → run → error → fix
- Config file issues (resources not included in JAR)
- External library native path problems
- GUI event threading issues

## Key lessons from past sessions

- Always check if config files end up inside the JAR: `jar -tf target/*.jar`
- For native libraries (OpenCV, Tesseract), set `java.library.path` or use absolute paths
- When using `maven-shade-plugin`, exclude `META-INF/*.SF` to avoid signature issues
- Test the JAR immediately after each build — don't accumulate untested changes
