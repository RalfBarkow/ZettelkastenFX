package ch.dreyeck.vector;

import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TextHypervectorFormationTest {

    @Test
    public void testFormTextHypervector() {
        // Test with a simple sentence
        String testSentence = "Welcome";
        int[] textHypervector = TextHypervectorFormation.formTextHypervector(testSentence);

        // Assert that textHypervector is not null
        assertNotNull(textHypervector);

        // Assert that the length of textHypervector is correct
        int expectedLength = (testSentence.length() - 2) * 10000;
        assertEquals(expectedLength, textHypervector.length);

        // Additional assertions can be added as needed
    }
}
