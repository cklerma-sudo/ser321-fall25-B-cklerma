# Task 1.2: Mystery Service Discovery and Protocol Documentation

**Your Name:**
**How I tested:** [Unit Tests / Extended Client / Both]

---

## Part 1: Discovery Log

Document at least 8 test attempts showing your systematic investigation.

### Attempt 1
**Request Sent:**
```json
{
"type": "stats"
}
```

**Response Received:**
```json
{
"ok":false
"message":"Field 'operation' does not exist in request"
}
```

**What I Learned:**
That an operation field is required

---

### Attempt 2
**Request Sent:**
```json
{
"type": "stats"
"operation" : "test"
}
```

**Response Received:**
```json
{
"ok":false
"message":"Field 'numbers' does not exist in request"
}
```

**What I Learned:**
That a numbers field is required.

---

### Attempt 3
**Request Sent:**
```json
{
"type": "stats"
"operation" : "test"
"numbers" : "1"
}
```

**Response Received:**
```json
Server Crash with exception that numbers is not a json array
```

**What I Learned:**
That numbers needs to be a json array.

---
### Attempt 4
**Request Sent:**
```json
{
"type": "stats"
"operation" : "test"
"numbers" : [1,2]
}
```

**Response Received:**
```json
{
"ok":false
"message":"Operation 'test' not supported. Valid operations: mean, sum, min, max, greaterThan, contains, help"
}
```

**What I Learned:**
Operation selection can be mean, sum, min, max, greaterThan, contains, help

---
### Attempt 5
**Request Sent:**
```json
{
"type": "stats"
}
```

**Response Received:**
```json
{
"ok":false
"message":"Field 'operation' does not exist in request"
}
```

**What I Learned:**


---
### Attempt 6
**Request Sent:**
```json
{
"type": "stats"
}
```

**Response Received:**
```json
{
"ok":false
"message":"Field 'operation' does not exist in request"
}
```

**What I Learned:**


---
### Attempt 7
**Request Sent:**
```json
{
"type": "stats"
}
```

**Response Received:**
```json
{
"ok":false
"message":"Field 'operation' does not exist in request"
}
```

**What I Learned:**


---
### Attempt 8
**Request Sent:**
```json
{
"type": "stats"
}
```

**Response Received:**
```json
{
"ok":false
"message":"Field 'operation' does not exist in request"
}
```

**What I Learned:**


---


---

## Part 2: Complete Protocol Specification

Follow the same format as Task 1.1 README protocols.

### [Service Name]

[Brief description of what the service does]

#### [Operation Name]

**Request:**
```json
{

}
```

**Success Response:**
```json
{

}
```

**Error Responses:**

```json
{

}
```

---

[Document ALL operations you discovered]

---

## Part 3: Summary

**Total Operations Discovered:**
**How I approached discovery:**
**Most challenging part:**
