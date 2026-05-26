package com.codescan;

import com.codescan.analyzer.Analyzer;
import com.codescan.analyzer.LongMethodAnalyzer;
import com.codescan.analyzer.MissingJavadocAnalyzer;
import com.codescan.analyzer.UnusedImportAnalyzer;
import com.codescan.model.ScanResult;
import com.codescan.reporter.ConsoleReporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the CodeScan CLI tool.
 *
 * <h2>Usage</h2>
 * <pre>
 *   java -jar codescan-cli.jar [options] &lt;path&gt;
 *
 *   Options:
 *     --no-color          Disable ANSI color output (useful for CI logs)
 *     --threshold &lt;N&gt;     Max lines per method before flagging (default: 30)
 *     --skip-imports      Skip unused import analysis
 *     --skip-methods      Skip long method analysis
 *     --skip-javadoc      Skip missing Javadoc analysis
 *     --help              Show this help text
 * </pre>
 *
 * <p>Exit codes mirror common CI conventions:</p>
 * <ul>
 *   <li>{@code 0} — no issues found (PASS)</li>
 *   <li>{@code 1} — issues found (WARN or FAIL)</li>
 *   <li>{@code 2} — usage error or I/O failure</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args) {
        // ── Parse arguments ─────────────────────────────────────────────────
        String targetPath   = null;
        boolean useColor    = true;
        int threshold       = LongMethodAnalyzer.DEFAULT_THRESHOLD;
        boolean skipImports = false;
        boolean skipMethods = false;
        boolean skipJavadoc = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--no-color"     -> useColor    = false;
                case "--skip-imports" -> skipImports = true;
                case "--skip-methods" -> skipMethods = true;
                case "--skip-javadoc" -> skipJavadoc = true;
                case "--help"         -> { printHelp(); System.exit(0); }
                case "--threshold"    -> {
                    if (i + 1 >= args.length) { die("--threshold requires a numeric argument"); }
                    try { threshold = Integer.parseInt(args[++i]); }
                    catch (NumberFormatException e) { die("Invalid threshold: " + args[i]); }
                }
                default -> {
                    if (args[i].startsWith("--")) { die("Unknown option: " + args[i]); }
                    if (targetPath != null)        { die("Only one target path is supported"); }
                    targetPath = args[i];
                }
            }
        }

        if (targetPath == null) {
            die("No target path specified. Run with --help for usage.");
        }

        // ── Build analyzer pipeline ──────────────────────────────────────────
        List<Analyzer> analyzers = new ArrayList<>();
        if (!skipImports) analyzers.add(new UnusedImportAnalyzer());
        if (!skipMethods) analyzers.add(new LongMethodAnalyzer(threshold));
        if (!skipJavadoc) analyzers.add(new MissingJavadocAnalyzer());

        if (analyzers.isEmpty()) {
            die("All analyzers are disabled — nothing to scan.");
        }

        // ── Run scan ─────────────────────────────────────────────────────────
        CodeScanner scanner = new CodeScanner(analyzers);
        ConsoleReporter reporter = new ConsoleReporter(System.out, useColor);

        System.out.printf("%n  Scanning: %s%n", targetPath);
        System.out.printf("  Analyzers active: %d  |  Method threshold: %d lines%n%n",
                analyzers.size(), threshold);

        ScanResult result;
        try {
            result = scanner.scan(Path.of(targetPath));
        } catch (IOException e) {
            System.err.println("[ERROR] " + e.getMessage());
            System.exit(2);
            return; // unreachable but satisfies compiler
        }

        reporter.report(result);

        // ── Exit code ────────────────────────────────────────────────────────
        System.exit(result.isClean() ? 0 : 1);
    }

    private static void printHelp() {
        System.out.println("""
                Usage: java -jar codescan-cli.jar [options] <path>
                
                  <path>               Directory or .java file to scan
                
                Options:
                  --no-color           Disable ANSI color output
                  --threshold <N>      Max lines per method (default: 30)
                  --skip-imports       Skip unused import analysis
                  --skip-methods       Skip long method analysis
                  --skip-javadoc       Skip missing Javadoc analysis
                  --help               Show this help message
                
                Exit codes:
                  0   No issues found
                  1   Issues found
                  2   Usage error or I/O failure
                """);
    }

    private static void die(String message) {
        System.err.println("[ERROR] " + message);
        System.err.println("Run with --help for usage.");
        System.exit(2);
    }
}
