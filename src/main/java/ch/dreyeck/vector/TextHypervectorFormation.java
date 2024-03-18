package ch.dreyeck.vector;

import java.util.List;
import java.util.Random;

public class TextHypervectorFormation {

    // Method to generate a random vector
    public static int[] generateRandomVector() {
        // Generate a vector of length 10,000 with random values (0 or 1)
        int[] randomVector = new int[10000];
        for (int i = 0; i < 10000; i++) {
            randomVector[i] = Math.random() < 0.5 ? 0 : 1;
        }
        return randomVector;
    }

    // Method to form the text hypervector
    public static int[] formTextHypervector(String text) {
        // Extract trigrams from the text using the TrigramExtractor
        List<String> trigrams = TrigramExtractor.extractTrigrams(text);

        // Initialize the hypervector with enough space for all trigrams
        int[] textHypervector = new int[trigrams.size() * 10000];

        // Iterate over each trigram
        for (int i = 0; i < trigrams.size(); i++) {
            // Generate a random vector for the trigram
            int[] randomVector = generateRandomVector();

            // Copy the random vector to the corresponding position in the hypervector
            for (int j = 0; j < 10000; j++) {
                textHypervector[i * 10000 + j] = randomVector[j];
            }
        }

        return textHypervector;
    }

    // Method to generate a random vector for Latin alphabets and space
    public static int[] generateRandomVectorForLatinAlphabets() {
        int[] latinAlphabetsVector = new int[26 * 10000 + 10000]; // 26 alphabets + 1 for space
        Random random = new Random();
        for (int i = 0; i < latinAlphabetsVector.length; i++) {
            latinAlphabetsVector[i] = random.nextInt(2); // Generates 0 or 1
        }
        return latinAlphabetsVector;
    }

    // Example of how to use the TextHypervectorFormation class
    public static void main(String[] args) {
        // Example text containing all 26 Latin alphabets and space
        String exampleText = "ABCDEFGHIJKLMNOPQRSTUVWXYZ ";

        // Form the text hypervector
        int[] hypervector = formTextHypervector(exampleText);

        // Display the length of the hypervector
        System.out.println("Length of the hypervector: " + hypervector.length);

        // Display the first 10 elements of the hypervector
        System.out.print("First 10 elements: ");
        for (int i = 0; i < 10; i++) {
            System.out.print(hypervector[i] + " ");
        }
    }

}
