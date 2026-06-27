public class TextUtils {
    public static String toSentenceCase(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input; // Return as is if input is null or empty
        }

        String[] words = input.trim().toLowerCase().split("\\s+");
        StringBuilder sentenceCase = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                sentenceCase.append(Character.toUpperCase(word.charAt(0))) // Capitalize first letter
                           .append(word.substring(1)) // Append the rest of the word
                           .append(" "); // Add a space
            }
        }

        return sentenceCase.toString().trim(); // Remove trailing space
    }
}
