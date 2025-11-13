# Activity 2: Auction Game Protocol Specification (vs Computer)

This document specifies the **exact** protocol for the Auction Game where players compete against AI opponents. All server implementations must follow this protocol to ensure compatibility.

## Overview

The protocol uses Protocol Buffers (protobuf) with delimited messages:
- Client sends `Request` messages using `request.writeDelimitedTo(out)`
- Server sends `Response` messages using `response.writeDelimitedTo(out)`
- Read messages using `Request.parseDelimitedFrom(in)` and `Response.parseDelimitedFrom(in)`


## Protocol Flow

```
1. Connection → Server sends WELCOME
2. Client → NAME → Server → WELCOME (with gold amount)
3. Client → JOIN → Server → GAME_JOINED (with first item + player gold)
4. Loop for items 1-4:
   Client → BID → Server → BID_RESULT (with next item + updated player gold)
5. Last item (item 5):
   Client → BID
   Server → BID_RESULT (no next_item field)
   Server → GAME_OVER (with final scores) [sent automatically, no client request]
6. From menu: Client → LEADERBOARD → Server → LEADERBOARD_RESPONSE
7. Client → QUIT → Server → BYE
```

**Key Protocol Feature**: Server sends `player_stats` with current gold in `GAME_JOINED` and `BID_RESULT` responses. Client displays this as "Enter your bid (0-X):" where X is the gold amount from `player_stats`.

---

## Protocol Endpoints

Each endpoint shows the Request/Response pair together.

---

### 1. Initial Connection

**Description**: Server sends initial welcome when client connects.

**Client Action**: Connect to server (no request message)

**Server Response**:
```protobuf
Response {
  type: WELCOME
  ok: true
  message: "Welcome to the Auction Game! Please set your name."
}
```

**Java Example**:
```java
Response.newBuilder()
    .setType(Response.ResponseType.WELCOME)
    .setOk(true)
    .setMessage("Welcome to the Auction Game! Please set your name.")
    .build();
```

---

### 2. Set Name (NAME)

**Description**: Player sets their name. Name must be unique among **active connections**.

**Thread Safety Requirement**: Server must track active player names in a thread-safe collection (e.g., `Collections.synchronizedSet(new HashSet<>())`) and **remove the name when client disconnects** (in finally block).

**Client Request**:
```protobuf
Request {
  type: NAME
  name: "Alice"          // Required: player's chosen name
}
```

**Success Response**:
```protobuf
Response {
  type: WELCOME
  ok: true
  message: "Welcome, Alice! You have 200 gold. Type 'join' to start playing against AI opponents!"
}
```

**Error Responses**:
```protobuf
// Empty name
Response {
  type: ERROR
  ok: false
  message: "Name cannot be empty"
}

// Name taken
Response {
  type: ERROR
  ok: false
  message: "Name already taken. Please choose another."
}
```

---

### 3. Join Game (JOIN)

**Description**: Start a new game against 2 AI opponents. Game starts immediately with first item. Response includes `player_stats` with current gold.

**Client Request**:
```protobuf
Request {
  type: JOIN
}
```

**Success Response**:
```protobuf
Response {
  type: GAME_JOINED
  ok: true
  message: "Game started! You're playing against Bot-Charlie and Bot-Dana. Current item:"
  player_stats: PlayerStats {
    gold_remaining: 100     // Player's current gold (starting amount)
  }
  next_item: AuctionItem {  // First auction item (randomly selected)
    id: 1
    name: "Sword of Flames"
    category: "weapon"
    min_value: 30
    max_value: 50
  }
}
```

**Error Responses**:
```protobuf
// Not named yet
Response {
  type: ERROR
  ok: false
  message: "Please set your name first"
}

// Already in game (player calls JOIN twice)
Response {
  type: ERROR
  ok: false
  message: "You are already in a game"
}
```

**Client Display**: After receiving GAME_JOINED, client shows item details and prompts:
```
Enter your bid (0-200):
```

---

### 4. Place Bid (BID)

**Description**: Place a bid on current item. Server processes with AI bids, determines winner, returns result. Response includes updated `player_stats` with current gold and next item (if not last).

**Client Request**:
```protobuf
Request {
  type: BID
  item_id: 1             // Required: ID of current item
  bid_amount: 40         // Required: gold to bid (0 to current gold)
}
```

**Success Response** (more items remain):
```protobuf
Response {
  type: BID_RESULT
  ok: true
  message: "Auction complete!"
  result: AuctionResult {
    item: AuctionItem { id: 1, name: "Sword of Flames", ... }
    actual_value: 42              // Item's actual value (revealed)
    winner_name: "Alice"
    winning_bid: 40
    all_bids: [                   // All bids (player + AI)
      PlayerBid { player_name: "Alice", bid_amount: 40 },
      PlayerBid { player_name: "Bot-Charlie", bid_amount: 35 },
      PlayerBid { player_name: "Bot-Dana", bid_amount: 32 }
    ]
  }
  player_stats: PlayerStats {
    gold_remaining: 60           // Player's gold AFTER this auction
  }
  next_item: AuctionItem {        // Next item to bid on
    id: 2
    name: "Magic Shield"
    category: "armor"
    min_value: 25
    max_value: 45
  }
}
```

**Success Response** (last item - no next_item, followed by GAME_OVER):
```protobuf
Response {
  type: BID_RESULT
  ok: true
  message: "Auction complete!"
  result: AuctionResult { /* same as above */ }
  player_stats: PlayerStats { gold_remaining: 60 }
  // NO next_item field - indicates last item
}

// Server immediately sends GAME_OVER (see endpoint #5) without another request
```

