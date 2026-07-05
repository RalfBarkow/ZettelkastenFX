package zk.source.zkn3;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Zkn3SourceModuleBoundaryTest {
    private static final Pattern FORBIDDEN_IMPORT = Pattern.compile(
            "^\\s*import\\s+(javax\\.swing|java\\.awt|javafx\\.|zk\\.storage\\.|zk\\.ui\\.).*");

    @Test
    void moduleDependsOnlyOnCoreAndTestLibraries() throws IOException {
        String pom = Files.readString(moduleRoot().resolve("pom.xml"), StandardCharsets.UTF_8);

        assertTrue(pom.contains("<artifactId>zk-core</artifactId>"));
        assertFalse(pom.contains("<artifactId>zk-storage-sqlite</artifactId>"));
        assertFalse(pom.contains("<artifactId>zk-ui-javafx</artifactId>"));
        assertFalse(pom.toLowerCase().contains("javafx"));
        assertFalse(pom.toLowerCase().contains("sqlite"));
        assertFalse(pom.toLowerCase().contains("jaxb"));
    }

    @Test
    void mainSourceMustNotImportUiStorageOrParserAdapters() throws IOException {
        Path sourceRoot = moduleRoot().resolve("src/main/java");
        assertTrue(Files.isDirectory(sourceRoot), "Expected source root at " + sourceRoot);

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> collectForbiddenImports(sourceRoot, path, violations));
        }

        assertTrue(violations.isEmpty(),
                "zk-source-zkn3 must stay independent of UI and storage packages:\n"
                        + String.join("\n", violations));
    }

    private static Path moduleRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        if (Files.exists(current.resolve("src/main/java/zk/source/zkn3/package-info.java"))) {
            return current;
        }

        Path childModule = current.resolve("zk-source-zkn3");
        if (Files.exists(childModule.resolve("src/main/java/zk/source/zkn3/package-info.java"))) {
            return childModule;
        }

        throw new IllegalStateException("Could not locate zk-source-zkn3 module from " + current);
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
