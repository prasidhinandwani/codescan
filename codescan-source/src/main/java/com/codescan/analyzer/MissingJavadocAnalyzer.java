package com.codescan.analyzer;

import com.codescan.model.Issue;
import com.codescan.model.IssueSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects public methods and classes that are missing a Javadoc comment.
 *
 * <p><b>Algorithm:</b>
 * <ol>
 *   <li>Scan lines looking for {@code public} class or method declarations.</li>
 *   <li>Walk backwards from the declaration to find if the immediately
 *       preceding non-blank line is the closing {@code * /} of a Javadoc block.</li>
 *   <li>Report an {@link IssueSeverity#INFO} issue when no Javadoc is found.</li>
 * </ol>
 *
 * <p>Constructors, interface methods, and enum declarations are also covered
 * by the same method-signature regex.</p>
 *
 * <p><b>Rule ID:</b> {@code MISSING_JAVADOC}</p>
 * <p><b>Severity:</b> {@link IssueSeverity#INFO}</p>
 */
public class MissingJavadocAnalyzer implements Analyzer {

    /** Matches a public class, interface, enum, or record declaration. */
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "^\\s*(?:public\\s+)?(?:(?:abstract|final|sealed)\\s+)?"
            + "(?:class|interface|enum|record)\\s+(\\w+)"
    );

    /**
     * Matches a public/protected method or constructor signature.
     * Excludes {@code @Override} annotated methods by checking backwards
     * in {@link #hasJavadoc}.
     */
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "^\\s*(?:public|protected)\\s+"
            + "(?:(?:static|final|synchronized|abstract|default)\\s+)*"
            + "(?:<[^>]+>\\s+)?"
            + "[\\w<>\\[\\]]+\\s+(\\w+)\\s*\\("
    );

    @Override
    public String getName() {
        return "MissingJavadocAnalyzer";
    }

    @Override
    public List<Issue> analyze(List<String> lines, String filePath) {
        List<Issue> issues = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            java.util.regex.Matcher classMatcher = CLASS_PATTERN.matcher(line);
            java.util.regex.Matcher methodMatcher = METHOD_PATTERN.matcher(line);

            if (classMatcher.find()) {
                String name = classMatcher.group(1);
                if (!hasJavadoc(lines, i)) {
                    issues.add(new Issue(
                            filePath,
                            i + 1,
                            "MISSING_JAVADOC",
                            "Class/type '" + name + "' is missing a Javadoc comment",
                            IssueSeverity.INFO
                    ));
                }
            } else if (methodMatcher.find()) {
                String name = methodMatcher.group(1);
                // Skip getters/setters shorter than 4 chars (optional strictness)
                if (isOverrideAnnotated(lines, i)) continue;

                if (!hasJavadoc(lines, i)) {
                    issues.add(new Issue(
                            filePath,
                            i + 1,
                            "MISSING_JAVADOC",
                            "Method '" + name + "' is missing a Javadoc comment",
                            IssueSeverity.INFO
                    ));
                }
            }
        }

        return issues;
    }

    /**
     * Walks backward from {@code declarationIndex} to check whether
     * the nearest non-blank, non-annotation line ends with {@code * /}.
     *
     * @param lines            all source lines
     * @param declarationIndex 0-based index of the declaration line
     * @return {@code true} if a Javadoc comment is present
     */
    private boolean hasJavadoc(List<String> lines, int declarationIndex) {
        int i = declarationIndex - 1;

        // Skip annotations and blank lines
        while (i >= 0) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("@")) {
                i--;
                continue;
            }
            // The line immediately before (ignoring annotations/blanks) must close a Javadoc
            return trimmed.equals("*/");
        }

        return false;
    }

    /**
     * Checks whether the line immediately before the declaration (ignoring blanks)
     * is an {@code @Override} annotation, in which case Javadoc is inherited.
     */
    private boolean isOverrideAnnotated(List<String> lines, int declarationIndex) {
        for (int i = declarationIndex - 1; i >= 0; i--) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty()) continue;
            return trimmed.equals("@Override");
        }
        return false;
    }
}
