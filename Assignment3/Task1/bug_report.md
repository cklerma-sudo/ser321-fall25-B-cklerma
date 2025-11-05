# StringConcatenation Debugging Exercise - Instructor Reference

## Overview
The stringconcatenation service is implemented in both client and server, but has **4 bugs** that prevent it from working correctly according to the protocol specification.

The Correct Protocolis in the README.md

---

## The 4 Bugs

### Bug #1:  string_1
**Location:** `SockClient`, line 71

**The Problem:**
"string_1" was the key used in the json object for the first string when the server expected to be "string1"


**The Fix:**
Change the key in the client to "string1"


**Why it matters:** 
This is so that the server can actually find what the first string is.

**How did you find this:**
Just used a basic two inputs "race" and "car" to see that the first string was not being found.

### Bug #2:  No "type" in server response
**Location:** `SockServer`, line 186

**The Problem:**
In the response there was not type key paired with stringconcatenation

**The Fix:**
In the JSON respone add the correct pairing

**Why it matters:** 
The client expects this is the response and a lack of it causes the client to crash

**How did you find this:**
Just used a basic two inputs "race" and "car" to see that the client program crashes and that the response is incorrectly formatted.

### Bug #3:  concat instead of result
**Location:** `SockServer`, line 185

**The Problem:**
The result is labelled as "concat" instead of "result" in the response.

**The Fix:**
Change "concat" to result.

**Why it matters:** 
The client expects the result to be labelled as result in the response and when it cannot find it, it causes an error.

**How did you find this:**
Just used a basic two inputs "race" and "car" to see that the client program has an error and seeing the format is incorect.

### Bug #4:  Result incorrectly Handled
**Location:** `SockClient`, line 94

**The Problem:**
Result in server response was not handled correctly by client so thus it caused a crash

**The Fix:**
Add an else if statement to handle the server response

**Why it matters:** 
The client need to process the request from how the server is documented to send it


**How did you find this:**
Just used a basic two inputs "race" and "car" to see that the client program has an error and seeing that the error was an integer expected but a string was given.


