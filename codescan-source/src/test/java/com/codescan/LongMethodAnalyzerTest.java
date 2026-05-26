package com.codescan;

import com.codescan.analyzer.LongMethodAnalyzer;
import com.codescan.model.Issue;
import com.codescan.model.IssueSeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LongMethodAnalyzer}.
 */
@DisplayName("LongMethodAnalyzer")
class LongMethodAnalyzerTest {

    /** Builds a method source with exactly {@code bodyLines} lines inside it. */
    private List<String> methodWithLines(int bodyLines) {
        List<String> src = new ArrayList<>();
        src.add("public class Foo {");
        src.add("    public void myMethod() {");
        for (int i = 0; i < bodyLines; i++) {
            src.add("        int x" + i + " = " + i + ";");
        }
        src.add("    }");
        src.add("}");
        return src;
    }

    @Test
    @DisplayName("No issue when method is within threshold")
    void noIssue_whenMethodIsShort() {
        LongMethodAnalyzer analyzer = new LongMethodAnalyzer(30);
        List<Issue> issues = analyzer.analyze(methodWithLines(10), "Foo.java");
        assertTrue(issues.isEmpty());
    }

    @Test
    @DisplayName("Flags method that exceeds threshold by one line")
    void flagsMethod_justOverThreshold() {
        LongMethodAnalyzer analyzer = new LongMethodAnalyzer(5);
        List<Issue> issues = analyzer.analyze(methodWithLines(6), "Foo.java");
        assertEquals(1, issues.size());
        assertEquals("LONG_METHOD", issues.get(0).getRuleId());
    }

    @Test
    @DisplayName("Exactly at threshold — no issue")
    void noIssue_atExactThreshold() {
        LongMethodAnalyzer analyzer = new LongMethodAnalyzer(5);
        List<Issue> issues = analyzer.analyze(methodWithLines(5), "Foo.java");
        assertTrue(issues.isEmpty(), "Method at threshold should not be flagged");
    }

    @Test
    @DisplayName("Issue message includes line count and threshold")
    void issueMessage_containsCountAndThreshold() {
        LongMethodAnalyzer analyzer = new LongMethodAnalyzer(3);
        List<Issue> issues = analyzer.analyze(methodWithLines(8), "Foo.java");
        assertEquals(1, issues.size());
        String msg = issues.get(0).getMessage();
        assertTrue(msg.contains("8"), "Should mention actual line count");
        assertTrue(msg.contains("3"), "Should mention threshold");
    }

    @Test
    @DisplayName("Multiple long methods in one file are all flagged")
    void flagsMultipleLongMethods() {
        List<String> src = new ArrayList<>();
        src.add("public class Foo {");
        // Method 1 — 40 lines (exceeds default 30)
        src.add("    public void alpha() {");
        for (int i = 0; i < 40; i++) src.add("        int a" + i + " = 0;");
        src.add("    }");
        // Method 2 — 35 lines
        src.add("    public void beta() {");
        for (int i = 0; i < 35; i++) src.add("        int b" + i + " = 0;");
        src.add("    }");
        src.add("}");

        LongMethodAnalyzer analyzer = new LongMethodAnalyzer(30);
        List<Issue> issues = analyzer.analyze(src, "Foo.java");
        assertEquals(2, issues.size());
    }

    @Test
    @DisplayName("Short method is not flagged even with low threshold")
    void noIssue_shortMethodWithLowThreshold() {
        LongMethodAnalyzer analyzer = new LongMethodAnalyzer(1);
        List<Issue> issues = analyzer.analyze(methodWithLines(1), "Foo.java");
        assertTrue(issues.isEmpty());
    }

    @Test
    @DisplayName("Severity is WARNING")
    void issueHasWarningSeverity() {
        LongMethodAnalyzer analyzer = new LongMethodAnalyzer(1);
        List<Issue> issues = analyzer.analyze(methodWithLines(5), "Foo.java");
        assertFalse(issues.isEmpty());
        assertEquals(IssueSeverity.WARNING, issues.get(0).getSeverity());
    }

    @Test
    @DisplayName("Throws on invalid threshold")
    void throwsOnInvalidThreshold() {
        assertThrows(IllegalArgumentException.class, () -> new LongMethodAnalyzer(0));
        assertThrows(IllegalArgumentException.class, () -> new LongMethodAnalyzer(-10));
    }

    @Test
    @DisplayName("Default threshold is 30")
    void defaultThresholdIs30() {
        assertEquals(30, new LongMethodAnalyzer().getThreshold());
    }

    @Test
    @DisplayName("Analyzer name is correct")
    void analyzerName() {
        assertEquals("LongMethodAnalyzer", new LongMethodAnalyzer().getName());
    }
}
