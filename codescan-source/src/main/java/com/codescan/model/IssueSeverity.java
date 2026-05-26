package com.codescan.model;

/**
 * Represents the severity level of a detected code quality issue.
 * Mirrors the severity classification used in AutoRABIT CodeScan.
 */
public enum IssueSeverity {

    /** Severe violation — likely a bug or security concern */
    CRITICAL("CRITICAL", "\u001B[31m"),   // Red

    /** Code smell that could mask real problems */
    WARNING("WARNING ", "\u001B[33m"),    // Yellow

    /** Style / documentation gap */
    INFO("INFO    ", "\u001B[36m");       // Cyan

    private final String label;
    private final String ansiColor;
    static final String RESET = "\u001B[0m";

    IssueSeverity(String label, String ansiColor) {
        this.label = label;
        this.ansiColor = ansiColor;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Returns a color-coded label for terminal output.
     *
     * @param useColor whether to apply ANSI color codes
     * @return formatted severity label
     */
    public String getColoredLabel(boolean useColor) {
        return useColor ? ansiColor + label + RESET : label;
    }
}
