# Peer-to-Peer Chat System

### Link to Video: https://youtu.be/UXJpPCUKTaM

## How to Run
You can use the gradle command "runPeer --args "[Name] [Port Number]" in order to begin the peer to peer network. After that to add more peers in the network you must use the same command and append a port number that is already in the network. For example, "gradle runPeer --args "Bob 8001 8000" would create a user named bob that is at port 8001 and will attempt to connect to the whole network via the bootstrap peer at port 8000. Everything will be run on localhost.

## Algorithm Explanation
The approach I used was thinking about in the way we introduce new friends into our friend group. Each peer in the network keeps a list of every other peer's port and knows who is in the network, then when a new computer wishes to connect it "introduces" itself and then learns about all the other computers from the list. In the actual code this is done by a basic text protocol. The bootstrap peer has the list of all other peers in the network, so the new peer sends "HELLO " with its port information to let everyone in the network know that it is connecting. 
