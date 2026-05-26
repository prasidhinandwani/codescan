package com.codescan.reporter;

import com.codescan.model.Issue;
import com.codescan.model.IssueSeverity;
import com.codescan.model.ScanResult;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;

/**
 * Renders a {@link ScanResult} to a {@link PrintStream} (typically {@code System.out}).
 *
 * <p>Output is grouped by file and sorted by line number. A summary block
 * shows total issue counts by severity, matching the style of
 * AutoRABIT CodeScan's CLI report format.</p>
 *
 * <p>ANSI color codes are enabled by default and can be disabled for
 * environments that do not support them (e.g., CI log files, Windows CMD).</p>
 */
public class ConsoleReporter {

    // ANSI escape codes
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String DIM    = "\u001B[2m";

    private final PrintStream out;
    private final boolean useColor;

    /**
     * Creates a reporter that writes to {@code System.out} with color enabled.
     */
    public ConsoleReporter() {
        this(System.out, true);
    }

    /**
     * Creates a reporter with full control over output target and color.
     *
     * @param out      target print stream
     * @param useColor whether to emit ANSI color codes
     */
    public ConsoleReporter(PrintStream out, boolean useColor) {
        this.out = out;
        this.useColor = useColor;
    }

    /**
     * Prints the full scan report including per-file issues and a summary.
     *
     * @param result the aggregated result of a code scan
     */
    public void report(ScanResult result) {
        printHeader();

        if (result.isClean()) {
            out.println(color(GREEN) + "  ✔  No issues found. Your code looks great!" + color(RESET));
        } else {
            printIssues(result.getIssues());
        }

        printSummary(result);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void printHeader() {
        out.println();
        out.println(color(BOLD) + "╔══════════════════════════════════════════════╗" + color(RESET));
        out.println(color(BOLD) + "║          CodeScan  Static Analyzer           ║" + color(RESET));
        out.println(color(BOLD) + "╚══════════════════════════════════════════════╝" + color(RESET));
        out.println();
    }

    private void printIssues(List<Issue> issues) {
        // Group by file path, preserving insertion order per file
        String currentFile = null;

        List<Issue> sorted = issues.stream()
                .sorted(Comparator.comparing(Issue::getFilePath)
                        .thenComparingInt(Issue::getLineNumber))
                .toList();

        for (Issue issue : sorted) {
            if (!issue.getFilePath().equals(currentFile)) {
                currentFile = issue.getFilePath();
                out.println(color(BOLD) + "  📄 " + currentFile + color(RESET));
            }

            String severityLabel = issue.getSeverity().getColoredLabel(useColor);
            String line = issue.getLineNumber() > 0
                    ? color(DIM) + "line " + String.format("%-4d", issue.getLineNumber()) + color(RESET)
                    : color(DIM) + "     " + color(RESET) + "    ";

            out.printf("     [%s] %s  %s  %s(%s)%s%n",
                    severityLabel,
                    line,
                    issue.getMessage(),
                    color(DIM),
                    issue.getRuleId(),
                    color(RESET));
        }

        out.println();
    }

    private void printSummary(ScanResult result) {
        long critical = result.countBySeverity(IssueSeverity.CRITICAL);
        long warnings = result.countBySeverity(IssueSeverity.WARNING);
        long infos    = result.countBySeverity(IssueSeverity.INFO);

        out.println(color(BOLD) + "──────────────────────────────────────────────" + color(RESET));
        out.printf("  Files scanned : %s%d%s%n",
                color(BOLD), result.getFilesScanned(), color(RESET));
        out.printf("  Total issues  : %s%d%s%n",
                result.isClean() ? color(GREEN) : color(BOLD),
                result.getTotalIssues(),
                color(RESET));

        if (!result.isClean()) {
            if (critical > 0) out.printf("    %s  %-8s : %d%s%n", color(RED),    "CRITICAL", critical, color(RESET));
            if (warnings > 0) out.printf("    %s  %-8s : %d%s%n", color(YELLOW), "WARNING",  warnings, color(RESET));
            if (infos    > 0) out.printf("    %s  %-8s : %d%s%n", color(CYAN),   "INFO",     infos,    color(RESET));
        }

        out.println(color(BOLD) + "──────────────────────────────────────────────" + color(RESET));
        out.println();

        // Exit message
        if (result.isClean()) {
            out.println(color(GREEN) + "  Result: PASS ✔" + color(RESET));
        } else if (critical > 0) {
            out.println(color(RED) + "  Result: FAIL ✘  (critical issues found)" + color(RESET));
        } else {
            out.println(color(YELLOW) + "  Result: WARN ⚠  (review issues above)" + color(RESET));
        }
        out.println();
    }

    private String color(String code) {
        return useColor ? code : "";
    }
}
