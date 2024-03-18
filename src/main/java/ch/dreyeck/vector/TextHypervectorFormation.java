package ch.dreyeck.vector;

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
        // Generate a random vector for each trigram in the text
        int trigramCount = text.length() - 2;
        int[] textHypervector = new int[trigramCount * 10000];
        for (int i = 0; i < trigramCount; i++) {
            int[] randomVector = generateRandomVector();
            for (int j = 0; j < 10000; j++) {
                textHypervector[i * 10000 + j] = randomVector[j];
            }
        }
        return textHypervector;
    }

    // Example of how to use the TextHypervectorFormation class
    public static void main(String[] args) {
        // Example text
        String exampleText = "This is an example text.";

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
