package zk.source.zkn3;

import org.junit.jupiter.api.Test;
import zk.core.importing.Zkn3DiagnosticSeverity;
import zk.core.importing.Zkn3ImportBatch;
import zk.core.importing.Zkn3ImportDiagnostic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

final class Zkn3TimestampDiagnosticReportTest {
    private static final String REAL_SOURCE_PROPERTY = "zkn3.real.source";
    private static final int SAMPLE_LIMIT = 10;
    private static final Set<String> TIMESTAMP_FIELDS = Set.of("ts_created", "ts_edited");
    private static final Pattern QUOTED_VALUE = Pattern.compile("'([^']*)'");
    private static final Pattern RAW_TIMESTAMP_VALUE = Pattern.compile("raw ts_(?:created|edited)='([^']*)'");

    @Test
    void reportsTimestampDiagnosticsForRealSourceOnlyWhenOptedIn() throws IOException {
        String propertyValue = System.getProperty(REAL_SOURCE_PROPERTY, "").trim();
        assumeFalse(propertyValue.isEmpty(), REAL_SOURCE_PROPERTY + " is not set; skipping timestamp report.");

        Path source = Path.of(propertyValue);
        assertTrue(Files.isRegularFile(source), "Expected readable real ZKN3 source at " + source);
        assertTrue(Files.isReadable(source), "Expected readable real ZKN3 source at " + source);

        Zkn3ImportBatch batch = new Zkn3DomSourceReader().read(source);
        assertNotNull(batch);
        assertNotNull(batch.diagnostics());

        List<DiagnosticGroup> groups = groupDiagnostics(batch.diagnostics());
        List<DiagnosticGroup> timestampGroups = groups.stream()
                .filter(group -> TIMESTAMP_FIELDS.contains(group.field()))
                .toList();
        List<DiagnosticGroup> nonTimestampErrorGroups = groups.stream()
                .filter(group -> Zkn3DiagnosticSeverity.ERROR == group.severity())
                .filter(group -> !TIMESTAMP_FIELDS.contains(group.field()))
                .toList();

        long errorCount = countDiagnostics(batch, Zkn3DiagnosticSeverity.ERROR);
        long warningCount = countDiagnostics(batch, Zkn3DiagnosticSeverity.WARNING);
        boolean rawTimestampValuesAvailable = timestampGroups.stream()
                .anyMatch(group -> !group.sampleRawValues().isEmpty());
        boolean sourceIdsAvailable = timestampGroups.stream()
                .anyMatch(group -> !group.sampleSourceIds().isEmpty());

        printReport(
                source,
                batch,
                errorCount,
                warningCount,
                timestampGroups,
                nonTimestampErrorGroups,
                rawTimestampValuesAvailable,
                sourceIdsAvailable
        );

        if (errorCount > 0) {
            assertNoPartialSuccessfulBatch(batch);
            assertFalse(timestampGroups.isEmpty(), "Expected timestamp diagnostics in rejected real-source batch.");
            assertFalse(nonTimestampErrorGroups.isEmpty(),
                    "Expected non-timestamp error groups in rejected real-source batch.");
        }
    }

    private static long countDiagnostics(Zkn3ImportBatch batch, Zkn3DiagnosticSeverity severity) {
        return batch.diagnostics().stream()
                .filter(diagnostic -> severity == diagnostic.severity())
                .count();
    }

    private static void assertNoPartialSuccessfulBatch(Zkn3ImportBatch batch) {
        assertEquals(0, batch.notes().size(), "Rejected real-source batch must not contain notes.");
        assertEquals(0, batch.keywords().size(), "Rejected real-source batch must not contain keywords.");
        assertEquals(0, batch.links().size(), "Rejected real-source batch must not contain links.");
        assertEquals(0, batch.sequences().size(), "Rejected real-source batch must not contain sequences.");
    }

    private static List<DiagnosticGroup> groupDiagnostics(List<Zkn3ImportDiagnostic> diagnostics) {
        Map<DiagnosticGroupKey, List<Zkn3ImportDiagnostic>> grouped = new LinkedHashMap<>();
        for (Zkn3ImportDiagnostic diagnostic : diagnostics) {
            DiagnosticGroupKey key = new DiagnosticGroupKey(
                    diagnostic.severity(),
                    normalizedField(diagnostic.field()),
                    normalizeMessage(diagnostic.message())
            );
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(diagnostic);
        }

        return grouped.entrySet().stream()
                .map(entry -> new DiagnosticGroup(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DiagnosticGroup::severity)
                        .thenComparing(DiagnosticGroup::field)
                        .thenComparing(DiagnosticGroup::pattern))
                .toList();
    }

    private static String normalizedField(String field) {
        if (field == null || field.isBlank()) {
            return "<unknown>";
        }
        return field;
    }

    private static String normalizeMessage(String message) {
        String normalized = message == null ? "" : message;
        normalized = QUOTED_VALUE.matcher(normalized).replaceAll("<value>");
        normalized = normalized.replaceAll("([A-Za-z]:)?[/\\\\][^\\s;,.]+", "<path>");
        normalized = normalized.replaceAll("\\b\\d+\\b", "<number>");
        return normalized;
    }

    private static List<String> sampleSourceIds(List<Zkn3ImportDiagnostic> diagnostics) {
        return diagnostics.stream()
                .map(Zkn3ImportDiagnostic::sourceId)
                .filter(sourceId -> sourceId != null && !sourceId.isBlank())
                .distinct()
                .limit(SAMPLE_LIMIT)
                .toList();
    }

