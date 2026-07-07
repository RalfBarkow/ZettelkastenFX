package zk.core.importing;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Zkn3ImportBatchUnresolvedReferencesTest {
    @Test
    void exposesUnresolvedReferencesAsCoreImportBatchComponent() {
        var component = Arrays.stream(Zkn3ImportBatch.class.getRecordComponents())
                .filter(c -> c.getName().equals("unresolvedReferences"))
                .findFirst();

        assertTrue(component.isPresent(), "Zkn3ImportBatch should expose unresolvedReferences");
        assertEquals(List.class, component.orElseThrow().getType());
    }
}
