package com.codescan;

import com.codescan.analyzer.UnusedImportAnalyzer;
import com.codescan.model.Issue;
import com.codescan.model.IssueSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UnusedImportAnalyzer}.
 */
@DisplayName("UnusedImportAnalyzer")
class UnusedImportAnalyzerTest {

    private UnusedImportAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new UnusedImportAnalyzer();
    }

    @Test
    @DisplayName("No issues for a clean file with all imports used")
    void noIssues_whenAllImportsAreUsed() {
        List<String> lines = List.of(
                "import java.util.List;",
                "import java.util.ArrayList;",
                "",
                "public class Foo {",
                "    List<String> items = new ArrayList<>();",
                "}"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertTrue(issues.isEmpty(), "Expected no issues but got: " + issues);
    }

    @Test
    @DisplayName("Flags an import that is never referenced in the body")
    void flagsUnusedImport() {
        List<String> lines = List.of(
                "import java.util.List;",
                "import java.util.Map;",  // Map never used
                "",
                "public class Foo {",
                "    List<String> items;",
                "}"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertEquals(1, issues.size());
        assertEquals("UNUSED_IMPORT", issues.get(0).getRuleId());
        assertTrue(issues.get(0).getMessage().contains("Map"));
    }

    @Test
    @DisplayName("Flags wildcard imports as bad practice")
    void flagsWildcardImport() {
        List<String> lines = List.of(
                "import java.util.*;",
                "",
                "public class Foo { }"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertEquals(1, issues.size());
        assertTrue(issues.get(0).getMessage().contains("Wildcard"));
    }

    @Test
    @DisplayName("Returns no issues for a file with no imports")
    void noIssues_whenNoImports() {
        List<String> lines = List.of(
                "public class Foo {",
                "    public void bar() { }",
                "}"
        );
        assertTrue(analyzer.analyze(lines, "Foo.java").isEmpty());
    }

    @Test
    @DisplayName("Detects multiple unused imports in one file")
    void flagsMultipleUnusedImports() {
        List<String> lines = List.of(
                "import java.util.List;",
                "import java.util.Map;",
                "import java.io.File;",
                "",
                "public class Foo { }"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertEquals(3, issues.size());
    }

    @Test
    @DisplayName("Issue severity is WARNING")
    void issueHasWarningSeverity() {
        List<String> lines = List.of(
                "import java.util.List;",
                "public class Foo { }"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertFalse(issues.isEmpty());
        assertEquals(IssueSeverity.WARNING, issues.get(0).getSeverity());
    }

    @Test
    @DisplayName("Static imports are also checked")
    void detectsUnusedStaticImport() {
        List<String> lines = List.of(
                "import static java.util.Collections.emptyList;",
                "public class Foo { }"
        );
        List<Issue> issues = analyzer.analyze(lines, "Foo.java");
        assertEquals(1, issues.size());
        assertTrue(issues.get(0).getMessage().contains("emptyList"));
    }

    @Test
    @DisplayName("analyzer name is correct")
    void analyzerName() {
        assertEquals("UnusedImportAnalyzer", analyzer.getName());
    }
}
