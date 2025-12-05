# GRPC Services and Registry

### Link to Video: https://youtu.be/609luHZM7x0

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

This program implements a server that provides several RPC-based services along with a client that allows users to interact with them through a simple and intuitive terminal interface. The available services are Echo, Joke, Converter, Library, and Triangle Generator. The server is started using the command gradle runNode, and the client is launched with gradle runClient. When the client begins, it automatically connects to the server using the default host and port. After connecting, the client displays a numbered menu that lists every available service. The user can choose a service by entering its number, and the program walks the user through any required inputs in a clear and easy way. This satisfies the initial execution and usability requirements for the assignment.

The Echo and Joke services were provided in the starter code and remain fully functional. The Echo service simply returns whatever message the user sends. The Joke service responds with a user-selected number of jokes. The Converter service is fully implemented and allows the user to convert an amount between two units. The client clearly lists the available units and prompts for the necessary input. The server performs the conversion and handles invalid or incompatible requests in a safe way, so it will not crash if the user enters something unexpected.

The Library service contains more complex and stateful behavior. When the user selects it, the client opens a submenu that allows them to view all books, search by title or author, borrow a book, or return a book. A one-month rental period is built into the service. The client prompts for any inputs needed at each step. The server responds with clear results and handles invalid operations safely, such as trying to borrow a book that does not exist or returning a book that is not currently checked out. The service also fulfills the persistence requirement because the server loads the library data from a JSON file on startup and saves changes whenever books are borrowed or returned. This ensures that the data remains available after the server restarts.

The Triangle Generator service fulfills the custom service requirement for the assignment. It supports two different RPC requests. One request calculates the number of characters needed to draw a right triangle of a given size. The other request generates triangle text in four different styles, which are right aligned, left aligned, center aligned, and hollow center aligned. The user supplies the number of rows and the character used for drawing, and the client guides them through the process. The server returns different data depending on the request. All invalid inputs, such as negative row values, are handled properly so the service is robust and stable.

Across all services, the client and server are designed to handle errors gracefully. If the server stops unexpectedly, the next request from the client detects the failure and the client exits cleanly with a helpful message. This prevents crashes and stack traces, and it fulfills the robustness requirement. Overall, this project provides a fully functional server and client system that meets all assignment expectations related to service implementation, input handling, persistence, custom service behavior, and reliability. The accompanying video demonstrates the program in action and clarifies how users can interact with each feature.
