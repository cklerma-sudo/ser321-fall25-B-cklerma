package auction;

import buffers.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Auction Game Server - Players compete against bot opponents.
 * Each player plays independently against 2 bots.
 */
public class AuctionServer {
    private static final int DEFAULT_PORT = 8889;
    private static final String LEADERBOARD_FILE = "leaderboard.txt";

    private static final int startGold = 100;

    // Shared leaderboard
    private static LeaderboardManager leaderboard;

    // Track connected player names (to prevent duplicates)
    private static Set<String> activePlayerNames = new HashSet<>();

    // Grading mode flag
    private static boolean gradingMode = false;

    // Bot opponent name pool
    private static final String[] BOT_NAMES = {
            "Alice", "Bob", "Charlie", "Dana",
            "Eve", "Frank", "Grace", "Henry"
    };
    private static Random botNameRandom = new Random();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--grading")) {
                gradingMode = true;
                System.out.println("Running in grading mode (deterministic results)");
            } else {
                try {
                    port = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[i]);
                }
            }
        }

        // Initialize leaderboard
        leaderboard = new LeaderboardManager(LEADERBOARD_FILE);
        System.out.println("Leaderboard loaded with " + leaderboard.size() + " scores");


        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Auction Server started on port " + port);
            System.out.println("Waiting for connections...");

            int clientId = 0;
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientId++;
                    final int id = clientId;
                    System.out.println("Client " + id + " connected from " +
                            clientSocket.getInetAddress().getHostAddress());
                    Thread t = new Thread(() -> handleClient(clientSocket, id));
                    t.start();
                        
                } catch (IOException e) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Handle a client connection (runs in thread pool).
     */
    private static void handleClient(Socket clientSocket, int clientId) {
        String playerName = null;
        PlayerGameState gameState = null;

        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            System.out.println("[Client " + clientId + "] Handler started");

            // Send initial welcome
            sendWelcome(out, "Welcome to the Auction Game! Please set your name.");

            // Read and process requests
            Request request;
            while ((request = Request.parseDelimitedFrom(in)) != null) {
                Request.RequestType type = request.getType();
                System.out.println("[Client " + clientId + "] Received: " + type);

                Response response = null;

                switch (type) {
                    case NAME:
                        String[] result = handleName(request, playerName);
                        playerName = result[0];
                        String message = result[1];
                        if (playerName != null) {
                            response = buildWelcome("Welcome, " + playerName + "! You have "+startGold+" gold. " +
                                    "Type 'join' to start playing against bot opponents!");
                        } else {
                            response = buildError(message);
                        }
                        break;
                    case JOIN:
                        if (gameState != null) {
                            response = buildError("You are already in a game");
                            break;
                        }
                        gameState = new PlayerGameState(playerName, gradingMode);
                        response = handleJoin(gameState);
                        break;
                    case BID:
                         if (gameState == null) {
                            response = buildError("You must join a game first");
                            break;
                        }
                        response = handleBid(request, gameState);
                        break;
                    case LEADERBOARD:
                        response = handleLeaderboard();
                        break;
                    case QUIT:
                        response = handleQuit(gameState);
                        if (response != null) {
                            response.writeDelimitedTo(out);
                        }
                        return; // Exit handler

                    default:
                        response = buildError("Unknown request type");
                }

                if (response != null) {
                    if (response.getType() == Response.ResponseType.BID_RESULT && response.getNextItem() == null){
                        response.writeDelimitedTo(out);
                        response = handleGameOver(gameState);
                        response.writeDelimitedTo(out);
                        break;
                    }
                    else {
                        response.writeDelimitedTo(out);
                    }
                }
            }

            System.out.println("[Client " + clientId + "] Disconnected");

        } catch (IOException e) {
            System.err.println("[Client " + clientId + "] Error: " + e.getMessage());
        } finally {
            // Cleanup
            if (playerName != null) {
                activePlayerNames.remove(playerName);
                System.out.println("[Client " + clientId + "] Removed player: " + playerName);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static Response handleLeaderBoard(){
        List<LeaderboardEntry> leaderboard = getTopScores(10);
        Response response = Response.newBuilder()
            .setType(Response.ResponseType.LEADERBOARD_RESPONSE)
            .setOk(true)
            .setMessage("Top 10 Scores:")
            .addLeaderboard(leaderboard)
            .build();
        return response;

    }

    private static Response handleGameOver (PlayerGameState gameState) {
        List<String> playerItems =  gameState.getItemNames();
        List<String> bot1Items =  gameState.getBot1().getItemNames();
        List<String> bot2Items =  gameState.getBot2().getItemNames();
        int leaderboardPos = leaderboard.addScore(gameState.getPlayerName(), gameState.getPlayerScore());
        PlayerStats player = PlayerStats.newBuilder()
            .setPlayerName(gameState.getPlayerName())
            .setGoldRemaining(gameState.getGold())
            .setItemsValue(gameState.getInventoryValue())
            .setTotalScore(gameState.getPlayerScore())
            .addAllItemsWon(playerItems)
            .build();
         PlayerStats bot1 = PlayerStats.newBuilder()
            .setPlayerName(gameState.getBot1().getName())
            .setGoldRemaining(gameState.getBot1().getGold())
            .setItemsValue(gameState.getBot1().getInventoryValue())
            .setTotalScore(gameState.getBot1().getTotalScore())
            .addAllItemsWon(bot1Items)
            .build();
        PlayerStats bot2 = PlayerStats.newBuilder()
            .setPlayerName(gameState.getBot2().getName())
            .setGoldRemaining(gameState.getBot2().getGold())
            .setItemsValue(gameState.getBot2().getInventoryValue())
            .setTotalScore(gameState.getBot2().getTotalScore())
            .addAllItemsWon(bot2Items)
            .build();
        String winner;
         if (gameState.getPlayerScore() > gameState.getBot1().getTotalScore() && gameState.getPlayerScore() > gameState.getBot2().getTotalScore()) {
            winner = gameState.getPlayerName();
       
        }
        else if (gameState.getBot2().getTotalScore() > gameState.getPlayerScore() && gameState.getBot2().getTotalScore() > gameState.getBot1().getTotalScore()) {
            winner = gameState.getBot2().getName();
            
        }
        else {
            winner = gameState.getBot1().getName();
            
        }
         GameResult result = GameResult.newBuilder()
            .addPlayerScores(player)
            .addPlayerScores(bot1)
            .addPlayerScores(bot2)
            .setWinnerName(winner)
            .setLeaderBoardPosition(leaderboardPos)
            .build();
             
        Response response = Response.newBuilder()
            .setType(Response.ResponseType.GAME_OVER)
            .setOk(true)
            .setMessage("Game over! Final results:")
            .setGameResult(result)
            .build();
        return response;

    }

    private static Response handleBid(Request request, PlayerGameState gameState){
        String validBid = gameState.validateBid(request.getItemId(), request.getBidAmount());
        if (validBid != null) return buildError(validBid);
        int bot1Bid = gameState.getBot1().decideBid(gameState.getCurrentItem());
        int bot2Bid = gameState.getBot2().decideBid(gameState.getCurrentItem());
        int playerBid = request.getBidAmount();
        String winner;
        int largestBid;
        if (playerBid > bot1Bid && playerBid > bot2Bid) {
            winner = gameState.getPlayerName();
            awardItemToPlayer(gameState.getCurrentItem(), playerBid);
            largestBid = playerBid;
        }
        else if (bot2Bid > playerBid && bot2Bid > bot1Bid) {
            winner = gameState.getBot2().getName();
            gameState.getBot2().awardItem(gameState.getCurrentItem(), bot2Bid);
            largestBid = bot2Bid;
        }
        else {
            winner = gameState.getBot1().getName();
            gameState.getBot1().awardItem(gameState.getCurrentItem(), bot1Bid);
            largestBid = bot1Bid;
        }
        AuctionItem item = AuctionItem.newBuilder()
            .setId(gameState.getCurrentItem().getId())
            .setName(gameState.getCurrentItem().getName())
            .setCategory(gameState.getCurrentItem().getCategory())
            .setMinValue(gameState.getCurrentItem().getMinValue())
            .setMaxValue(gameState.getCurrentItem().getMaxValue())
            .build();
        PlayerBid player = PlayerBid.newBuilder()
            .setPlayerName(gameState.getPlayerName())
            .setBidAmount(playerBid)
            .build();
         PlayerBid bot1 = PlayerBid.newBuilder()
            .setPlayerName(gameState.getBot1().getName)
            .setBidAmount(bot1Bid)
            .build();
        PlayerBid bot2 = PlayerBid.newBuilder()
            .setPlayerName(gameState.getBot2().getName)
            .setBidAmount(bot2Bid)
            .build();
        AuctionResult result = AuctionResult.newBuilder()
            .setItem(item)
            .setActualValue(gameState.getCurrentItem().getActualValue())
            .setWinnerName(winner)
            .addAllBids(player)
            .addAllBids(bot1)
            .addAllBids(bot2)
            .build();
        PlayerStats stats = PlayerStats.newBuilder()
            .setGoldRemaining(gameState.getGold())
            .build();
        if (gameState.moveToNextItem()) {
             AuctionItem nextItem = AuctionItem.newBuilder()
                .setId(gameState.getCurrentItem().getId())
                .setName(gameState.getCurrentItem().getName())
                .setCategory(gameState.getCurrentItem().getCategory())
                .setMinValue(gameState.getCurrentItem().getMinValue())
                .setMaxValue(gameState.getCurrentItem().getMaxValue())
                .build();
            Response response = Response.newBuilder()
                .setType(Response.ResponseType.BID_RESULT)
                .setOk(true)
                .setMessage("Auction complete!")
                .setResult(result)
                .setPlayerStats(stats)
                .setNextItem(nextItem)
                .build();
            return response;

        } else {
            Response response = Response.newBuilder()
                .setType(Response.ResponseType.BID_RESULT)
                .setOk(true)
                .setMessage("Auction complete!")
                .setResult(result)
                .setPlayerStats(stats)
                .build();
            return response;
        }
            

    }
    
    private static Response handleJoin(PlayerGameState gameState) {
        if (gameState.getPlayerName() == null) return buildError("Please set your name first");
        
        PlayerStats player = PlayerStats.newBuilder().setGoldRemaining(100).build();
        AuctionItem item = AuctionItem.newBuilder()
            .setId(gameState.getCurrentItem().getId())
            .setName(gameState.getCurrentItem().getName())
            .setCategory(gameState.getCurrentItem().getCategory())
            .setMinValue(gameState.getCurrentItem().getMinValue())
            .setMaxValue(gameState.getCurrentItem().getMaxValue())
            .build();
        Response response = Response.newBuilder()
            .setType(Response.ResponseType.GAME_JOINED)
            .setOk(true)
            .setMessage("Game started! You're playing against Bot-" + gameState.getBot1().getName() + "and Bot-" + gameState.getBot2().getName() + ". Current item:")
            .setPlayerStats(player)
            .setNextItem(item)
            .build();

    }

    /**
     * Handle NAME request - set player name.
     * Returns [playerName, errorMessage] - playerName is null if error.
     */
    private static String[] handleName(Request request, String currentName) {
        String name = request.getName().trim();

        if (name.isEmpty()) {
            return new String[]{null, "Name cannot be empty"};
        }

        if (activePlayerNames.contains(name)) {
            return new String[]{null, "Name already taken. Please choose another."};
        }

        // Add new name
        activePlayerNames.add(name);
        return new String[]{name, null};
    }

    /**
     * Handle QUIT request.
     */
    private static Response handleQuit(PlayerGameState gameState) {
        String message = "Thanks for playing!";
        if (gameState != null) {
            message += " Final score: " + gameState.getPlayerScore() + ".";
        }
        message += " Goodbye!";

        return Response.newBuilder()
                .setType(Response.ResponseType.BYE)
                .setOk(true)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: send welcome response.
     */
    private static void sendWelcome(OutputStream out, String message) throws IOException {
        buildWelcome(message).writeDelimitedTo(out);
    }

    /**
     * Helper: build welcome response.
     */
    private static Response buildWelcome(String message) {
        return Response.newBuilder()
                .setType(Response.ResponseType.WELCOME)
                .setOk(true)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: build error response.
     */
    private static Response buildError(String message) {
        return Response.newBuilder()
                .setType(Response.ResponseType.ERROR)
                .setOk(false)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: convert Item to protobuf AuctionItem.
     */
    private static AuctionItem itemToProto(Item item) {
        return AuctionItem.newBuilder()
                .setId(item.getId())
                .setName(item.getName())
                .setCategory(item.getCategory())
                .setMinValue(item.getMinValue())
                .setMaxValue(item.getMaxValue())
                .build();
    }

    /**
     * Helper: get random bot name.
     */
    private static String getRandomBotName() {
        return BOT_NAMES[botNameRandom.nextInt(BOT_NAMES.length)];
    }

    /**
     * Inner class to track player game state.
     */
    private static class PlayerGameState {
        private String playerName;
        private int gold;
        private List<Item> inventory;
        private List<Item> items;
        private int currentItemIndex;
        private BotOpponent bot1;
        private BotOpponent bot2;

        public PlayerGameState(String playerName, boolean gradingMode) {
            this.playerName = playerName;
            this.gold = startGold;
            this.inventory = new ArrayList<>();

            // Load items
            this.items = ItemLoader.loadItems(gradingMode);
            this.currentItemIndex = 0;

            // Create bot opponents
            this.bot1 = new BotOpponent(getRandomBotName(), gradingMode);
            this.bot2 = new BotOpponent(getRandomBotName(), gradingMode);

            // Ensure different names
            while (bot2.getName().equals(bot1.getName())) {
                bot2 = new BotOpponent(getRandomBotName(), gradingMode);
            }
        }

        public String validateBid(int itemId, int bidAmount) {
            Item currentItem = getCurrentItem();

            if (currentItem.getId() != itemId) {
                return "Invalid item ID. Current item is #" + currentItem.getId();
            }

            if (bidAmount < 0) {
                return "Bid cannot be negative";
            }

            if (bidAmount > gold) {
                return "Insufficient gold. You have " + gold + " gold.";
            }

            return null; // Valid
        }

        public void awardItemToPlayer(Item item, int bidAmount) {
            inventory.add(item);
            gold -= bidAmount;
        }

        public boolean moveToNextItem() {
            currentItemIndex++;
            return currentItemIndex < items.size();
        }

        public Item getCurrentItem() {
            return items.get(currentItemIndex);
        }

        public int getInventoryValue() {
            int total = 0;
            for (Item item : inventory) {
                total += item.getActualValue();
            }
            return total;
        }

        public int getPlayerScore() {
            return gold + getInventoryValue();
        }

        public List<String> getItemNames() {
            List<String> names = new ArrayList<>();
            for (Item item : inventory) {
                names.add(item.getName());
            }
            return names;
        }

        // Getters
        public String getPlayerName() { return playerName; }
        public int getGold() { return gold; }
        public List<Item> getInventory() { return new ArrayList<>(inventory); }
        public BotOpponent getBot1() { return bot1; }
        public BotOpponent getBot2() { return bot2; }
    }
}
