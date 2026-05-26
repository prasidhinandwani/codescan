package com.codescan.analyzer;

import com.codescan.model.Issue;
import com.codescan.model.IssueSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects import statements whose simple class name (or wildcard package)
 * is never referenced in the rest of the source file.
 *
 * <p><b>Algorithm:</b>
 * <ol>
 *   <li>Collect every {@code import} line and extract the simple name.</li>
 *   <li>Build a single string from all non-import lines.</li>
 *   <li>Flag any import whose simple name does not appear in that body.</li>
 * </ol>
 *
 * <p>Wildcard imports (e.g. {@code import java.util.*}) are always flagged
 * as best-practice violations — they hide which types are actually used.</p>
 *
 * <p><b>Rule ID:</b> {@code UNUSED_IMPORT}</p>
 * <p><b>Severity:</b> {@link IssueSeverity#WARNING}</p>
 */
public class UnusedImportAnalyzer implements Analyzer {

    private static final Pattern IMPORT_PATTERN =
            Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+(?:\\.\\*)?);");

    @Override
    public String getName() {
        return "UnusedImportAnalyzer";
    }

    @Override
    public List<Issue> analyze(List<String> lines, String filePath) {
        List<Issue> issues = new ArrayList<>();

        // --- Pass 1: collect all import lines with their metadata ---
        record ImportEntry(int lineNumber, String fullName, String simpleName) {}
        List<ImportEntry> imports = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher m = IMPORT_PATTERN.matcher(line);
            if (m.find()) {
                String fullName = m.group(1);
                if (fullName.endsWith(".*")) {
                    // Wildcard import — flag immediately
                    issues.add(new Issue(
                            filePath,
                            i + 1,
                            "UNUSED_IMPORT",
                            "Wildcard import '" + fullName + "' should be replaced with explicit imports",
                            IssueSeverity.WARNING
                    ));
                } else {
                    String simpleName = extractSimpleName(fullName);
                    imports.add(new ImportEntry(i + 1, fullName, simpleName));
                }
            }
        }

        if (imports.isEmpty()) {
            return issues;
        }

        // --- Pass 2: build the non-import body as one blob ---
        StringBuilder body = new StringBuilder();
        for (String line : lines) {
            if (!IMPORT_PATTERN.matcher(line).find()) {
                body.append(line).append('\n');
            }
        }
        String bodyText = body.toString();

        // --- Pass 3: check each import against the body ---
        for (ImportEntry entry : imports) {
            // Simple heuristic: search for the simple class name as a whole word
            Pattern usagePattern = Pattern.compile("\\b" + Pattern.quote(entry.simpleName()) + "\\b");
            if (!usagePattern.matcher(bodyText).find()) {
                issues.add(new Issue(
                        filePath,
                        entry.lineNumber(),
                        "UNUSED_IMPORT",
                        "Import '" + entry.fullName() + "' is never used",
                        IssueSeverity.WARNING
                ));
            }
        }

        return issues;
    }

    private String extractSimpleName(String fullName) {
        int dot = fullName.lastIndexOf('.');
        return dot >= 0 ? fullName.substring(dot + 1) : fullName;
    }
}
