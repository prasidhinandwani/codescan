package com.codescan.analyzer;

import com.codescan.model.Issue;
import com.codescan.model.IssueSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Flags methods whose body exceeds a configurable line-length threshold.
 *
 * <p><b>Algorithm:</b>
 * <ol>
 *   <li>Detect method signature lines using a regex that matches common
 *       access modifiers, return types, and opening braces.</li>
 *   <li>Count lines inside each method body by tracking curly-brace depth.</li>
 *   <li>Emit a {@link IssueSeverity#WARNING} when the line count exceeds
 *       {@link #getThreshold()}.</li>
 * </ol>
 *
 * <p>The default threshold is {@value #DEFAULT_THRESHOLD} lines.
 * A custom threshold can be supplied via the constructor for stricter
 * or more relaxed enforcement.</p>
 *
 * <p><b>Rule ID:</b> {@code LONG_METHOD}</p>
 * <p><b>Severity:</b> {@link IssueSeverity#WARNING}</p>
 */
public class LongMethodAnalyzer implements Analyzer {

    /** Default max method body length before an issue is reported. */
    public static final int DEFAULT_THRESHOLD = 30;

    /**
     * Matches a method or constructor declaration line.
     * Handles: access modifiers, static, final, generic return types,
     * method names, and parameter lists (including multi-line if the
     * opening brace appears on the same line).
     */
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "^\\s*(?:(?:public|protected|private|static|final|synchronized|abstract|native)\\s+)*"
            + "(?:<[^>]+>\\s+)?"          // optional generic type
            + "(?!if|for|while|switch|try|catch|finally|else|do\\b)"
            + "[\\w<>\\[\\]]+\\s+"         // return type
            + "\\w+\\s*\\([^)]*\\)?"       // method name + params (may be partial)
            + "(?:\\s*throws\\s+[\\w,\\s]+)?"
            + "\\s*\\{\\s*$"              // must end with opening brace
    );

    private final int threshold;

    /** Constructs an analyzer using the default threshold. */
    public LongMethodAnalyzer() {
        this(DEFAULT_THRESHOLD);
    }

    /**
     * Constructs an analyzer with a custom threshold.
     *
     * @param threshold maximum number of lines allowed in a method body
     * @throws IllegalArgumentException if threshold is less than 1
     */
    public LongMethodAnalyzer(int threshold) {
        if (threshold < 1) throw new IllegalArgumentException("Threshold must be >= 1");
        this.threshold = threshold;
    }

    /** @return the currently configured line threshold */
    public int getThreshold() {
        return threshold;
    }

    @Override
    public String getName() {
        return "LongMethodAnalyzer";
    }

    @Override
    public List<Issue> analyze(List<String> lines, String filePath) {
        List<Issue> issues = new ArrayList<>();

        int depth = 0;                 // brace nesting depth
        int methodStartLine = -1;      // 1-based line where the current method begins
        int methodBodyLines = 0;       // lines counted inside the current method body
        String methodName = null;      // extracted method name for messages
        boolean inMethod = false;      // are we inside a method body?

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (!inMethod && METHOD_PATTERN.matcher(line).find()) {
                // Start tracking a new method
                inMethod = true;
                methodStartLine = i + 1;
                methodBodyLines = 0;
                methodName = extractMethodName(line);
                depth = 1; // the opening brace on this line
                continue;
            }

            if (inMethod) {
                // Process braces before counting so the closing-brace line
                // is not counted as a method body line.
                for (char c : line.toCharArray()) {
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                }

                if (depth <= 0) {
                    // Method body has closed
                    if (methodBodyLines > threshold) {
                        issues.add(new Issue(
                                filePath,
                                methodStartLine,
                                "LONG_METHOD",
                                "Method '" + methodName + "' has " + methodBodyLines
                                + " lines (threshold: " + threshold + ")",
                                IssueSeverity.WARNING
                        ));
                    }
                    inMethod = false;
                    depth = 0;
                    methodName = null;
                } else {
                    methodBodyLines++;
                }
            } else {
                // Track class/block depth so nested classes don't confuse us
                for (char c : line.toCharArray()) {
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                }
            }
        }

        return issues;
    }

    private String extractMethodName(String line) {
        // Grab the word immediately before the opening parenthesis
        java.util.regex.Matcher m =
                Pattern.compile("(\\w+)\\s*\\(").matcher(line);
        String name = "unknown";
        while (m.find()) name = m.group(1); // last match = method name
        return name;
    }
}
