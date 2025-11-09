import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 * Hangman Game Client - Student Starter Code
 *
 * Your task: Implement the protocol communication for all game features.
 *
 * What's provided:
 * - Complete menu structure with different game states
 * - Name handling as a complete example
 * - Method stubs for all features
 *
 * What you need to implement:
 * - Protocol requests/responses for all game operations
 * - Proper response handling and display
 */
public class HangmanClient {
    static Socket sock;
    static ObjectOutputStream oos;
    static ObjectInputStream in;

    static Scanner scanner = new Scanner(System.in);
    static boolean gameInProgress = false;
    static boolean hasName = false;
    static String playerName = "";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            sock = new Socket(host, port);
            oos = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());

            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║     WELCOME TO HANGMAN GAME!           ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println();

            boolean running = true;
            while (running) {
                if (!hasName) {
                    running = showInitialMenu();
                } else if (!gameInProgress) {
                    running = showMainMenu();
                } else {
                    running = showGameMenu();
                }
                System.out.println();
            }

            overandout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initial menu - before name is set
     */
    static boolean showInitialMenu() {
        System.out.println("────────────────────────────────────────");
        System.out.println("  1. Set Your Name");
        System.out.println("  2. Quit");
        System.out.println("────────────────────────────────────────");
        System.out.print("Enter choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                setName();
                return true;
            case "2":
                quit();
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
                return true;
        }
    }

    /**
     * Main menu - after name set, no active game
     */
    static boolean showMainMenu() {
        System.out.println("────────────────────────────────────────");
        System.out.println("MAIN MENU:");
        System.out.println("  1. Start New Game");
        System.out.println("  2. View Leaderboard");
        System.out.println("  3. Quit");
        System.out.println("────────────────────────────────────────");
        System.out.print("Enter choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                startGame();
                return true;
            case "2":
                getLeaderboard();
                return true;
            case "3":
                quit();
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
                return true;
        }
    }

    /**
     * Game menu - during active game
     * Natural input: just type letter/word to guess
     * Commands: 1, 2, 3, 0 for special actions
     */
    static boolean showGameMenu() {
        System.out.println("\n────────────────────────────────────────");
        System.out.println("Type a letter or word to guess");
        System.out.println("Or choose:");
        System.out.println("  1 - Show game state");
        System.out.println("  2 - See guessed letters");
        System.out.println("  3 - Give up (return to main menu)");
        System.out.println("  0 - Quit game");
        System.out.println("────────────────────────────────────────");
        System.out.print("Your input: ");
        String input = scanner.nextLine().trim();

        // Handle special commands
        if (input.equals("1")) {
            getState();
            return true;
        } else if (input.equals("2")) {
            getLetters();
            return true;
        } else if (input.equals("3")) {
            giveUp();
            return true;
        } else if (input.equals("0")) {
            quit();
            gameInProgress = false;
            return false;
        }

        if (input.isEmpty()) {
            System.out.println("Please enter a letter, word, or command.");
            return true;
        }

        // Single character = letter guess, multiple = word guess
        if (input.length() == 1) {
            guessLetter(input);
        } else {
            guessWord(input);
        }

        return true;
    }

    /**
     * TODO: Implement give up
     * IMPORTANT: This should send a request to the server to end the game!
     * Just setting gameInProgress = false locally creates a state mismatch
     * where the client thinks the game is over but the server still has it active.
     *
     * Proper implementation should:
     * - Confirm with user
     * - Send "give up" or "end game" request to server
     * - Server ends game, adds to leaderboard if desired
     * - Server responds confirming game ended
     * - Client sets gameInProgress = false
     */
    static void giveUp() {
        System.out.print("\nAre you sure you want to give up? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("yes") || confirm.equals("y")) {
            JSONObject req = new JSONObject();
            req.put("type", "return");
            JSONObject res = sendRequest(req);
            if (res != null && res.getBoolean("ok")) {
                gameInProgress = false;
                System.out.println(res.getString("message"));
                System.out.println("\nYou gave up! Returning to main menu...\n");
            }
            else{
                System.out.println("Unexpected error, please try again.");
            }
        } else {
            System.out.println("\nContinuing game...");
        }
    }

    /**
     * EXAMPLE IMPLEMENTATION: Set player name
     * This is provided as a complete example.
     * Use this as a reference for implementing other methods.
     */
    static void setName() {
        System.out.print("\nEnter your name: ");
        String name = scanner.nextLine().trim();

        // Create request according to YOUR protocol design
        JSONObject request = new JSONObject();
        request.put("type", "name");
        request.put("name", name);

        // Send request and get response
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                hasName = true;
                playerName = name;
                System.out.println("\n" + response.getString("message"));
                System.out.println();
            } else {
                System.out.println("✗ Error: " + response.getString("message"));
            }
        }
    }

    static void startGame() {
        // the difficulty is part of the middle part you can skip that or comment it while you do the core if you like
        System.out.println("\nSelect difficulty:");
        System.out.println("  1. Easy (long words - more clues)");
        System.out.println("  2. Medium (moderate length words)");
        System.out.println("  3. Hard (short words - fewer clues)");
        System.out.print("Enter choice (1-3): ");
        String diffChoice = scanner.nextLine().trim();

        String difficulty = "easy";
        switch (diffChoice) {
            case "1":
                difficulty = "easy";
                break;
            case "2":
                difficulty = "medium";
                break;
            case "3":
                difficulty = "hard";
                break;
            default:
                System.out.println("Invalid choice, defaulting to easy");
        }

        JSONObject req = new JSONObject();
        req.put("difficulty", difficulty);
        req.put("type", "start");
        JSONObject res = sendRequest(req);
        if (res != null && res.getBoolean("ok")) {
            System.out.println("New game started! Difficulty: " + res.getString("difficulty"));
            System.out.println("Word Length: " + res.getInt("length"));
            System.out.println("Max wrong guesses: 9");
            System.out.println(res.getString("hangStage"));
            System.out.println("Word:" + res.getString("word"));
            gameInProgress = true;
        }
        else if (res != null) {
            System.out.println("Error starting game:" + res.getString("message"));
        }
        else {
            System.out.println("Error starting game...");
        }
    }

    static void guessLetter(String letter) {
        if (!gameInProgress) {
            System.out.println("✗ No game in progress. Start a new game first.");
            return;
        }
        JSONObject req = new JSONObject();
        req.put("type", "guessLetter");
        req.put("letter", letter);
        JSONObject res = sendRequest(req);
        if (res != null && res.getBoolean("ok")) {
            if (!(res.getBoolean("won") || res.getBoolean("loss"))) {
                System.out.println("Score: " + res.getInt("score"));
                System.out.println(res.getString("hangStage"));
                System.out.println("Word: " + res.getString("word"));
            }
            else if (res.getBoolean("loss")) {
                gameInProgress = false;
                System.out.println("Score: " + res.getInt("score"));
                System.out.println(res.getString("hangStage"));
                System.out.println("Word: " + res.getString("word"));
                System.out.println("Game Over! The word was: " + res.getString("finalWord"));
                System.out.println("Final Score: " + res.getInt("score"));
            }
            else if (res.getBoolean("won")) {
                gameInProgress = false;
                System.out.println("Score: " + res.getInt("score"));
                System.out.println(res.getString("hangStage"));
                System.out.println("Word: " + res.getString("word"));
                System.out.println("You won!");
                System.out.println("Final Score: " + res.getInt("score"));
            }


        }
        else if (res != null) {
            System.out.println(res.getString("message"));
        }
        else {
            System.out.println("Unexpected error, please try again.");
        }
    }

    static void guessWord(String word) {
        if (!gameInProgress) {
            System.out.println("✗ No game in progress. Start a new game first.");
            return;
        }
        JSONObject req = new JSONObject();
        req.put("type", "guessWord");
        req.put("letter", word);
        JSONObject res = sendRequest(req);
        if (res != null && res.getBoolean("ok")) {
            if (!(res.getBoolean("won") || res.getBoolean("loss"))) {
                System.out.println("Score: " + res.getInt("score"));
                System.out.println(res.getString("hangStage"));
                System.out.println("Word: " + res.getString("word"));
            }
            else if (res.getBoolean("loss")) {
                gameInProgress = false;
                System.out.println("Score: " + res.getInt("score"));
                System.out.println(res.getString("hangStage"));
                System.out.println("Word: " + res.getString("word"));
                System.out.println("Game Over! The word was: " + res.getString("finalWord"));
                System.out.println("Final Score: " + res.getInt("score"));
            }
            else if (res.getBoolean("won")) {
                gameInProgress = false;
                System.out.println("Score: " + res.getInt("score"));
                System.out.println(res.getString("hangStage"));
                System.out.println("Word: " + res.getString("word"));
                System.out.println("You won!");
                System.out.println("Final Score: " + res.getInt("score"));
            }


        }
        else if (res != null) {
            System.out.println(res.getString("message"));
        }
        else {
            System.out.println("Unexpected error, please try again.");
        }

    }

    static void getState() {
        if (!gameInProgress) {
            System.out.println("✗ No game in progress. Start a new game first.");
            return;
        }
        JSONObject req = new JSONObject();
        req.put("type", "state");
        JSONObject res = sendRequest(req);
        if (res != null && res.getBoolean("ok")) {
            System.out.println("=== GAME STATE ===");
            System.out.println(res.getString("hangStage"));
            System.out.println("Word: " + res.getString("word"));
            System.out.println("Wrong Guesses: " + res.getString("guesses"));
            System.out.println("Score: " + res.getInt("score"));
            System.out.println("Letters guessed: " + res.getInt("lettersAmount"));
        }
        else{
            System.out.println("Unexpected error, please try again.");
        }
    }

    static void getLetters() {
        if (!gameInProgress) {
            System.out.println("✗ No game in progress. Start a new game first.");
            return;
        }
        JSONObject req = new JSONObject();
        req.put("type", "letters");
        JSONObject res = sendRequest(req);
        if (res != null && res.getBoolean("ok")) {
            System.out.println("=== Guessed Letters ===");
            String letters = " ";
            JSONArray lettersGuessed = res.getJSONArray("lettersGuessed");
            for (int i = 0; i < res.getInt("total"); i++){
                letters += lettersGuessed.getString(i) + ", ";
            }
            System.out.println("Letters:" + letters);
            System.out.println("Total: " + res.getInt("total"));

        }
        else {
            System.out.println("Unexpected error, please try again.");
        }
    }

    static void getLeaderboard() {
        JSONObject req = new JSONObject();
        req.put("type", "leaderboard");
        JSONObject res = sendRequest(req);
        if (res != null && res.getBoolean("ok")) {
            System.out.println("=========== Leaderboard (Top 10) ===========");
            System.out.println("Rank  Name                  Score Difficulty");
            System.out.println("--------------------------------------------");
            if (res.getInt("count") == 0) System.out.println("No one is on the leaderboard yet!");
            else {
                JSONArray leaderboard = res.getJSONArray("leaderboard");
                for (int i = 0; i < leaderboard.length(); i++) {
                    JSONObject obj = leaderboard.getJSONObject(i);
                    int rank = obj.getInt("rank");
                    int score = obj.getInt("score");
                    String difficulty = obj.getString("difficulty");
                    String name = obj.getString("name");
                    System.out.printf("%-5d %-21s %-5d %-10s\n", rank, name, score, difficulty);
                }
            }
        }
        else {
            System.out.println("Unexpected error, please try again.");
        }
    }

    /**
     * Quit game
     */
    static boolean quit() {
        JSONObject request = new JSONObject();
        request.put("type", "quit");

        JSONObject response = sendRequest(request);
        if (response != null && response.getBoolean("ok")) {
            try{System.out.println("\n" + response.getString("message"));}
            catch (JSONException e) {
                System.out.println("Goodbye");
            }
            System.out.println("Thanks for playing!");
        }
        return false; // Stop the main loop
    }

    /**
     * Helper: Send request and receive response
     * This handles the basic communication pattern
     */
    static JSONObject sendRequest(JSONObject request) {
        try {
            String req = request.toString();
            oos.writeObject(req);
            oos.flush();

            String res = (String) in.readObject();
            return new JSONObject(res);
        } catch (Exception e) {
            System.out.println("Error communicating with server: " + e.getMessage());
            return null;
        }
    }

    /**
     * Close connection
     */
    static void overandout() {
        try {
            if (oos != null) oos.close();
            if (in != null) in.close();
            if (sock != null) sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
