package com.codescan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates all {@link Issue}s discovered during a scan of a single file or
 * an entire directory. Provides summary statistics used by reporters.
 */
public final class ScanResult {

    private final List<Issue> issues = new ArrayList<>();
    private int filesScanned = 0;

    /**
     * Records that one more file was scanned (regardless of whether it has issues).
     */
    public void incrementFilesScanned() {
        filesScanned++;
    }

    /**
     * Adds an issue to this result set.
     *
     * @param issue the issue to add (must not be null)
     */
    public void addIssue(Issue issue) {
        if (issue != null) {
            issues.add(issue);
        }
    }

    /**
     * Adds all issues from a collection to this result.
     *
     * @param newIssues issues to merge in
     */
    public void addAll(List<Issue> newIssues) {
        if (newIssues != null) {
            issues.addAll(newIssues);
        }
    }

    /** @return an unmodifiable view of all issues */
    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    /** @return total number of issues found */
    public int getTotalIssues() {
        return issues.size();
    }

    /** @return number of files scanned */
    public int getFilesScanned() {
        return filesScanned;
    }

    /** @return count of issues at the given severity */
    public long countBySeverity(IssueSeverity severity) {
        return issues.stream()
                     .filter(i -> i.getSeverity() == severity)
                     .count();
    }

    /** @return {@code true} if no issues were found */
    public boolean isClean() {
        return issues.isEmpty();
    }
}
