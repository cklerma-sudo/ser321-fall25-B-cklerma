# Activity 2 Alternative: Auction Game vs AI Opponents

## Overview

This is an alternative implementation of the auction game where players compete against AI opponents instead of other human players. This design eliminates the threading complexity of barrier synchronization while still teaching thread pools and basic synchronization.

## Key Differences from Original

### Original Version (activity2-solution)
- Players wait for N other human players to join
- Barrier synchronization (wait for all joins, wait for all bids)
- Broadcasting to all connected players
- Complex threading coordination

### Alternative Version (this directory)
- Player competes against 2 AI opponents
- Game starts immediately (no waiting)
- Each player's game is independent
- Only synchronization: shared leaderboard
- Much simpler threading model

## Game Mechanics

1. **Player connects** and sets name
2. **Player joins** → Server creates game with player + 2 AI bots
3. **Game starts immediately** with first auction item (5 items total)
4. **Player bids** → AI opponents bid automatically → winner determined
5. **Repeat** for all 5 items
6. **Game ends** → Player's score added to shared leaderboard
7. **Leaderboard** shows top 10 scores from all players

### Strategic Design

**Hidden Information**: During the game, players can see:
- Their own gold amount
- All bid amounts (theirs and AI)
- Who won each auction

**NOT visible during game**:
- AI gold amounts (hidden until GAME_OVER)

This prevents "wait for AI to go broke" strategies and maintains tension throughout the game. Observant players may track AI spending patterns, but it requires analysis!

## What Students Implement

### Threading (Must Implement)
- Thread pool with ExecutorService
- Each client runs in separate thread
- Threads are independent (no coordination)

### Game Logic
- Process JOIN request → create game with AI opponents
- Process BID request → get AI bids, determine winner, award item
- Track player gold and inventory
- Calculate final score

### Leaderboard (Only Sync Point)
- Add score to shared leaderboard (synchronized)
- Query top scores (synchronized)
- This is the ONLY place students need synchronization!

## Files in This Directory

