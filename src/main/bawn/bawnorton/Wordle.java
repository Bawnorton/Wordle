package main.bawn.bawnorton;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class Wordle {
    final static int wordLength = 5;
    final static boolean auto = true;
    final static String path = "/Users/benjamin/Documents/Developer/Java/Wordle/src/main/resources/wordle.txt";
    public static void main(String[] args) {
        Hashtable<Integer, List<String>> words = new Hashtable<>();
        System.out.println("Loading Words...");
        try {
            words = loadWords();
        } catch(FileNotFoundException e) {
            System.out.printf("Could not find file: %s", path);
        }
        List<String> selectWords = words.get(wordLength);
        System.out.printf("Processing Length %s... (%s words)\n", wordLength, selectWords.size());
        System.out.println("Generating Probability Table...");
        Hashtable<Character, Hashtable<Integer, Float>> probabilities = genProbabilityTable(selectWords);
        System.out.println("Guessing...");
        if (auto) {
            generateAutomatic(selectWords, probabilities);
        } else {
            input(selectWords, probabilities);
        }
    }
    private static void input(List<String> selectWords, Hashtable<Character, Hashtable<Integer, Float>> probabilities) {
        String guessingBase = "*".repeat(wordLength);
        Scanner scanner = new Scanner(System.in);
        Hashtable<Character, List<Integer>> knownChars = new Hashtable<>();
        List<Character> usedChars = new ArrayList<>();
        String toGuess = guessingBase;
        while (true) {
            List<String> possibleWords = genPossibleWords(toGuess, knownChars, usedChars, selectWords);
            String bestWord = getBestWord(possibleWords, probabilities);
            System.out.printf("Best Word: %s\n", bestWord);
            System.out.print("Result Word: ");
            toGuess = scanner.nextLine();
            if(toGuess.equals("skip")) {
                possibleWords.remove(bestWord);
            } else if (toGuess.equals(bestWord)) {
                break;
            }
            toGuess = addKnownChars(toGuess, bestWord, knownChars, usedChars);
        }
    }
    private static String addKnownChars(String toGuess, String bestWord, Hashtable<Character, List<Integer>> knownChars, List<Character> usedChars) {
        int i = 0;
        Character[] blankWord = new Character[toGuess.length()];
        for(char c: toGuess.toCharArray()) {
            blankWord[i] = '*';
            if (c == '*') {
                if(!usedChars.contains(bestWord.charAt(i))) usedChars.add(bestWord.charAt(i));
            }
            else if(Character.isLowerCase(c)) {
                char letter = Character.toUpperCase(c);
                List<Integer> positions = knownChars.getOrDefault(letter, new ArrayList<>());
                if(!positions.contains(i)) positions.add(i);
                knownChars.put(letter, positions);
            } else if(Character.isUpperCase(c)) {
                blankWord[i] = c;
            }
            i++;
        }
        return Arrays.stream(blankWord).map(String::valueOf).collect(Collectors.joining());
    }

    private static void generateAutomatic(List<String> selectWords, Hashtable<Character, Hashtable<Integer, Float>> probabilities) {
        long start = System.currentTimeMillis();
        Hashtable<Integer, Integer> guessDistribution = new Hashtable<>();
        String guessingBase = "*".repeat(wordLength);
        int count = 10000;
        for (int i = 0; i < count; i++) {
            Hashtable<Character, List<Integer>> knownChars = new Hashtable<>();
            List<Character> usedChars = new ArrayList<>();
            System.out.print("\b\b\b\b\b\b\b");
            System.out.print(100 * (float) i / count);
            String randWord = selectWords.get(new Random(System.currentTimeMillis()).nextInt(selectWords.size()));
            String toGuess = guessingBase;
            int j = 0;
            while (!toGuess.equals(randWord)) {
                List<String> possibleWords = genPossibleWords(toGuess, knownChars, usedChars, selectWords);
                String bestWord = getBestWord(possibleWords, probabilities);
                toGuess = guess(bestWord, randWord, usedChars, knownChars);
                j++;
            }
            guessDistribution.put(j, guessDistribution.getOrDefault(j, 0) + 1);
        }
        long end = System.currentTimeMillis();
        System.out.printf("\nComplete (%ss)\n", (end - start) / 1000D);
        for(Map.Entry<Integer, Integer> entry: guessDistribution.entrySet()) {
            System.out.println("    Guess Count: " + entry.getKey() + ", Number of Times: " + entry.getValue());
        }
    }

    private static String guess(String guessWord, String unknownWord, List<Character> usedChars, Hashtable<Character, List<Integer>> knownChars) {
        Character[] blankWord = new Character[guessWord.length()];
        int i = 0;
        for(char chr: guessWord.toCharArray()) {
            if (guessWord.charAt(i) == unknownWord.charAt(i)) blankWord[i] = chr;
            else if (unknownWord.indexOf(chr) == -1 && !usedChars.contains(chr)) usedChars.add(chr);
            else if (!usedChars.contains(chr)){
                List<Integer> locations = knownChars.getOrDefault(chr, new ArrayList<>());
                locations.add(i);
                knownChars.put(chr, locations);
            }
            i++;
        }
        i = 0;
        for(Character chr: blankWord) {
            if (chr == null) {
                blankWord[i] = '*';
            }
            i++;
        }
        return Arrays.stream(blankWord).map(String::valueOf).collect(Collectors.joining());
    }
    private static String getBestWord(List<String> possibleWords, Hashtable<Character, Hashtable<Integer, Float>> probabilities) {
        Hashtable<String, Float> wordProbabilities = new Hashtable<>();
        for(String word : possibleWords) {
            float total = 0;
            int i = 0;
            for(char chr: word.toCharArray()) {
                total += probabilities.get(chr).get(i);
                i++;
            }
            wordProbabilities.put(word, total);
        }

        Hashtable<String, Hashtable<Character, Integer>> charCounts = new Hashtable<>();
        for(Map.Entry<String, Float> entry: wordProbabilities.entrySet()) {
            String word = entry.getKey();
            Hashtable<Character, Integer> charCount = new Hashtable<>();
            for(char chr: word.toCharArray()) {
                charCount.put(chr, charCount.getOrDefault(chr, 0) + 1);
            }
            charCounts.put(word, charCount);
        }
        int maxSize = 0;
        for(Hashtable<Character, Integer> charCount: charCounts.values()) {
            if(charCount.size() > maxSize) {
                maxSize = charCount.size();
            }
        }
        List<String> bestWords = new ArrayList<>();
        for(Map.Entry<String, Hashtable<Character, Integer>> charCount: charCounts.entrySet()) {
            if(charCount.getValue().size() == maxSize) {
                bestWords.add(charCount.getKey());
            }
        }
        int maxIndex = 0;
        float max = 0;
        for(String word: bestWords) {
            float probability = wordProbabilities.get(word);
            if (probability > max) {
                maxIndex = bestWords.indexOf(word);
                max = probability;
            }
        }
        return bestWords.get(maxIndex);
    }
    private static List<String> genPossibleWords(String toGuess, Hashtable<Character, List<Integer>> knownChars, List<Character> usedChars, List<String> words) {
        char[] wordArr = toGuess.toCharArray();
        List<String> possibleWords = new ArrayList<>();
        outer: for(String word: words) {
            for(int i = 0; i < wordArr.length; i++) {
                if (word.charAt(i) != wordArr[i] && wordArr[i] != '*') continue outer;
            }
            for (char c: word.toCharArray()) {
                if (usedChars.contains(c)) {
                    continue outer;
                }
            }
            for (char c: knownChars.keySet()) {
                if (word.indexOf(c) != -1) {
                    int i = 0;
                    for (char chr: word.toCharArray()) {
                        if (knownChars.containsKey(chr)) {
                            List<Integer> prePositions = knownChars.get(chr);
                            if (prePositions.contains(i)) {
                                continue outer;
                            }
                        }
                        i++;
                    }
                } else {
                    continue outer;
                }
            }
            possibleWords.add(word);
        }
        return possibleWords;
    }
    private static Hashtable<Character, Hashtable<Integer, Float>> genProbabilityTable(List<String> words) {
        Hashtable<Character, Hashtable<Integer, Integer>> charCountLocations = new Hashtable<>();
        for (String word: words) {
            int i = 0;
            for(Character character : word.toCharArray()) {
                Hashtable<Integer, Integer> locationCounts = charCountLocations.getOrDefault(character, new Hashtable<>());
                locationCounts.put(i, locationCounts.getOrDefault(i, 0) + 1);
                charCountLocations.put(character, locationCounts);
                i++;
            }
        }
        Hashtable<Character, Integer> total = new Hashtable<>();
        for(Map.Entry<Character, Hashtable<Integer, Integer>> count: charCountLocations.entrySet()) {
            int totalValue = 0;
            for(Hashtable<Integer, Integer> charCount: charCountLocations.values()) {
                for(Integer value: charCount.values()) {
                    totalValue += value;
                }
            }
            total.put(count.getKey(), totalValue);
        }
        Hashtable<Character, Hashtable<Integer, Float>> probabilities = new Hashtable<>();
        for(Character character : charCountLocations.keySet()) {
            Hashtable<Integer, Float> percentages = new Hashtable<>();
            for(Map.Entry<Integer, Integer> entry: charCountLocations.get(character).entrySet()) {
                Float percentage = Float.valueOf(entry.getValue()) / total.get(character);
                percentages.put(entry.getKey(), percentage);
            }
            probabilities.put(character, percentages);
        }
        return probabilities;
    }
    private static Hashtable<Integer, List<String>> loadWords() throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(path));
        Hashtable<Integer, List<String>> words = new Hashtable<>();
        Hashtable<String, String> memory = new Hashtable<>();
        while(scanner.hasNext()) {
            String word = scanner.nextLine().replaceAll("\\P{L}", "").toUpperCase();
            List<String> list = words.getOrDefault(word.length(), new ArrayList<>());
            if(!memory.containsKey(word)) {
                memory.put(word, "");
                list.add(word);
            }
            words.put(word.length(), list);
        }
        return words;
    }
}