    private static List<String> sampleRawValues(List<Zkn3ImportDiagnostic> diagnostics) {
        List<String> values = new ArrayList<>();
        for (Zkn3ImportDiagnostic diagnostic : diagnostics) {
            Matcher matcher = RAW_TIMESTAMP_VALUE.matcher(diagnostic.message() == null ? "" : diagnostic.message());
            while (matcher.find() && values.size() < SAMPLE_LIMIT) {
                String value = displayRawValue(matcher.group(1));
                if (!values.contains(value)) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private static String displayRawValue(String rawValue) {
        if (rawValue.isEmpty()) {
            return "<empty>";
        }
        return rawValue;
    }

    private static void printReport(
            Path source,
            Zkn3ImportBatch batch,
            long errorCount,
            long warningCount,
            List<DiagnosticGroup> timestampGroups,
            List<DiagnosticGroup> nonTimestampErrorGroups,
            boolean rawTimestampValuesAvailable,
            boolean sourceIdsAvailable
    ) {
        StringBuilder report = new StringBuilder();
        report.append("ZKN3 timestamp diagnostic report\n");
        report.append("source=").append(source).append('\n');
        report.append("batchStatus=").append(errorCount == 0 ? "IMPORTABLE" : "CORRECTLY_REJECTED").append('\n');
        report.append("records:\n");
        report.append("  notes=").append(batch.notes().size()).append('\n');
        report.append("  keywords=").append(batch.keywords().size()).append('\n');
        report.append("  links=").append(batch.links().size()).append('\n');
        report.append("  sequences=").append(batch.sequences().size()).append('\n');
        report.append("diagnostics:\n");
        report.append("  total=").append(batch.diagnostics().size()).append('\n');
        report.append("  errors=").append(errorCount).append('\n');
        report.append("  warnings=").append(warningCount).append('\n');
        report.append('\n');
        report.append("timestamp groups:\n");
        appendTimestampGroups(report, timestampGroups);
        report.append('\n');
        report.append("non-timestamp error groups:\n");
        appendNonTimestampErrorGroups(report, nonTimestampErrorGroups);
        report.append('\n');
        report.append("diagnostic sufficiency:\n");
        report.append("  raw timestamp values available: ").append(rawTimestampValuesAvailable).append('\n');
        report.append("  source ids available: ").append(sourceIdsAvailable).append('\n');
        report.append("  sufficient for parser/core-vocabulary decision: ")
                .append(rawTimestampValuesAvailable && sourceIdsAvailable)
                .append('\n');
        report.append("  next recommended slice: ")
                .append(rawTimestampValuesAvailable
                        ? "implement-zkn3-blank-ts-edited-compatibility"
                        : "improve-zkn3-timestamp-diagnostics")
                .append('\n');

        System.out.println(report);
    }

    private static void appendTimestampGroups(StringBuilder report, List<DiagnosticGroup> timestampGroups) {
        if (timestampGroups.isEmpty()) {
            report.append("  none\n");
            return;
        }

        for (DiagnosticGroup group : timestampGroups) {
            report.append("  field=").append(group.field()).append('\n');
            report.append("  severity=").append(group.severity()).append('\n');
            report.append("  count=").append(group.count()).append('\n');
            report.append("  pattern=").append(group.pattern()).append('\n');
            report.append("  sampleSourceIds=").append(group.sampleSourceIds()).append('\n');
            report.append("  sampleRawValues=");
            if (group.sampleRawValues().isEmpty()) {
                report.append("NOT AVAILABLE IN CURRENT DIAGNOSTICS\n");
                report.append("  raw offending timestamp values: NOT AVAILABLE IN CURRENT DIAGNOSTICS\n");
            } else {
                report.append(group.sampleRawValues()).append('\n');
            }
            report.append('\n');
        }
    }

    private static void appendNonTimestampErrorGroups(
            StringBuilder report,
            List<DiagnosticGroup> nonTimestampErrorGroups
    ) {
        if (nonTimestampErrorGroups.isEmpty()) {
            report.append("  none\n");
            return;
        }

        for (DiagnosticGroup group : nonTimestampErrorGroups) {
            report.append("  field=").append(group.field()).append('\n');
            report.append("  severity=").append(group.severity()).append('\n');
            report.append("  count=").append(group.count()).append('\n');
            report.append("  pattern=").append(group.pattern()).append('\n');
            report.append('\n');
        }
    }

    private record DiagnosticGroupKey(
            Zkn3DiagnosticSeverity severity,
            String field,
            String pattern
    ) {
    }

    private record DiagnosticGroup(
            DiagnosticGroupKey key,
            List<Zkn3ImportDiagnostic> diagnostics
    ) {
        private Zkn3DiagnosticSeverity severity() {
            return key.severity();
        }

        private String field() {
            return key.field();
        }

        private String pattern() {
            return key.pattern();
        }

        private int count() {
            return diagnostics.size();
        }

        private List<String> sampleSourceIds() {
            return Zkn3TimestampDiagnosticReportTest.sampleSourceIds(diagnostics);
        }

        private List<String> sampleRawValues() {
            return Zkn3TimestampDiagnosticReportTest.sampleRawValues(diagnostics);
        }
    }
}
