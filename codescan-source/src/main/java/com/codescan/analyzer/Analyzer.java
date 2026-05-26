package com.codescan.analyzer;

import com.codescan.model.Issue;

import java.util.List;

/**
 * Contract for all static analysis rules in CodeScan.
 *
 * <p>Each implementation focuses on a single concern (unused imports,
 * long methods, missing Javadoc, etc.) and receives the raw lines of a
 * Java source file together with the file's path for issue attribution.</p>
 *
 * <p>Implementations must be stateless — a single instance must be
 * safely reusable across multiple files.</p>
 */
public interface Analyzer {

    /**
     * Analyzes the supplied source lines and returns all violations found.
     *
     * @param lines    raw lines of a {@code .java} file (1-indexed in messages)
     * @param filePath the file path used when constructing {@link Issue}s
     * @return list of issues; empty if none found; never {@code null}
     */
    List<Issue> analyze(List<String> lines, String filePath);

    /**
     * Returns a unique, human-readable name for this analyzer.
     * Used in summary output and rule IDs.
     *
     * @return analyzer name, e.g. {@code "UnusedImportAnalyzer"}
     */
    String getName();
}
