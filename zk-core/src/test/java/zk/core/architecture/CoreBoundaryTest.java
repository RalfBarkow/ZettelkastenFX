package zk.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CoreBoundaryTest {
    private static final Pattern FORBIDDEN_IMPORT = Pattern.compile(
            "^\\s*import\\s+(javax\\.swing|java\\.awt|javafx\\.|zk\\.storage\\.|zk\\.ui\\.).*");

    @Test
    void coreMustNotImportUiOrStoragePackages() throws IOException {
        Path sourceRoot = Paths.get(System.getProperty("user.dir")).resolve("src/main/java");
        assertTrue(Files.isDirectory(sourceRoot), "Expected zk-core source root at " + sourceRoot);

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> collectForbiddenImports(sourceRoot, path, violations));
        }

        assertTrue(violations.isEmpty(),
                "zk-core must stay independent of Swing, AWT, JavaFX, storage, and UI packages:\n"
                        + String.join("\n", violations));
    }

    private static void collectForbiddenImports(Path sourceRoot, Path file, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                if (FORBIDDEN_IMPORT.matcher(lines.get(i)).matches()) {
                    violations.add(sourceRoot.relativize(file) + ":" + (i + 1) + ": " + lines.get(i).trim());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not inspect " + file, e);
        }
    }
}
