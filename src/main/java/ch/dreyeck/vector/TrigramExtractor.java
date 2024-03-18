package ch.dreyeck.vector;

import java.util.ArrayList;
import java.util.List;

public class TrigramExtractor {

    // Method to extract trigrams from a given text
    public static List<String> extractTrigrams(String text) {
        List<String> trigrams = new ArrayList<>();

        // Iterate through the text to extract trigrams
        for (int i = 0; i < text.length() - 2; i++) {
            // Extract three consecutive characters
            String trigram = text.substring(i, i + 3);
            // Add the trigram to the list
            trigrams.add(trigram);
        }

        return trigrams;
    }

    // Example of how to use the TrigramExtractor class
    public static void main(String[] args) {
        // Example text
        String exampleText = "This is an example text.";

        // Extract trigrams from the example text
        List<String> trigrams = extractTrigrams(exampleText);

        // Display the extracted trigrams
        System.out.println("Extracted Trigrams:");
        for (String trigram : trigrams) {
            System.out.println(trigram);
        }
    }
}
