# Assignment 3 Task 2: Hangman Game Protocol

**Author:** Christopher Lerma
**Date:** 11/6

---

## How to Run
You can use Gradle to run things, running with ./gradlew is of course also an option
**Server:**
Default
```bash
gradle Server
```

With arguments
```bash
gradle Server -Pport=8888
```

**Client:**
Default
```bash
gradle Client --console=plain -q
```

With arguments
```bash
gradle Client -Phost=localhost -Pport=8888
```

---

## Video Demonstration

**Link:** [Insert link to your 4-7 minute video demonstration here]

The video demonstrates:
- Starting server and client
- Complete game playthrough
- All implemented features

---

## Implemented Features Checklist

### Core Features (Required)
- [x] Set Player Name (provided as example)
- [ ] Start New Game
- [ ] Guess Letter
- [ ] Game State
- [ ] Win/Lose Detection
- [x] Graceful Quit

### Medium Features (Enhanced Gameplay)
- [ ] Difficulty Selection
- [ ] Word Guessing
- [ ] Guessed Letters Command

### Advanced Features (Competition)
- [ ] Scoring System
- [ ] Leaderboard

**Note:** Mark [x] for completed features, [ ] for not implemented.

---

## Protocol Specification

### Overview
The idea of the protocols is to have the user send data that is stored in the server and then the server sends the updated game state back to the client.

---

### 1. Set Player Name

**Request:**
```json
{
    "type": "name",
    "name": "<string>"
}
```

**Success Response:**
```json
{
    "type": "name",
    "ok": true,
    "message": "Welcome <name>! ..."
}
```

**Error Response:**
```json
{
    "ok": false,
    "message": "Name cannot be empty"
}
```

---

### 2. Start New Game

**Request:**
```json
{
    "type": "start",
    "difficulty": "<String>" -- selection from easy, medium, or hard
}
```

**Success Response:**
```json
{
    "type": "start",
    "difficulty": "<String>", -- echoes the user selection easy, medium, or hard
    "ok": true,
    "hangStage": <String>, -- sends the current stage of hangman
    "word": "<String>", --Shows the state of the word, '_' will show blank spaces
    "length": <Integer> -- Shows length of word
}
```

**Error Response(s):**
```json
{
    "ok": false,
    "message": "Difficulty must be easy, medium, or hard"
}
```
---

### 3. View Leaderboard

**Request:**
```json
{
    "type": "leaderboard",
}
```

**Success Response:**
```json
{
    "type": "leaderboard",
    "ok": true,
    "leaderboard": ["<String>", "..."], --Array containing names of players on the leaderboard, their score, and their difficulty
    "count": <Integer> --Number that shows how many players are on the leaderboard
}
```

---

### 4. Quit

**Request:**
```json
{
    "type": "quit"
}
```

**Success Response:**
```json
{
    "type": "quit",
    "ok": true,
    "message": "Thanks for playing!"
}
```
---

### 5. Guess a Letter

**Request:**
```json
{
    "type": "guessLetter",
    "letter": "<String>" -- The letter that the user guesses
}
```

**Success Response:**
```json
{
    "type": "guessLetter",
    "ok": true,
    "hangStage": <String>, -- Sends the stage of hangman based on guess
    "word": "<String>", --Shows the state of the word, '_' will show blank spaces
    "win":<Boolean>, -- Tells if the player has won
    "loss":<Boolean>, --Tells if player has loss
    "guess": "<String>" --Tells if the guess was correct or incorrect
    "score": "<Integer>" --Tells the player what is their current score
}
```

**Error Responses:**
```json
{
    "ok": false,
    "message": "Guess must be a letter such as 'a'"
}
```
```json
{
    "ok": false,
    "message": "Guess cannot be empty"
}
```
```json
{
    "ok": false,
    "message": "You already guessed this letter"
}
```
---

### 6. Guess a Word

**Request:**
```json
{
    "type": "guessWord",
    "letter": "<String>" -- The word that the user guesses
}
```

**Success Response:**
```json
{
    "type": "guessWord",
    "ok": true,
    "hangStage": "STAGE<Integer>", -- Shows the stage of hangman based on guess
    "word": "<String>", --Shows the state of the word, '_' will show blank spaces
    "win":<Boolean>, -- Tells if the player has won
    "loss":<Boolean>, --Tells if player has loss
    "guess": "<String>" --Tells if the guess was correct or incorrect
    "score": "<Integer>" --Tells the player what is their current score
}
```

**Error Responses:**
```json
{
    "ok": false,
    "message": "Guess must be a word with only letters such as 'rotten'"
}
```
```json
{
    "ok": false,
    "message": "Guess cannot be empty"
}
```
```json
{
    "ok": false,
    "message": "Guess was not the correct amount of letters"
}
```
---
### 7. Show Game State

**Request:**
```json
{
    "type": "state"
}
```

**Success Response:**
```json
{
    "type": "state",
    "ok": true,
    "hangStage": "STAGE<Integer>", -- Shows the stage of hangman based on guess
    "word": "<String>", --Shows the state of the word, '_' will show blank spaces
    "score": "<Integer>", --Tells the player what is their current score
    "guesses": "<String>", --Shows the guesses remaining as a fraction
    "lettersCorrect": "<Integer>" --Shows the amount of letters correctly guessed
}
```
---
### 8. Show Letters Guessed

**Request:**
```json
{
    "type": "letters"
}
```

**Success Response:**
```json
{
    "type": "letters",
    "ok": true,
    "lettersGuessed": ["<String>, "..."] --Shows the letters that are guessed
    "total": "<Number>" --Shows the total
}
```
---
### 9. Return the Menu

**Request:**
```json
{
    "type": "return"
}
```

**Success Response:**
```json
{
    "type": "return",
    "ok": true,
    "message": "Success, going back to main menu"
}
```
---

## Error Handling Strategy

[Explain your approach to error handling:] I plan on guessing what faulty inputs can be used and being prepared to handle those inputs. Then my code will be built not to crash and alert the user of what went wrong.

**Server-side validation:**
- [What validations does your server perform?]
  It will validate inputs from the user such as their name and the letters they guess and then ensure that they are valid.

- [How do you handle missing fields?]
  I will either set to a default value if possible, or continue to prompt the user until something is entered.

- [How do you handle invalid data types?]
  Avoid a crash and alert the user of the mistake then continue to prompt them.

- [How do you handle game state errors?]
  Avoid a crash, alert the user that something went wrong, and revert to the last stable state. Worse case senario, the game restarts.
---

## Robustness

[Explain how you ensured robustness:] I will use equivalence partitioning on test cases to find where the code is error prone. This rigorous testing will ensure that the code works in the vast majority of cirmcumstances.

**Server robustness:**
- [How does server handle invalid input without crashing?]
- The server will be designed to expect faulty input and navigate around it either through try and catch blocks or if statements.


**Client robustness:**
- [How does client handle unexpected responses?]
- There will be a default error that the client can always revert to if the erronous response is not expected. Then the client can attempt to reconnect if needed.

- [What happens if server is unavailable?]
- The user will be notfied and the client will continue to attempt a connection until it succeeds or the user elects to quit trying.

---

## Assumptions (if applicable)

[List any assumptions you made about the protocol or game rules]

1. [Assumption 1]
2. [Assumption 2]
3. [etc.]

---

## Known Issues

[List any known bugs or limitations]

1. [Issue 1]
2. [Issue 2]

---

