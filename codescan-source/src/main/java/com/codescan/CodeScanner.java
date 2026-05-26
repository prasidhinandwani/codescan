package com.codescan;

import com.codescan.analyzer.Analyzer;
import com.codescan.model.Issue;
import com.codescan.model.ScanResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Orchestrates static analysis across a directory (or single file) of
 * {@code .java} source files.
 *
 * <p>The scanner is configured with a list of {@link Analyzer} implementations
 * at construction time. It discovers all {@code .java} files under the given
 * root, reads them line-by-line, and feeds each file to every registered
 * analyzer, accumulating results into a {@link ScanResult}.</p>
 *
 * <p>This class is intentionally decoupled from I/O presentation — all
 * display logic lives in reporter classes.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * List<Analyzer> analyzers = List.of(
 *     new UnusedImportAnalyzer(),
 *     new LongMethodAnalyzer(),
 *     new MissingJavadocAnalyzer()
 * );
 * CodeScanner scanner = new CodeScanner(analyzers);
 * ScanResult result = scanner.scan(Path.of("src/main/java"));
 * }</pre>
 */
public class CodeScanner {

    private final List<Analyzer> analyzers;

    /**
     * Creates a scanner with the given set of analyzers.
     *
     * @param analyzers analyzers to apply to each file; must not be null or empty
     */
    public CodeScanner(List<Analyzer> analyzers) {
        if (analyzers == null || analyzers.isEmpty()) {
            throw new IllegalArgumentException("At least one analyzer is required");
        }
        this.analyzers = new ArrayList<>(analyzers);
    }

    /**
     * Scans all {@code .java} files reachable from {@code root}.
     *
     * <p>If {@code root} is a regular file it is scanned directly.
     * If it is a directory the tree is walked recursively.</p>
     *
     * @param root path to a {@code .java} file or a directory
     * @return aggregated scan result
     * @throws IOException if the root path cannot be read
     */
    public ScanResult scan(Path root) throws IOException {
        ScanResult result = new ScanResult();

        if (Files.isRegularFile(root)) {
            scanFile(root, result);
        } else if (Files.isDirectory(root)) {
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            scanFile(p, result);
                        } catch (IOException e) {
                            System.err.println("  [WARN] Could not read " + p + ": " + e.getMessage());
                        }
                    });
            }
        } else {
            throw new IOException("Path does not exist or is not readable: " + root);
        }

        return result;
    }

    /**
     * Reads a single file and runs every analyzer against it.
     *
     * @param filePath path to the {@code .java} file
     * @param result   scan result to populate
     * @throws IOException if the file cannot be read
     */
    private void scanFile(Path filePath, ScanResult result) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        String pathStr = filePath.toString();

        result.incrementFilesScanned();

        for (Analyzer analyzer : analyzers) {
            List<Issue> issues = analyzer.analyze(lines, pathStr);
            result.addAll(issues);
        }
    }

    /** @return an unmodifiable view of the registered analyzers */
    public List<Analyzer> getAnalyzers() {
        return java.util.Collections.unmodifiableList(analyzers);
    }
}
