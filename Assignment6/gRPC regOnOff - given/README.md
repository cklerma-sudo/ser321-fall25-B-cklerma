# GRPC Services and Registry

## Run things locally without registry
To run see also video. To run locally and without Registry which you should do for the beginning

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
Will run a node with services. This includes an echo, joke, library, converter, and triangle services.

For the Library service: A books.txt file is provided with initial book data (format: title|author|isbn, one per line). Your server should load this on first run and create library_data.json for persistence.

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
Runs the test cases. The starter code includes example tests for Joke and Echo in ServerTest.java. You need to add your own tests for Converter and Library services in the same file.

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