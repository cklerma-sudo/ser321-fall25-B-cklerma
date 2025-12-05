# GRPC Services and Registry

### Link to Video: 

## Run things locally without registry

First Terminal

    gradle runNode

Second Terminal

    gradle runClient

## Run things locally with registry

First terminal

    gradle runRegistryServer

Second terminal

    gradle runNode -PregOn=true 

Third Terminal

    gradle runClient -PregOn=true

### gradle runRegistryServer
Will run the Registry node on localhost (arguments are possible see gradle). This node will run and allows nodes to register themselves. 

The Server allows Protobuf, JSON and gRPC. We will only be using gRPC

### gradle runNode
Will run a node with services. This includes an echo, joke, library, converter, and triangle service.

For the Library service: A books.txt file is provided with initial book data formatted as a json.

The node registers itself on the Registry. You can change the host and port the node runs on and this will register accordingly with the Registry

### gradle runClient
Will run a client which will call the services from the node, it talks to the node directly not through the registry. At the end the client does some calls to the Registry to pull the services, this will be needed later.

### gradle runDiscovery
Will create a couple of threads with each running a node with services in JSON and Protobuf. This is just an example and not needed for assignment 6. 

### gradle testProtobufRegistration
Registers the protobuf nodes from runDiscovery and do some calls. 

### gradle testJSONRegistration
Registers the json nodes from runDiscovery and do some calls. 

### gradle test
Runs the test cases.

IMPORTANT: Tests expect the server to be running first!
First run in one terminal:
    gradle runNode
Then in second terminal:
    gradle test

## List of Requirements

[x] Server can run with gradle runNode using default settings. 

[x] Client can run with gradle runClient and connects using default settings.

[x] Converter service implemented according to .proto and fully functional.

[x] Library service implemented according to .proto and fully functional.

[x] Client provides a clear menu of services and prompts user for needed inputs.

[x] Client handles all invalid input or server issues without crashing.

[x] Library data persists across server restarts (load on startup, save on change).

[x] Unit tests added for all converter and library RPCs (happy paths + error cases).

[x] Unit tests verify library persistence after server restart.

[x] Create custom .proto defining a new service meeting assignment rules.

[x] Server implements all RPCs for custom service.

[x] Client adds menu option and allows user to interact with custom service.

[x] Server and client remain robust and error-tolerant.

[x] All functionality demonstrated clearly in README and screencast.

## Program Description and how to work with it

This program offers a server that offers a variey of services and a client that allows the user to make requests in a easily understandable manner. The services that the program offers is an echo, joke, library, converter, and triangle generator service. It is able to run using the gradle commands listed above which fufills the first few requirements. From starting the client it will automatically connect to the server and then a numbered menu with all the serivces will display and then the user can select which they wish to use by entering the number. The starter code already implemented the echo and joke service and the echo simply prints what you send and the jokes will tell you a number of jokes

The converter service is implemented and fully functionally sasitifing that requirement. The converter serivce takes an amount and two units and converts from one and the other. The client menu walks through what you need each step of the way and what units you can convert. The server is prepared to handle faulty requests like converting from two incompatible units making progress in that reqiurement.

The library service has its own menu when it is selected. You can see all the books in the database, search for a book by title or author name, borrow a book, or return a borrowed book. On the client menu it walks you through what inputs you need each step of the way. There is a month long rental policy on each book hardcoded in. Like the rest the server is fully prepared to handle erronous requests and will not crash if it gets something that is not expected. This fufills the requirements lined out by the service and making the server robust.

The triangle service can calculate how many characters that a right triangle would take to make or print a triangle in four different styles: right, left, or center aligned, and hollow ceneter aligned. As with the rest the client is user friendly and gives you what you need to input each step of the way. The triangle can be built from any character and any given amount of rows (that makes sense like no negatives). The service takes two requests, is more complex than a simply add, returns two different kind of data in the responses, and each request needs at least one input. This service also like the rest is robust, thus fully meets the requirements.

Finally, in the event of a node crash the next request a client tries to send will cause the client to see the node is dead and will gracefully shut down and notify the user what happened. If any other clarification is needed the video fully illustats how the program works. This fufills the final requrement.
