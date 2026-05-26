package com.codescan;

import com.codescan.analyzer.MissingJavadocAnalyzer;
import com.codescan.model.Issue;
import com.codescan.model.IssueSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MissingJavadocAnalyzer}.
 */
@DisplayName("MissingJavadocAnalyzer")
class MissingJavadocAnalyzerTest {

    private MissingJavadocAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new MissingJavadocAnalyzer();
    }

    @Test
    @DisplayName("No issues when class and methods have Javadoc")
    void noIssues_whenFullyDocumented() {
        List<String> lines = List.of(
                "/**",
                " * A documented class.",
                " */",
                "public class Foo {",
                "",
                "    /**",
                "     * Does something.",
                "     */",
                "    public void bar() { }",
                "}"
        );
        assertTrue(analyzer.analyze(lines, "Foo.java").isEmpty());
    }

    @Test
    @DisplayName("Flags a public method missing Javadoc")
    void flagsPublicMethod_withoutJavadoc() {
        List<String> lines = List.of(
                "/**",
                " * Documented class.",
                " */",
                "public class Foo {",
                "    public void bar() { }",
                "}"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertTrue(issues.stream().anyMatch(i -> i.getMessage().contains("bar")));
    }

    @Test
    @DisplayName("Flags a public class missing Javadoc")
    void flagsClass_withoutJavadoc() {
        List<String> lines = List.of(
                "public class Foo {",
                "}"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertTrue(issues.stream().anyMatch(i -> i.getMessage().contains("Foo")));
    }

    @Test
    @DisplayName("Does not flag @Override methods — Javadoc is inherited")
    void doesNotFlag_overriddenMethods() {
        List<String> lines = List.of(
                "/**",
                " * Documented.",
                " */",
                "public class Foo {",
                "    @Override",
                "    public String toString() { return \"\"; }",
                "}"
        );
        // toString should NOT be flagged because it has @Override
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertTrue(issues.stream().noneMatch(i -> i.getMessage().contains("toString")));
    }

    @Test
    @DisplayName("Issue severity is INFO")
    void issueHasInfoSeverity() {
        List<String> lines = List.of(
                "public class Foo {",
                "    public void bar() { }",
                "}"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertFalse(issues.isEmpty());
        issues.forEach(i -> assertEquals(IssueSeverity.INFO, i.getSeverity()));
    }

    @Test
    @DisplayName("Issue rule ID is MISSING_JAVADOC")
    void issueRuleId() {
        List<String> lines = List.of("public class Foo { }");
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertFalse(issues.isEmpty());
        assertEquals("MISSING_JAVADOC", issues.get(0).getRuleId());
    }

    @Test
    @DisplayName("Protected methods are also checked")
    void flagsProtectedMethod() {
        List<String> lines = List.of(
                "/**",
                " * Doc.",
                " */",
                "public class Foo {",
                "    protected void helper() { }",
                "}"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertTrue(issues.stream().anyMatch(i -> i.getMessage().contains("helper")));
    }

    @Test
    @DisplayName("Private methods are not flagged")
    void doesNotFlag_privateMethods() {
        List<String> lines = List.of(
                "/**",
                " * Doc.",
                " */",
                "public class Foo {",
                "    private void secret() { }",
                "}"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertTrue(issues.stream().noneMatch(i -> i.getMessage().contains("secret")));
    }

    @Test
    @DisplayName("Analyzer name is correct")
    void analyzerName() {
        assertEquals("MissingJavadocAnalyzer", analyzer.getName());
    }
}
