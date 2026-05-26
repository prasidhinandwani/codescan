# CodeScan – Java Static Code Analyzer

A command-line tool written in Java that scans `.java` source files for common code quality violations — unused imports, overly long methods, and missing Javadoc documentation. Built to mirror the core functionality of enterprise static analysis tools like AutoRABIT CodeScan and SonarQube.

---

## Table of Contents

- [Features](#features)
- [Project Structure](#project-structure)
- [Design & Architecture](#design--architecture)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Example Output](#example-output)
- [Analysis Rules](#analysis-rules)
- [Running Tests](#running-tests)
- [Tech Stack](#tech-stack)
- [Resume Context](#resume-context)

---

## Features

- Scans a single `.java` file or an entire directory recursively
- Three built-in analysis rules out of the box
- Color-coded terminal output with per-severity issue counts
- Configurable method length threshold
- Toggle individual rules on or off via CLI flags
- CI/CD friendly exit codes (`0` = clean, `1` = issues found, `2` = error)
- Zero external runtime dependencies — ships as a single JAR

---

## Project Structure

```
codescan/
├── pom.xml
└── src/
    ├── main/java/com/codescan/
    │   ├── Main.java                        ← CLI entry point & argument parsing
    │   ├── CodeScanner.java                 ← Orchestrates file discovery & scanning
    │   ├── analyzer/
    │   │   ├── Analyzer.java                ← Interface — contract for all rules
    │   │   ├── UnusedImportAnalyzer.java    ← Detects unused/wildcard imports
    │   │   ├── LongMethodAnalyzer.java      ← Flags methods exceeding line threshold
    │   │   └── MissingJavadocAnalyzer.java  ← Checks public API documentation
    │   ├── model/
    │   │   ├── Issue.java                   ← Immutable value object for a single issue
    │   │   ├── IssueSeverity.java           ← Enum: CRITICAL / WARNING / INFO
    │   │   └── ScanResult.java             ← Aggregates issues across all files
    │   └── reporter/
    │       └── ConsoleReporter.java         ← ANSI color output + summary block
    └── test/java/com/codescan/
        ├── UnusedImportAnalyzerTest.java    ← 8 unit tests
        ├── LongMethodAnalyzerTest.java      ← 10 unit tests
        ├── MissingJavadocAnalyzerTest.java  ← 9 unit tests
        ├── ScanResultTest.java              ← 6 unit tests
        └── CodeScannerIntegrationTest.java  ← End-to-end tests with real temp files
```

---

## Design & Architecture

### Open/Closed Principle — Extensible Analyzer Pipeline

The core design decision is the `Analyzer` interface:

```java
public interface Analyzer {
    List<Issue> analyze(List<String> lines, String filePath);
    String getName();
}
```

Every rule is an independent, stateless class that implements this interface. Adding a new rule — say, detecting magic numbers or empty catch blocks — requires zero changes to existing code. You write the new class and register it in `Main.java`. This directly mirrors how SonarQube and CodeScan manage their rule sets.

### Separation of Concerns

| Layer | Class | Responsibility |
|---|---|---|
| Entry point | `Main` | Parse CLI args, wire dependencies |
| Orchestration | `CodeScanner` | Walk files, run analyzers |
| Analysis | `*Analyzer` | Detect issues in source lines |
| Data | `Issue`, `ScanResult` | Immutable, value-semantic models |
| Presentation | `ConsoleReporter` | Render output — no logic |

The scanner never prints anything. The reporters never scan anything. Neither knows about the other's internals.

### Stateless Analyzers

Every analyzer is stateless — no instance fields are mutated during analysis. A single instance can safely analyze thousands of files sequentially or in a parallel stream without synchronization.

### Analysis Approach

Rather than building a full AST (which would require a parser like ANTLR or JavaParser), the tool uses **regex-based line analysis with brace-depth tracking**. This is intentionally lightweight:

- `UnusedImportAnalyzer` — two-pass: collect import lines, then check simple class names against the rest of the file body
- `LongMethodAnalyzer` — regex detects method signatures, then tracks `{` / `}` depth to count body lines
- `MissingJavadocAnalyzer` — matches `public`/`protected` declarations, walks backwards to find a closing `*/`

---

## Getting Started

### Prerequisites

- Java 17 or higher

Check your version:
```bash
java -version
```

If Java is not installed, download it from [https://adoptium.net](https://adoptium.net) and run the installer.

### Download

Download `codescan-cli.jar` from the releases section and place it anywhere on your machine.

---

## Usage

```
java -jar codescan-cli.jar [options] <path>
```

### Arguments

| Argument | Description |
|---|---|
| `<path>` | Path to a `.java` file or a directory to scan recursively |

### Options

| Flag | Description | Default |
|---|---|---|
| `--threshold <N>` | Max lines allowed in a method body before flagging | `30` |
| `--no-color` | Disable ANSI colors (useful for CI log files or Windows CMD) | off |
| `--skip-imports` | Skip unused import analysis | off |
| `--skip-methods` | Skip long method analysis | off |
| `--skip-javadoc` | Skip missing Javadoc analysis | off |
| `--help` | Print usage information | — |

### Examples

```bash
# Scan an entire source directory
java -jar codescan-cli.jar src/main/java/

# Scan a single file
java -jar codescan-cli.jar src/main/java/com/example/OrderService.java

# Stricter method length (flag anything over 20 lines)
java -jar codescan-cli.jar --threshold 20 src/

# Disable color for CI pipelines
java -jar codescan-cli.jar --no-color src/

# Only check for unused imports
java -jar codescan-cli.jar --skip-methods --skip-javadoc src/
```

---

## Example Output

Running against a file with several issues:

```
  Scanning: src/main/java/com/example/
  Analyzers active: 3  |  Method threshold: 30 lines

╔══════════════════════════════════════════════╗
║          CodeScan  Static Analyzer           ║
╚══════════════════════════════════════════════╝

  📄 src/main/java/com/example/OrderService.java
     [WARNING ] line 2     Import 'java.util.Map' is never used  (UNUSED_IMPORT)
     [WARNING ] line 3     Import 'java.util.HashMap' is never used  (UNUSED_IMPORT)
     [INFO    ] line 7     Class/type 'OrderService' is missing a Javadoc comment  (MISSING_JAVADOC)
     [WARNING ] line 31    Method 'processMonthlyStatement' has 45 lines (threshold: 30)  (LONG_METHOD)
     [INFO    ] line 31    Method 'processMonthlyStatement' is missing a Javadoc comment  (MISSING_JAVADOC)

──────────────────────────────────────────────
  Files scanned : 3
  Total issues  : 5
      WARNING  : 3
      INFO     : 2
──────────────────────────────────────────────

  Result: WARN ⚠  (review issues above)
```

### Exit Codes

| Code | Meaning |
|---|---|
| `0` | No issues found — clean pass |
| `1` | Issues found — warnings or info |
| `2` | Usage error or I/O failure |

Exit code `1` on issues found means CI pipelines can fail a build automatically:
```bash
java -jar codescan-cli.jar src/ || echo "Quality gate failed"
```

---

## Analysis Rules

### UNUSED_IMPORT — Severity: WARNING

Flags any `import` statement whose simple class name does not appear anywhere in the file body. Also flags wildcard imports (`import java.util.*`) as a best-practice violation regardless of usage, since they obscure which types are actually referenced.

**Examples flagged:**
```java
import java.util.Map;      // Map never used in the file
import java.util.*;        // Wildcard — always flagged
import static java.util.Collections.emptyList;  // emptyList never called
```

### LONG_METHOD — Severity: WARNING

Counts the lines inside a method's `{...}` body (excluding the signature line and closing brace). Reports an issue when the count exceeds the configured threshold (default: 30).

Long methods are a well-known code smell — they're hard to test, hard to read, and usually violate the Single Responsibility Principle. The threshold is configurable so teams can set their own standard.

**Customise:**
```bash
java -jar codescan-cli.jar --threshold 15 src/
```

### MISSING_JAVADOC — Severity: INFO

Checks that every `public` or `protected` class, interface, enum, and method has a Javadoc comment immediately before it (ignoring blank lines and annotations in between). Methods annotated with `@Override` are skipped since their Javadoc is inherited from the parent type. Private methods are not checked.

**What it looks for:**
```java
// ❌ Flagged — no Javadoc
public List<String> getOrders() { ... }

// ✔ Not flagged — has Javadoc
/**
 * Returns all active orders for the given account.
 */
public List<String> getOrders() { ... }

// ✔ Not flagged — @Override inherits Javadoc
@Override
public String toString() { ... }
```

---

## Running Tests

### With Maven (recommended)

```bash
mvn test
```

Expected output:
```
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Test Coverage Summary

| Test Class | Tests | What's Covered |
|---|---|---|
| `UnusedImportAnalyzerTest` | 8 | Used imports, unused imports, wildcards, static imports, multiple violations |
| `LongMethodAnalyzerTest` | 10 | Threshold boundaries, multiple methods, invalid config, message content |
| `MissingJavadocAnalyzerTest` | 9 | Class/method coverage, `@Override` skip, private skip, severity |
| `ScanResultTest` | 6 | `isClean()`, counts, unmodifiability, null safety |
| `CodeScannerIntegrationTest` | 5 | Real temp files, directory walk, clean file, end-to-end detection |

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Core language — records, pattern matching, text blocks |
| File I/O (`java.nio.file`) | Directory walking, file reading |
| Regex (`java.util.regex`) | Import and method signature detection |
| OOP / SOLID principles | Analyzer interface, separation of concerns |
| JUnit 5 | Unit and integration testing |
| Maven | Build, dependency management, test runner |

---

## Resume Context

This project was built to demonstrate familiarity with **AutoRABIT's CodeScan** product, which performs static analysis on Salesforce Apex and Java codebases as part of a DevSecOps pipeline. CodeScan surfaces the same categories of issues — unused code, complexity violations, documentation gaps — and integrates with CI/CD pipelines using the same exit-code convention implemented here.

The architecture (pluggable rule interface, immutable issue model, decoupled reporter) reflects the design patterns used in production static analysis tools like SonarQube, PMD, and Checkstyle.
