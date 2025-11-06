# Task 1.2: Mystery Service Discovery and Protocol Documentation

**Your Name:** Christopher Lerma
**How I tested:** Extended Client

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
"operation" : "help"
"numbers" : [1,2]
}
```

**Response Received:**
```json
{
"operations":["mean","sum","min","max","greaterThan","contains","help"]
"type":"stats"
"ok":true
}
```

**What I Learned:**
That help just tells me the operations but does not tell me what they do.

---
### Attempt 6
**Request Sent:**
```json
{
"type": "stats"
"operation" : "mean"
"numbers" : [1,2]
}
```

**Response Received:**
```json
{
"result":1.5
"count":2
"type":"stats"
"ok":true
"operation":"mean"
}
```

**What I Learned:**
That mean gives the mean as an float point not an int and also it keeps a count.

---
### Attempt 7
**Request Sent:**
```json
{
"type": "stats"
"operation" : "greaterThan"
"numbers" : [1,2,3]
}
```

**Response Received:**
```json
{
"ok":false
"message":"Field 'threshold' does not exist in request"
}
```

**What I Learned:**
Greater than needs a threshold that I assume will show all the numbers greaters than that.

---
### Attempt 8
**Request Sent:**
```json
{
"type": "stats"
"operation" : "sum"
"numbers" : [1]
}
```

**Response Received:**
```json
{
"result":1
"count":1
"type":"stats"
"ok":true
"operation":"sum"
}
```

**What I Learned:**
This handles a collection of just one number just fine.

---


---

## Part 2: Complete Protocol Specification

Follow the same format as Task 1.1 README protocols.

### Stats

Does a variety of stastical operations on a collection of numbers.

#### Mean

**Request:**
```json
{
"type": "stats"
"operation" : "mean"
"numbers" : [<Nummber>, ...] -- JSONArray of numbers that the calculations are done on
}
```

**Success Response:**
```json
{
"result": <Number> -- Shows the mean of the array of numbers
"count":<Integer> -- Number of numbers in recieved array
"type":"stats"
"ok":true
"operation": "mean"
}
```

**Error Responses:**

```json
{
"ok":false
"message":<String> -- Response on what went wrong
}
```

---

#### Sum

**Request:**
```json
{
"type": "stats"
"operation" : "sum"
"numbers" : [<Nummber>, ...] -- JSONArray of numbers that the calculations are done on
}
```

**Success Response:**
```json
{
"result": <Number> -- Shows the result of the summation
"count":<Integer> -- Number of numbers in recieved array
"type":"stats"
"ok":true
"operation": "sum"
}
```

**Error Responses:**

```json
{
"ok":false
"message":<String> -- Response on what went wrong
}
```

---

#### Min

**Request:**
```json
{
"type": "stats"
"operation" : "min"
"numbers" : [<Nummber>, ...] -- JSONArray of numbers that the calculations are done on
}
```

**Success Response:**
```json
{
"result": <Number> -- Shows the lowest value in the recieved array
"count":<Integer> -- Number of numbers in recieved array
"type":"stats"
"ok":true
"operation": "min"
}
```

**Error Responses:**

```json
{
"ok":false
"message":<String> -- Response on what went wrong
}
```

---
#### Max

**Request:**
```json
{
"type": "stats"
"operation" : "max"
"numbers" : [<Nummber>, ...] -- JSONArray of numbers that the calculations are done on
}
```

**Success Response:**
```json
{
"result": <Number> -- Shows the largest number in array
"count":<Integer> -- Number of numbers in recieved array
"type":"stats"
"ok":true
"operation": "max"
}
```

**Error Responses:**

```json
{
"ok":false
"message":<String> -- Response on what went wrong
}
```

---

#### greaterThan

**Request:**
```json
{
"type": "stats"
"operation" : "greaterThan"
"numbers" : [<Nummber>, ...] -- JSONArray of numbers that the calculations are done on
"threshold" : <Number> --A number that is used to check what numbers in the JSONArray are greater than it
}
```

**Success Response:**
```json
{
"result": [<Number>, ...] -- Shows all numbers greater than the threshold
"count":<Integer> -- Number of numbers in recieved array
"type":"stats"
"ok":true
"operation": "greaterThan
}
```

**Error Responses:**

```json
{
"ok":false
"message":<String> -- Response on what went wrong
}
```

---

#### contains

**Request:**
```json
{
"type": "stats"
"operation" : contains
"numbers" : [<Nummber>, ...] -- JSONArray of numbers that the calculations are done on
"value" : <Number> --A number that is used to check if the number is in the JSONArray
}
```

**Success Response:**
```json
{
"result": <String> -- true or false depending if the value number is found in the array
"count":<Integer> -- Number of numbers in recieved array
"type":"stats"
"ok":true
"operation": "contains"
}
```

**Error Responses:**

```json
{
"ok":false
"message":<String> -- Response on what went wrong
}
```

---

#### help

**Request:**
```json
{
"type": "stats"
"operation" : "help"
"numbers" : [<Nummber>, ...] -- JSONArray of numbers that the calculations are done on
}
```

**Success Response:**
```json
{
"result": [<String>, ...] -- Shows all possible operations
"count":<Integer> -- Number of numbers in recieved array
"type":"stats"
"ok":true
"operation": "help"
}
```

**Error Responses:**

```json
{
"ok":false
"message":<String> -- Response on what went wrong
}
```

---


## Part 3: Summary

**Total Operations Discovered:**
7
**How I approached discovery:**
I did trial and error responses and read the error messages to see how it worked. I kept going until I got a success reponse and then tested to see what I could do and still get a success reponse.
**Most challenging part:**
The most challenging part was figuring out what requests to send in order to test the server. Also coding those requests was challenging on the Syntax.
