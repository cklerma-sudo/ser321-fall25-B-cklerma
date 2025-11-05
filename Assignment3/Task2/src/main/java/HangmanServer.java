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
                    } else {
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
