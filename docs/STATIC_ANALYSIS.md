# Static Code Analysis

This document describes the static code analysis setup for the Smart Lighting project.

## Backend (Java/Gradle)

The backend uses three industry-standard static analysis tools:

### Tools

| Tool | Version | Purpose |
|------|---------|---------|
| Checkstyle | 10.20.1 | Code style enforcement (based on Google Java Style) |
| PMD | 7.7.0 | Code quality and bug detection |
| SpotBugs | 4.8.6 | Bug pattern detection (FindBugs successor) |

### Installation

No additional installation required. Tools are configured as Gradle plugins and will be downloaded automatically on first run.

### Configuration Files

```
backend/
├── config/
│   ├── checkstyle/
│   │   └── checkstyle.xml      # Checkstyle rules
│   ├── pmd/
│   │   └── pmd-rules.xml       # PMD rules
│   └── spotbugs/
│       └── spotbugs-exclude.xml # SpotBugs exclusions
└── build.gradle.kts            # Plugin configuration
```

### Running Analysis

Run all analysis tools:

```bash
cd backend
./gradlew analyze
```

Run individual tools:

```bash
./gradlew checkstyleMain    # Checkstyle only
./gradlew pmdMain           # PMD only
./gradlew spotbugsMain      # SpotBugs only
```

### Reports

HTML reports are generated in:

- `backend/build/reports/checkstyle/main.html`
- `backend/build/reports/pmd/main.html`
- `backend/build/reports/spotbugs/spotbugsMain.html`

### Rule Configuration

**Checkstyle** enforces:
- Naming conventions (classes, methods, variables)
- Import organization (no star imports, no unused imports)
- Line length (max 120 characters)
- Method length (max 80 lines)
- Block and whitespace formatting

**PMD** checks for:
- Unused code (variables, parameters, methods)
- Code complexity (cognitive complexity max 25)
- Error-prone patterns (null checks, empty blocks)
- Performance issues (string concatenation, inefficient collections)
- Security issues (hardcoded credentials)

**SpotBugs** detects:
- Null pointer dereferences
- Resource leaks
- Concurrency issues
- Security vulnerabilities
- Correctness bugs

## Frontend (Vue.js/JavaScript)

The frontend uses ESLint with Vue-specific plugins and Prettier for formatting.

### Tools

| Tool | Purpose |
|------|---------|
| ESLint 8.x | JavaScript/Vue linting |
| eslint-plugin-vue | Vue 3 specific rules |
| Prettier 3.x | Code formatting |

### Installation

```bash
cd frontend
npm install
```

### Configuration Files

```
frontend/
├── .eslintrc.cjs       # ESLint configuration
├── .prettierrc.json    # Prettier configuration
└── .prettierignore     # Prettier ignore patterns
```

### Running Analysis

```bash
cd frontend

# Lint and auto-fix
npm run lint

# Lint without auto-fix (CI mode)
npm run lint:check

# Format code
npm run format

# Check formatting (CI mode)
npm run format:check
```

### Rule Configuration

**ESLint** enforces:
- Vue 3 best practices (component naming, emit declarations)
- No unused variables
- Prefer `const` over `let`
- Use template literals
- Strict equality (`===`)
- Maximum nesting depth (4 levels)
- Maximum function complexity (15)

**Prettier** enforces:
- No semicolons
- Single quotes
- 2-space indentation
- No trailing commas
- 100 character line width

## Suppressing Warnings

### Backend

Use `@SuppressWarnings` annotation:

```java
@SuppressWarnings("PMD.TooManyMethods")
public class MyService { }
```

Or configure exclusions in the respective config files.

### Frontend

Use ESLint comments:

```javascript
// eslint-disable-next-line no-unused-vars
const unusedButNeeded = 'value'
```

## Troubleshooting

### Backend

If Gradle fails to download plugins:
```bash
./gradlew --refresh-dependencies analyze
```

### Frontend

If ESLint shows parsing errors:
```bash
rm -rf node_modules
npm install
```