**Error Responses** - Player can retry after error:
```protobuf
// Wrong item ID
Response {
  type: ERROR
  ok: false
  message: "Invalid item ID. Current item is #2"
}

// Insufficient gold
Response {
  type: ERROR
  ok: false
  message: "Insufficient gold. You have 30 gold."
}

// Negative bid
Response {
  type: ERROR
  ok: false
  message: "Bid cannot be negative"
}

// Not in game
Response {
  type: ERROR
  ok: false
  message: "You must join a game first"
}
```

**IMPORTANT - Invalid Bid Retry Flow**:
When server sends ERROR response, the current item **remains the same**. Client can submit another BID request with corrected values:

```
Server → GAME_JOINED (item id=1)
Client → BID: item_id=1, amount=200  (invalid - too much gold)
Server → ERROR: "Insufficient gold. You have 100 gold."
         (Item #1 is still current, player can retry)
Client → BID: item_id=1, amount=50   (valid)
Server → BID_RESULT: (auction complete, move to item #2)
```

**Client Display**: After receiving BID_RESULT with next item:
```
Auction complete!
Winner: Alice (bid: 40 gold)
Actual Value: 42 gold

All Bids:
  Alice: 40 gold
  Bot-Charlie: 35 gold
  Bot-Dana: 32 gold

=== Item #2 ===
Name: Magic Shield
Category: armor
Value Range: 25-45 gold

Enter your bid (0-60):
```
Note: Client uses `player_stats.gold_remaining` (60) to show correct bid range.

---

### 5. Game Over (GAME_OVER)

**Description**: Sent automatically after last auction. Shows final scores for all players, game winner, and player's leaderboard position.

**Client Request**: None (server sends automatically after last BID)

**Server Response**:
```protobuf
Response {
  type: GAME_OVER
  ok: true
  message: "Game over! Final results:"
  game_result: GameResult {
    player_scores: [
      PlayerStats {
        player_name: "Alice"
        gold_remaining: 30
        items_value: 95
        total_score: 125          // gold + items_value
        items_won: ["Sword of Flames", "Magic Shield", "Fire Scroll"]
      },
      PlayerStats {
        player_name: "Bot-Charlie"
        gold_remaining: 40
        items_value: 70
        total_score: 110
        items_won: ["Healing Potion", "Ice Dagger"]
      },
      PlayerStats {
        player_name: "Bot-Dana"
        gold_remaining: 55
        items_value: 50
        total_score: 105
        items_won: ["Lightning Staff"]
      }
    ]
    winner_name: "Alice"          // Game winner (highest score)
    leaderboard_position: 3       // Player's global leaderboard rank
  }
}
```

---

### 6. View Leaderboard (LEADERBOARD)

**Description**: Query top 10 scores from global leaderboard. Can be called anytime.

**Client Request**:
```protobuf
Request {
  type: LEADERBOARD
}
```

**Server Response**:
```protobuf
Response {
  type: LEADERBOARD_RESPONSE
  ok: true
  message: "Top 10 Scores:"
  leaderboard: Leaderboard {
    entries: [
      LeaderboardEntry {
        rank: 1
        player_name: "Charlie"
        score: 250
        timestamp: "2025-10-31T10:30:00"
      },
      LeaderboardEntry {
        rank: 2
        player_name: "Alice"
        score: 225
        timestamp: "2025-10-31T09:15:00"
      }
      // ... up to 10 entries ...
    ]
  }
}
```

---

### 7. Quit (QUIT)

**Description**: Disconnect from server gracefully.

**Client Request**:
```protobuf
Request {
  type: QUIT
}
```

**Server Response**:
```protobuf
Response {
  type: BYE
  ok: true
  message: "Thanks for playing! Final score: 225. Goodbye!"
}
```

---

**Usage Notes**:
- In `GAME_JOINED` and `BID_RESULT` responses: Only `gold_remaining` is set (other fields unused)
- In `GAME_OVER` response: All fields are populated with final stats

---

## Example Game Flow

```
1. Alice connects
   Server → WELCOME: "Welcome to the Auction Game! Please set your name."

2. Alice → NAME: "Alice"
   Server → WELCOME: "Welcome, Alice! You have 200 gold. Type 'join' to start..."

3. Alice → JOIN
   Server creates: Alice vs Bot-Charlie vs Bot-Dana
   Server randomly selects 5 items from pool
   Server → GAME_JOINED:
     - First item: Sword of Flames (30-50)
     - player_stats: {gold_remaining: 200}
   Client shows: "Enter your bid (0-100):"

4. Alice → BID: item_id=1, amount=40
   Server generates: Bot-Charlie=35, Bot-Dana=32
   Alice wins! (spent 40 gold)
   Server → BID_RESULT:
     - Winner: Alice (40)
     - Bids: Alice=40, Bot-Charlie=35, Bot-Dana=32
     - Actual value: 42
     - player_stats: {gold_remaining: 60}
     - Next item: Magic Shield (25-45)
   Client shows: "Enter your bid (0-60):"

5. ... continues for all 5 randomly selected items ...

6. After last item (item 5):
   Server → BID_RESULT: (result for item 5, no next item)
   Server → GAME_OVER:
     - Alice: gold=30, items_value=95, total=125
     - Bot-Charlie: gold=40, items_value=70, total=110
     - Bot-Dana: gold=55, items_value=50, total=105
     - Winner: Alice
     - Leaderboard position: #3

7. Alice → LEADERBOARD
   Server → LEADERBOARD_RESPONSE: Top 10 scores

8. Alice → QUIT
   Server → BYE: "Thanks for playing! Final score: 125. Goodbye!"
```


## Protocol Compliance

Your server is compliant if:
1. Accepts all request types
2. Sends responses in exact formats
3. Error messages match standard messages
4. Leaderboard persists correctly
5. AI opponents bid correctly
6. Provided client can play successfully
