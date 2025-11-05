# StringConcatenation Debugging Exercise - Instructor Reference

## Overview
The stringconcatenation service is implemented in both client and server, but has **4 bugs** that prevent it from working correctly according to the protocol specification.

The Correct Protocolis in the README.md

---

## The 4 Bugs

### Bug #1:  string_1
**Location:** `SockClient`, line 71

**The Problem:**
```"string_1" was the key used in the json object for the first string when the server expected to be "string1"
```

**The Fix:**
```Change the key in the client to "string1"
```

**Why it matters:** 
This is so that the server can actually find what the first string is.

**How did you find this:**
Just used a basic two inputs "race" and "car" to see that the first string was not being found.

### Bug #2:  <NAME>
**Location:** `Filename`, line xxx

**The Problem:**
```Describe
```

**The Fix:**
```Solution
```

**Why it matters:** 


**How did you find this:**

### Bug #3:  <NAME>
**Location:** `Filename`, line xxx

**The Problem:**
```Describe
```

**The Fix:**
```Solution
```

**Why it matters:** 


**How did you find this:**

### Bug #4:  <NAME>
**Location:** `Filename`, line xxx

**The Problem:**
```Describe
```

**The Fix:**
```Solution
```

**Why it matters:** 


**How did you find this:**


