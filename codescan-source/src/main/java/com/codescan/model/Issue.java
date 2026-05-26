package com.codescan.model;

import java.util.Objects;

/**
 * Represents a single code quality issue found during analysis.
 *
 * <p>Each issue captures the file, line number, rule that triggered it,
 * a human-readable message, and a severity level — matching the data model
 * used in AutoRABIT CodeScan's issue reporting.</p>
 */
public final class Issue {

    private final String filePath;
    private final int lineNumber;
    private final String ruleId;
    private final String message;
    private final IssueSeverity severity;

    /**
     * Constructs a new Issue.
     *
     * @param filePath   relative or absolute path to the offending file
     * @param lineNumber 1-based line number of the issue (0 = file-level)
     * @param ruleId     short identifier for the rule, e.g. "UNUSED_IMPORT"
     * @param message    descriptive message explaining the issue
     * @param severity   severity classification of the issue
     */
    public Issue(String filePath, int lineNumber, String ruleId,
                 String message, IssueSeverity severity) {
        this.filePath   = Objects.requireNonNull(filePath,  "filePath must not be null");
        this.lineNumber = lineNumber;
        this.ruleId     = Objects.requireNonNull(ruleId,   "ruleId must not be null");
        this.message    = Objects.requireNonNull(message,  "message must not be null");
        this.severity   = Objects.requireNonNull(severity, "severity must not be null");
    }

    /** @return path to the file containing the issue */
    public String getFilePath()   { return filePath;   }

    /** @return 1-based line number, or 0 for file-level issues */
    public int getLineNumber()    { return lineNumber; }

    /** @return short rule identifier */
    public String getRuleId()     { return ruleId;     }

    /** @return human-readable description of the issue */
    public String getMessage()    { return message;    }

    /** @return severity of the issue */
    public IssueSeverity getSeverity() { return severity; }

    @Override
    public String toString() {
        String loc = lineNumber > 0 ? ":" + lineNumber : "";
        return String.format("[%s] %s%s — %s (%s)",
                severity.getLabel(), filePath, loc, message, ruleId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Issue issue)) return false;
        return lineNumber == issue.lineNumber
                && filePath.equals(issue.filePath)
                && ruleId.equals(issue.ruleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, lineNumber, ruleId);
    }
}
