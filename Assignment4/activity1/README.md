# Activity 1: Task Management System with Threading

## Overview

In this activity, you will work with a **Task Management System** that allows multiple clients to manage shared tasks. The system supports:
- Adding tasks with priority levels
- Listing tasks (all, pending, or completed)
- Marking tasks as completed
- Assigning tasks to people

You will learn about:
- **Protocol Buffers (Protobuf)** - An efficient binary serialization format
- **Multi-threading** - Handling multiple clients simultaneously
- **Thread safety** - Protecting shared data from race conditions

## Starter Code

### What's Provided

1. **Server.java** - Single-threaded server using JSON protocol
2. **Client.java** - Complete client with menu system
3. **Performer.java** - Handles all client requests using JSON
4. **Task.java** - Task data structure
5. **TaskList.java** - Thread-safe task collection 
6. **JsonUtils.java** - JSON utility methods 
7. **task.proto** - Protobuf protocol definition (STARTER - incomplete)
8. **build.gradle** - Build configuration

### Running the Starter Code

The starter code uses JSON protocol and is fully functional.

**Start the server:**
```bash
gradle runServer
# Or with custom port:
gradle runServer -Pport=9000
```

**Start the client (in a new terminal):**
```bash
gradle runClient
# Or with custom host/port:
gradle runClient -Phost=localhost -Pport=9000
```

Try the operations:
1. Add a task
2. List tasks
3. Complete a task
4. Assign a task
5. Try running multiple clients - only one works at a time (single-threaded!)

## Current Protocol (JSON)

The server currently uses a JSON-based protocol:

### Request Format
```json
{
  "type": "add|list|complete|assign|quit",
  ... other fields depending on type
}
```

### Add Task
**Request:**
```json
{
  "type": "add",
  "description": "Implement Protobuf protocol",
  "priority": "high"
}
```

**Success Response:**
```json
{
  "ok": true,
  "type": "add",
  "data": {
    "id": 1,
    "description": "Implement Protobuf protocol",
    "priority": "high",
    "assignee": "unassigned",
    "completed": false
  }
}
```

**Error Response:**
```json
{
  "ok": false,
  "type": "add",
  "data": {
    "error": "Missing 'description' field"
  }
}
```

### List Tasks
**Request:**
```json
{
  "type": "list",
  "filter": "all"
}
```
- `filter` can be: "all", "pending", or "completed" (optional, defaults to "all")

**Success Response:**
```json
{
  "ok": true,
  "type": "list",
  "data": {
    "tasks": [
      {
        "id": 1,
        "description": "Implement Protobuf protocol",
        "priority": "high",
        "assignee": "unassigned",
        "completed": false
      },
      ...
    ],
    "count": 5
  }
}
```

### Complete Task
**Request:**
```json
{
  "type": "complete",
  "id": 1
}
```

**Success Response:**
```json
{
  "ok": true,
  "type": "complete",
  "data": {
    "message": "Task #1 marked as completed"
  }
}
```

### Assign Task
**Request:**
```json
{
  "type": "assign",
  "id": 1,
  "assignee": "Alice"
}
```

**Success Response:**
```json
{
  "ok": true,
  "type": "assign",
  "data": {
    "message": "Task #1 assigned to Alice"
  }
}
```

### Quit
**Request:**
```json
{
  "type": "quit"
}
```

**Success Response:**
```json
{
  "ok": true,
  "type": "quit",
  "data": {
    "message": "Goodbye!"
  }
}
```
