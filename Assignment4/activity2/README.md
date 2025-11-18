# Required Information

### Link for Video: 

## How to Compile and Run Server
To Start and compile the Server use the command "gradle runServer" and that will start the server. For the client it is "gradle runClient". If you wish to use...//TODO finsh specs

## Implemented Checklist
[x] Make it multithreaded  
[x] JOIN request  
[x] BID request  
[x] GAME_OVER request  
[x] LEADERBOARD request  
[x] Make Everything Thread Safe  

## Design Decisions and Challenges
The Protocol Buffer method of serialization naturally aligns with the builder design pattern, which I used throughout all request and response objects. I also decided to break each response into its own helper method so the code stays modular, easier to extend, and much simpler to debug or test.

The biggest challenge I ran into was the syntax of Protocol Buffers themselves. This was a completely new format for me, so I spent a good amount of time fixing syntax-related errors while testing and getting familiar with how everything fits together.

Overall, these were the key design decisions I made and the main challenges I worked through while writing this code.

## Known Issues
-Certain characters are not recognized by a windows command prompt and as a result appear as '?' in the console.