### Complete (Provided to Students)
- `Item.java` - Item data structure
- `ItemLoader.java` - Loads items from file
- `AIOpponent.java` - AI bidding logic (students don't modify)
- `LeaderboardManager.java` - Thread-safe leaderboard
- `AuctionClient.java` - Complete menu-driven client
- `auction.proto` - Protocol Buffers definition
- `items.txt` - Fantasy items for auction
- `PROTO_PROTOCOL_V2.md` - Complete protocol specification
- `build.gradle` - Gradle build file

### Solution (This File)
- `AuctionServer.java` - Complete reference implementation

### What Students Get (for student version, to be created later)
- `AuctionServer.java` with:
  - NAME and QUIT handlers (complete examples)
  - JOIN, BID, LEADERBOARD handlers (TODO comments)
  - Helper methods (complete)
  - Thread pool setup (STUDENTS IMPLEMENT)

## Running the Solution

### Build
```bash
cd activity2-alternative
gradle build
```

### Run Server
```bash
# Normal mode (random AI)
gradle runServer

# Grading mode (deterministic AI)
gradle runServerGrading

# Custom port
gradle runServer --args="9000"

# Grading mode with custom port
gradle runServer --args="9000 --grading"
```

### Run Client
```bash
# Connect to localhost:8889
gradle runClient

# Connect to custom host/port
gradle runClient -Phost=localhost -Pport=9000
```

## Testing

### Single Player Test
1. Start server: `gradle runServer`
2. Start client: `gradle runClient`
3. Set name: "Alice"
4. Join game
5. Bid on all items
6. See final score and leaderboard position

### Multiple Concurrent Players Test
1. Start server: `gradle runServer`
2. Start 3 clients in separate terminals
3. Each sets unique name and joins
4. All play independent games simultaneously
5. Check leaderboard to see all scores

### Leaderboard Persistence Test
1. Play a game and note your score
2. Stop server (Ctrl+C)
3. Restart server
4. Query leaderboard - your score should still be there

## Protocol Details

See `PROTO_PROTOCOL_V2.md` for complete protocol specification.

### Request Types
- NAME - Set player name
- JOIN - Start game against AI
- BID - Submit bid on current item
- LEADERBOARD - Query top scores
- QUIT - Disconnect

### Response Types
- WELCOME - Greeting
- GAME_JOINED - Game started with first item
- BID_RESULT - Auction complete, show winner
- GAME_OVER - Final scores + leaderboard rank
- LEADERBOARD_RESPONSE - Top 10 scores
- ERROR - Validation error
- BYE - Disconnect acknowledged

## AI Opponent Behavior

AI opponents use a simple bidding strategy:
- Bid 40-70% of average item value
- Occasionally bid 0 (10% chance in normal mode)
- Never bid more than current gold
- Deterministic in grading mode

Students do NOT need to understand or modify AI logic.

## Leaderboard Implementation

The leaderboard is thread-safe and persistent:

```java
class LeaderboardManager {
    private List<ScoreEntry> scores;  // synchronized list

    public synchronized int addScore(String name, int score) {
        // Add score, sort, persist to file
        // Return rank (1-based)
    }

    public synchronized List<LeaderboardEntry> getTopScores(int n) {
        // Return top n scores
    }
}
```

Students study this as an example of thread safety.

## Pedagogical Benefits

### ✅ Teaches
- Thread pools (ExecutorService)
- Independent concurrent clients
- Basic synchronization (leaderboard only)
- Protocol Buffers
- Client-server architecture
- Game logic implementation

### ✅ Avoids
- Barrier synchronization (too complex for first assignment)
- Broadcasting between threads
- Complex state coordination
- Waiting for multiple players

### ✅ Prepares for Assignment 5
- Assignment 5 can introduce real multi-player with consensus
- Students already understand basics (threads, protocols)
- Can focus on new concepts (coordination, consensus)

## Point Distribution (Activity 2: 60 points)

**Implementation (50 points)**:
- Thread Pool Setup: 5 pts (students implement ExecutorService)
- JOIN Handler: 10 pts (create game with AI opponents)
- BID Handler: 20 pts (process bids, determine winner, game logic)
- Leaderboard: 10 pts (integrate LeaderboardManager correctly)
- Protocol Compliance: 5 pts (follow PROTO_PROTOCOL_V2.md)

**Deliverables (10 points)**:
- README: 2 pts
- Video: 4 pts
- AWS: 2 pts
- Peer Testing: 2 pts

## Example Game Session

```
Server: Welcome to the Auction Game! Please set your name.
Client: Alice
Server: Welcome, Alice! You have 100 gold. Type 'join' to start playing!

Client: join
Server: Game started! You're playing against Bot-Charlie and Bot-Dana.
        Item #1: Sword of Flames (weapon, 30-50 gold)
Client: 40

Server: Auction complete!
        Winner: Alice (bid: 40)
        All Bids: Alice=40, Bot-Charlie=35, Bot-Dana=32
        Actual Value: 42 gold
        Item #2: Magic Shield (armor, 25-45 gold)
Client: 30

... continues for all items ...

Server: Game over! Final results:
        Alice: 125 total (30 gold + 95 items)
        Bot-Charlie: 110 total (40 gold + 70 items)
        Bot-Dana: 105 total (55 gold + 50 items)
        Winner: Alice
        Your Leaderboard Position: #3
```

## Next Steps

When ready to create student version:
1. Copy this directory to `activity2/`
2. Edit `AuctionServer.java`:
   - Keep NAME, QUIT handlers (as examples)
   - Replace JOIN, BID, LEADERBOARD with TODO comments
   - Remove thread pool setup (students implement)
3. Test that it compiles
4. Provide PROTO_PROTOCOL_V2.md as reference

## Comparison with Original

| Feature | Original (Multi-Player) | Alternative (vs AI) |
|---------|------------------------|---------------------|
| Threading Complexity | High (barriers, broadcasting) | Low (independent threads) |
| Synchronization | Multiple points | One point (leaderboard) |
| Player Waiting | Yes (barrier) | No (immediate start) |
| Broadcasting | Yes (complex) | No |
| Testing Difficulty | Hard (need multiple clients) | Easy (single client works) |
| Learning Curve | Steep | Gentle |
| Fun Factor | High (PvP) | Medium (PvE) |
| Sets up Assignment 5 | Partially | Well |

The alternative version is **much more appropriate for first threading assignment** while still being engaging and educational.
