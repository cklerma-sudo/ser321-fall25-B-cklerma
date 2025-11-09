
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Hangman Game Server - Student Starter Code
 *
 * Your task: Design the protocol and implement the game logic.
 *
 * What's provided:
 * - Resource loading (hangman stages, word lists)
 * - Name handling as a complete example
 * - Basic server structure and routing
 *
 * What you need to implement:
 * - Complete protocol design (document in README.md)
 * - All game logic handlers (stubs provided below)
 */
public class HangmanServer {
    static Socket sock;
    static ObjectOutputStream os;
    static ObjectInputStream in;
    static int port = 8888;

    // Game state for current player - YOU WILL NEED THESE
    static String playerName = null;
    static String currentWord = null;
    static String difficulty = null;
    static Set<Character> guessedLetters = new HashSet<>();
    static int wrongGuesses = 0;
    static int score = 0;
    static boolean gameActive = false;

    // Leaderboard - list of game results
    static List<Map<String, Object>> leaderboard = new ArrayList<>();

    // Hangman ASCII art - 10 stages (0-9 wrong guesses)
    // Loaded from resources/hangman_stages.txt
    static String[] HANGMAN_STAGES = new String[10];

    // Word lists by difficulty - loaded from resource files
    static String[] EASY_WORDS;
    static String[] MEDIUM_WORDS;
    static String[] HARD_WORDS;

    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Expected arguments: <port(int)>");
            System.exit(1);
        }

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("Port must be an integer");
            System.exit(2);
        }

        // Load game resources
        loadHangmanStages();
        loadWords();

        try {
            ServerSocket serv = new ServerSocket(port);
            System.out.println("Hangman Server ready for connections on port " + port);

            while (true) {
                System.out.println("Server waiting for a connection");
                sock = serv.accept();
                System.out.println("Client connected");

                // Setup streams
                in = new ObjectInputStream(sock.getInputStream());
                OutputStream out = sock.getOutputStream();
                os = new ObjectOutputStream(out);

                // Reset game state for new connection
                resetGame();

                boolean connected = true;
                while (connected) {
                    String s = "";
                    try {
                        s = (String) in.readObject();
                    } catch (Exception e) {
                        System.out.println("Client disconnect");
                        connected = false;
                        continue;
                    }

                    JSONObject res = isValid(s);
                    if (res.has("ok")) {
                        writeOut(res);
                        continue;
                    }

                    JSONObject req = new JSONObject(s);
                    res = testField(req, "type");
                    if (!res.getBoolean("ok")) {
                        res = noType(req);
                        writeOut(res);
                        continue;
                    }

                    // Route to appropriate handler
                    String type = req.getString("type");
                    if (type.equals("name")) {
                        res = handleName(req);
                      /// include the other types
                    } else if (type.equals("quit")) {
                        res = handleQuit(req);
                        writeOut(res);
                        connected = false;
                        continue;
                    }
                    else if (type.equals("start")) {
                        res = startGame(req);
                    }
                    else if (type.equals("guessLetter")) {
                        res = guessLetter(req);
                    }
                    else if (type.equals("guessWord")) {
                        res = guessWord(req);
                    }
                    else if (type.equals("state")) {
                        res = state(req);
                    }
                    else if (type.equals("letters")) {
                        res = lettersGuessed(req);
                    }
                    else if (type.equals("leaderboard")) {
                        res = showLeaderboard(req);
                    }
                    else if (type.equals("return")) {
                        res = returnToMenu(req);
                    }
                    else {
                        res = wrongType(req);
                    }
                    writeOut(res);
                }
                overandout();
            }
        } catch (Exception e) {
            e.printStackTrace();
            overandout();
        }
    }

    static JSONObject returnToMenu(JSONObject req) {
        JSONObject res = new JSONObject();
        String temp = playerName;
        resetGame();
        playerName = temp;
        res.put("ok", true);
        res.put("type", "return");
        res.put("message", "Success, going back to main menu");
        return res;
    }

    static JSONObject showLeaderboard(JSONObject req) {
        JSONObject res = new JSONObject();
        JSONArray leaderboardRes = new JSONArray();
        if (leaderboard.isEmpty()) {
            res.put("ok", true);
            res.put("count", 0);
            res.put("type", "leaderboard");
        }
        else {
            int rank = 1;
            for (Map<String, Object> obj : leaderboard) {
                JSONObject row = new JSONObject();
                row.put("rank", rank++);
                row.put("name", obj.get("name"));
                row.put("score", obj.get("score"));
                row.put("difficulty", obj.get("difficulty"));
                leaderboardRes.put(row);
            }

            res.put("leaderboard", leaderboardRes);
            res.put("ok", true);
            res.put("count", leaderboard.size());
            res.put("type", "leaderboard");
        }
        return res;
    }

    static JSONObject lettersGuessed(JSONObject req) {
        JSONObject res = new JSONObject();
        res.put("ok", true);
        res.put("type", "letters");
        JSONArray letters = new JSONArray();
        for (Character c : guessedLetters) letters.put(c.toString().toUpperCase());
        res.put("lettersGuessed", letters);
        res.put("total", guessedLetters.size());
        return res;
    }

    static JSONObject state(JSONObject req) {
        JSONObject res = new JSONObject();
        res.put("ok", true);
        res.put("type", "state");
        res.put("hangStage", HANGMAN_STAGES[wrongGuesses]);
        res.put("score", score);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentWord.length(); i++) {
            char c = currentWord.charAt(i);
            if (guessedLetters.contains(c)) {
                String spaceAdded =" " + c;
                sb.append(spaceAdded);
            }
            else {
                sb.append(" _");
            }
        }
        String word = sb.toString();
        res.put("word", word);
        String numGuess = Integer.toString(wrongGuesses) + "/9";
        res.put("guesses", numGuess);
        res.put("lettersAmount", guessedLetters.size());
        return res;
    }

    static boolean checkLetter(String word) {
        boolean allLetters = true;
        for (int i = 0; i < word.length(); i++) {
            if (!Character.isLetter(word.charAt(i))) allLetters = false;
        }
        return allLetters;
    }

    static JSONObject guessWord(JSONObject req) {
        JSONObject res = new JSONObject();
        String guess = req.getString("letter");
        if (guess == null) {
            res.put("ok", false);
            res.put("message", "Guess cannot be empty");
            return res;
        }
        else if (!checkLetter(guess)) {
            res.put("ok", false);
            res.put("message", "Guess must be a word with only letters such as 'rotten'");
            return res;
        }
        else if (guess.length() != currentWord.length()) {
            res.put("ok", false);
            res.put("message", "Guess was not the correct amount of letters");
            return res;
        }
        guess = guess.toLowerCase();
        if (guess.equals(currentWord)) {
            res.put("loss", false);
            res.put("ok", true);
            res.put("type", "guessWord");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < guess.length(); i++) {
                sb.append(" ");
                sb.append(guess.charAt(i));
            }
            res.put("word", sb.toString());
            res.put("won", true);
            res.put("guess", "correct");
            int tempScore = currentWord.length()*10;
            for (int j = 0; j < currentWord.length(); j++) {
                char c = currentWord.charAt(j);
                if (guessedLetters.contains(c)) tempScore -= 10;
            }
            score += tempScore + 50;
            if (wrongGuesses == 0) score += 20;
            res.put("score", score);
            res.put("hangStage", HANGMAN_STAGES[wrongGuesses]);
            updateLeaderboard(playerName, score, difficulty);
            String temp = playerName;
            resetGame();
            playerName = temp;
        }
        else {
            res.put("ok", true);
            res.put("type", "guessWord");
            res.put("won", false);
            res.put("score", score);
            res.put("guess", "incorrect");
            wrongGuesses++;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < currentWord.length(); i++) {
                char c = currentWord.charAt(i);
                if (guessedLetters.contains(c)) {
                    String spaceAdded =" " + c;
                    sb.append(spaceAdded);
                }
                else {
                    sb.append(" _");
                }
            }
            String word = sb.toString();
            res.put("word", word);
            if (wrongGuesses == 9){
                res.put("loss", true);
                res.put("hangStage", HANGMAN_STAGES[wrongGuesses]);
                res.put("finalWord", currentWord);
                updateLeaderboard(playerName, score, difficulty);
                String temp = playerName;
                resetGame();
                playerName = temp;
            }
            else{
                res.put("loss", false);
                res.put("hangStage", HANGMAN_STAGES[wrongGuesses]);
            }
        }

        return res;
    }

    static JSONObject guessLetter(JSONObject req) {
        JSONObject res = new JSONObject();
        String guess = req.getString("letter");
        if (guess == null) {
            res.put("ok", false);
            res.put("message", "Guess cannot be empty");
            return res;
        }
        else if (!Character.isLetter(guess.charAt(0))) {
            res.put("ok", false);
            res.put("message", "Guess must be a letter such as 'a'");
            return res;
        }
        else if (guessedLetters.contains(guess.charAt(0))) {
            res.put("ok", false);
            res.put("message", "You already guessed this letter");
            return res;
        }
        guess = guess.toLowerCase();
        guessedLetters.add(guess.charAt(0));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentWord.length(); i++) {
            char c = currentWord.charAt(i);
            if (guessedLetters.contains(c)) {
                String spaceAdded =" " + c;
                sb.append(spaceAdded);
            }
            else {
                sb.append(" _");
            }
        }
        String word = sb.toString();
        res.put("ok", true);
        res.put("type", "guessLetter");
        res.put("word", word);
        if (currentWord.contains(guess)) {
            res.put("loss", false);
            int count = 0;
            for (int i = 0; i < currentWord.length(); i++) {
                if (currentWord.charAt(i) == guess.charAt(0)) {
                    count++;
                }

            }
            score += 10*count;
            if (!word.contains("_")) {
                res.put("won", true);
                score += 50;
                if (wrongGuesses == 0) score += 20;
                updateLeaderboard(playerName, score, difficulty);
                res.put("score", score);
                res.put("hangStage", HANGMAN_STAGES[wrongGuesses]);
                String temp = playerName;
                resetGame();
                playerName = temp;
            }
            else{
                res.put("won", false);
                res.put("score", score);
                res.put("hangStage", HANGMAN_STAGES[wrongGuesses]);
            }
            res.put("guess", "correct");
        }
        else{
            res.put("won", false);
            if (score >= 5) score -= 5;
            res.put("score", score);
            res.put("guess", "incorrect");
            wrongGuesses++;
            if (wrongGuesses == 9){
                res.put("loss", true);
                res.put("hangStage", HANGMAN_STAGES[wrongGuesses]);
                res.put("finalWord", currentWord);
                updateLeaderboard(playerName, score, difficulty);
                String temp = playerName;
                resetGame();
                playerName = temp;
            }
            else{
                res.put("loss", false);
                res.put("hangStage", HANGMAN_STAGES[wrongGuesses]);
            }
        }
        return res;
    }

    static void updateLeaderboard(String name, int score, String difficulty) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("name", name);
        entry.put("score", score);
        entry.put("difficulty", difficulty);
        leaderboard.add(entry);
        leaderboard.sort((a, b) ->
                Integer.compare(((Number) b.get("score")).intValue(), ((Number) a.get("score")).intValue()));
        if (leaderboard.size() > 10) leaderboard.removeLast();
    }

    static JSONObject startGame(JSONObject req) {
        JSONObject res = new JSONObject();
        Random rand = new Random();
        difficulty = req.getString("difficulty");
        if (!(difficulty.equals("easy") || difficulty.equals("medium") || difficulty.equals("hard"))) {
            res.put("ok", false);
            res.put("message", "Difficulty must be easy, medium, or hard");
            return res;
        }
        else if (difficulty.equals("easy")) {
            int wordNum = rand.nextInt(12);
            currentWord = EASY_WORDS[wordNum];
        }
        else if (difficulty.equals("medium")) {
            int wordNum = rand.nextInt(14);
            currentWord = MEDIUM_WORDS[wordNum];
        }
        else {
            int wordNum = rand.nextInt(14);
            currentWord = HARD_WORDS[wordNum];
        }
        String codedWord = " _".repeat(currentWord.length());
        gameActive = true;

        res.put("ok", true);
        res.put("word", codedWord);
        res.put("length", currentWord.length());
        res.put("difficulty", difficulty);
        res.put("type", "start");
        res.put("hangStage", HANGMAN_STAGES[0]);
        System.out.println("The word is " + currentWord);
        return res;
    }



    /**
     * EXAMPLE IMPLEMENTATION: Set player name
     * This is provided as a complete example of request handling.
     * Use this as a reference for implementing other handlers.
     */
    static JSONObject handleName(JSONObject req) {
        System.out.println("Name request: " + req.toString());
        JSONObject res = testField(req, "name");
        if (!res.getBoolean("ok")) {
            return res;
        }

        String name = req.getString("name");
        if (name == null || name.trim().isEmpty()) {
            res = new JSONObject();
            res.put("ok", false);
            res.put("message", "Name cannot be empty");
            return res;
        }

        playerName = name.trim();
        res = new JSONObject();
        res.put("ok", true);
        res.put("type", "name");
        res.put("message", "Welcome " + playerName + "! Ready to play Hangman?");
        return res;
    }

    /**
     * Quit handler
     */
    static JSONObject handleQuit(JSONObject req) {
        System.out.println("Quit request: " + req.toString());
        JSONObject res = new JSONObject();

        res.put("ok", true);
        res.put("type", "quit");
        res.put("message", "Goodbye " + (playerName != null ? playerName : "player") + "!");

        return res;
    }

    /**
     * Helper: Reset game state for new connection
     */
    static void resetGame() {
        playerName = null;
        currentWord = null;
        difficulty = null;
        guessedLetters = new HashSet<>();
        wrongGuesses = 0;
        score = 0;
        gameActive = false;
    }

    /**
     * Helper: Check if field exists in request
     */
    static JSONObject testField(JSONObject req, String key) {
        JSONObject res = new JSONObject();
        if (!req.has(key)) {
            res.put("ok", false);
            res.put("message", "Field '" + key + "' does not exist in request");
            return res;
        }
        return res.put("ok", true);
    }

    /**
     * Helper: Validate JSON
     */
    static JSONObject isValid(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            try {
                new JSONArray(json);
            } catch (JSONException ne) {
                JSONObject res = new JSONObject();
                res.put("ok", false);
                res.put("message", "Request is not valid JSON");
                return res;
            }
        }
        return new JSONObject();
    }

    /**
     * Error: no type field
     */
    static JSONObject noType(JSONObject req) {
        System.out.println("No type request: " + req.toString());
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "No request type was given");
        return res;
    }

    /**
     * Error: wrong type
     */
    static JSONObject wrongType(JSONObject req) {
        System.out.println("Wrong type request: " + req.toString());
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "Type '" + req.getString("type") + "' is not supported");
        return res;
    }

    /**
     * Load hangman ASCII art stages from resource file
     */
    static void loadHangmanStages() {
        try {
            InputStream is = HangmanServer.class.getResourceAsStream("/hangman_stages.txt");
            if (is == null) {
                System.err.println("Error: hangman_stages.txt not found in resources");
                System.exit(1);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder currentStage = new StringBuilder();
            int stageIndex = 0;

            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    HANGMAN_STAGES[stageIndex++] = "\n" + currentStage.toString();
                    currentStage = new StringBuilder();
                } else if (!line.startsWith("STAGE")) {
                    currentStage.append(line).append("\n");
                }
            }
            // Add final stage
            if (currentStage.length() > 0 && stageIndex < 10) {
                HANGMAN_STAGES[stageIndex] = "\n" + currentStage.toString();
            }
            reader.close();
            System.out.println("Loaded " + (stageIndex + 1) + " hangman stages");
        } catch (Exception e) {
            System.err.println("Error loading hangman stages: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Load word lists from resource files
     */
    static void loadWords() {
        try {
            EASY_WORDS = loadWordList("/easy_words.txt");
            MEDIUM_WORDS = loadWordList("/medium_words.txt");
            HARD_WORDS = loadWordList("/hard_words.txt");

            System.out.println("Loaded word lists: " + EASY_WORDS.length + " easy, " +
                             MEDIUM_WORDS.length + " medium, " + HARD_WORDS.length + " hard");
        } catch (Exception e) {
            System.err.println("Error loading word lists: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Helper: Load a single word list from file
     */
    static String[] loadWordList(String filename) throws IOException {
        InputStream is = HangmanServer.class.getResourceAsStream(filename);
        if (is == null) {
            throw new IOException("Word list file not found: " + filename);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        List<String> words = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                words.add(line.toLowerCase());
            }
        }
        reader.close();

        return words.toArray(new String[0]);
    }

    /**
     * Write response to client
     */
    static void writeOut(JSONObject res) {
        try {
            os.writeObject(res.toString());
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Close connection
     */
    static void overandout() {
        try {
            if (os != null) os.close();
            if (in != null) in.close();
            if (sock != null) sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
