package com.codescan;

import com.codescan.model.Issue;
import com.codescan.model.IssueSeverity;
import com.codescan.model.ScanResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ScanResult}.
 */
@DisplayName("ScanResult")
class ScanResultTest {

    private Issue issue(IssueSeverity severity) {
        return new Issue("Test.java", 1, "RULE", "msg", severity);
    }

    @Test
    @DisplayName("isClean() returns true when no issues added")
    void isClean_whenEmpty() {
        ScanResult result = new ScanResult();
        assertTrue(result.isClean());
    }

    @Test
    @DisplayName("isClean() returns false after adding an issue")
    void isNotClean_afterAddingIssue() {
        ScanResult result = new ScanResult();
        result.addIssue(issue(IssueSeverity.INFO));
        assertFalse(result.isClean());
    }

    @Test
    @DisplayName("getTotalIssues() counts all severities")
    void totalIssueCount() {
        ScanResult result = new ScanResult();
        result.addIssue(issue(IssueSeverity.CRITICAL));
        result.addIssue(issue(IssueSeverity.WARNING));
        result.addIssue(issue(IssueSeverity.INFO));
        assertEquals(3, result.getTotalIssues());
    }

    @Test
    @DisplayName("countBySeverity() correctly filters")
    void countBySeverity() {
        ScanResult result = new ScanResult();
        result.addIssue(issue(IssueSeverity.WARNING));
        result.addIssue(issue(IssueSeverity.WARNING));
        result.addIssue(issue(IssueSeverity.INFO));
        assertEquals(2, result.countBySeverity(IssueSeverity.WARNING));
        assertEquals(1, result.countBySeverity(IssueSeverity.INFO));
        assertEquals(0, result.countBySeverity(IssueSeverity.CRITICAL));
    }

    @Test
    @DisplayName("incrementFilesScanned() is tracked independently")
    void filesScannedTracking() {
        ScanResult result = new ScanResult();
        result.incrementFilesScanned();
        result.incrementFilesScanned();
        result.incrementFilesScanned();
        assertEquals(3, result.getFilesScanned());
    }

    @Test
    @DisplayName("getIssues() returns unmodifiable list")
    void getIssues_isUnmodifiable() {
        ScanResult result = new ScanResult();
        result.addIssue(issue(IssueSeverity.INFO));
        assertThrows(UnsupportedOperationException.class,
                () -> result.getIssues().add(issue(IssueSeverity.INFO)));
    }

    @Test
    @DisplayName("addIssue(null) is silently ignored")
    void addNullIssue_isIgnored() {
        ScanResult result = new ScanResult();
        assertDoesNotThrow(() -> result.addIssue(null));
        assertEquals(0, result.getTotalIssues());
    }
}
