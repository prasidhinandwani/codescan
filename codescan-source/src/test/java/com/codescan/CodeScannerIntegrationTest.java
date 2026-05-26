package com.codescan;

import com.codescan.analyzer.LongMethodAnalyzer;
import com.codescan.analyzer.MissingJavadocAnalyzer;
import com.codescan.analyzer.UnusedImportAnalyzer;
import com.codescan.model.ScanResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link CodeScanner}.
 * Writes real {@code .java} files to a temp directory and scans them.
 */
@DisplayName("CodeScanner Integration")
class CodeScannerIntegrationTest {

    @TempDir
    Path tempDir;

    private CodeScanner fullScanner() {
        return new CodeScanner(List.of(
                new UnusedImportAnalyzer(),
                new LongMethodAnalyzer(10),
                new MissingJavadocAnalyzer()
        ));
    }

    @Test
    @DisplayName("Scans a clean file and returns zero issues")
    void cleanFile_returnsNoIssues() throws IOException {
        String source = """
                /**
                 * A perfectly documented class.
                 */
                public class Clean {

                    /**
                     * Does something useful.
                     */
                    public void doIt() {
                        int x = 1;
                    }
                }
                """;
        Path file = tempDir.resolve("Clean.java");
        Files.writeString(file, source);

        ScanResult result = fullScanner().scan(file);
        assertEquals(1, result.getFilesScanned());
        assertTrue(result.isClean(), "Expected no issues but got: " + result.getIssues());
    }

    @Test
    @DisplayName("Detects unused import in a single file")
    void singleFile_unusedImport() throws IOException {
        String source = """
                import java.util.List;
                import java.util.Map;
                
                /**
                 * Uses List but not Map.
                 */
                public class UsesOnlyList {
                    /**
                     * Returns a list.
                     */
                    public List<String> get() { return null; }
                }
                """;
        Path file = tempDir.resolve("UsesOnlyList.java");
        Files.writeString(file, source);

        ScanResult result = fullScanner().scan(file);
        assertTrue(result.getIssues().stream()
                .anyMatch(i -> i.getRuleId().equals("UNUSED_IMPORT")
                               && i.getMessage().contains("Map")));
    }

    @Test
    @DisplayName("Scans multiple files in a directory")
    void directory_scansAllJavaFiles() throws IOException {
        Files.writeString(tempDir.resolve("A.java"), "public class A { }");
        Files.writeString(tempDir.resolve("B.java"), "public class B { }");
        Files.writeString(tempDir.resolve("C.java"), "public class C { }");
        Files.writeString(tempDir.resolve("notes.txt"), "not a java file");

        ScanResult result = fullScanner().scan(tempDir);
        assertEquals(3, result.getFilesScanned(), "Should scan exactly 3 .java files");
    }

    @Test
    @DisplayName("Throws IOException for non-existent path")
    void throwsForNonExistentPath() {
        Path ghost = tempDir.resolve("does-not-exist");
        assertThrows(IOException.class, () -> fullScanner().scan(ghost));
    }

    @Test
    @DisplayName("CodeScanner rejects empty analyzer list")
    void rejectsEmptyAnalyzerList() {
        assertThrows(IllegalArgumentException.class,
                () -> new CodeScanner(List.of()));
    }

    @Test
    @DisplayName("Long method issue is detected end-to-end")
    void longMethod_isDetectedEndToEnd() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("/** Class doc. */\npublic class BigClass {\n");
        sb.append("    /** Method doc. */\n");
        sb.append("    public void fatMethod() {\n");
        for (int i = 0; i < 20; i++) sb.append("        int x").append(i).append(" = 0;\n");
        sb.append("    }\n}\n");

        Path file = tempDir.resolve("BigClass.java");
        Files.writeString(file, sb.toString());

        ScanResult result = new CodeScanner(List.of(new LongMethodAnalyzer(10))).scan(file);
        assertTrue(result.getIssues().stream()
                .anyMatch(i -> i.getRuleId().equals("LONG_METHOD")));
    }
}
