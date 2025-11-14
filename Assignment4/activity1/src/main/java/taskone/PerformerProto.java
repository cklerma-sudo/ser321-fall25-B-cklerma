package taskone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Performer class handles client requests using JSON protocol.
 * This version uses JSON for serialization.
 */
public class ProtoPerformer {
    private final Socket clientSocket;
    private final TaskList taskList;
    private BufferedReader in;
    private PrintWriter out;

    public Performer(Socket clientSocket, TaskList taskList) {
        this.clientSocket = clientSocket;
        this.taskList = taskList;
    }

    /**
     * Main method to process client requests.
     * Reads requests, processes them, and sends responses.
     */
    public void doPerform() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Send welcome message
            JSONObject welcome = JsonUtils.createSuccessResponse("connect", "Connected to Task Management Server");
            out.println(welcome.toString());

            // Process requests
            String request;
            while ((request = in.readLine()) != null) {
                JSONObject response = handleRequest(request);
                out.println(response.toString());

                // If quit, break the loop
                if (response.has("type") && response.getString("type").equals("quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    /**
     * Handle a single request from the client.
     * @param requestStr JSON request string
     * @return JSON response object
     */
    private JSONObject handleRequest(String requestStr) {
        try {
            JSONObject request = new JSONObject(requestStr);

            // Validate request has type field
            if (!request.has("type")) {
                return JsonUtils.createErrorResponse("unknown", "Request missing 'type' field");
            }

            String type = request.getString("type");

            // Route to appropriate handler
            switch (type) {
                case "add":
                    return handleAdd(request);
                case "list":
                    return handleList(request);
                case "complete":
                    return handleComplete(request);
                case "assign":
                    return handleAssign(request);
                case "quit":
                    return handleQuit();
                default:
                    return JsonUtils.createErrorResponse(type, "Unknown request type: " + type);
            }
        } catch (Exception e) {
            return JsonUtils.createErrorResponse("error", "Invalid JSON or request format", e.getMessage());
        }
    }

    private JSONObject handleAdd(JSONObject request) {
        // Validate required fields
        if (!request.has("description")) {
            return JsonUtils.createErrorResponse("add", "Missing 'description' field");
        }
        if (!request.has("priority")) {
            return JsonUtils.createErrorResponse("add", "Missing 'priority' field");
        }

        String description = request.getString("description");
        String priority = request.getString("priority");

        // Validate description not empty
        if (description.trim().isEmpty()) {
            return JsonUtils.createErrorResponse("add", "Description cannot be empty");
        }

        // Validate priority value
        if (!priority.equals("low") && !priority.equals("medium") && !priority.equals("high")) {
            return JsonUtils.createErrorResponse("add", "Invalid priority value. Must be 'low', 'medium', or 'high'");
        }

        // Add task
        Task task = taskList.addTask(description, priority);

        // Return success response with created task
        return JsonUtils.createSuccessResponse("add", JsonUtils.taskToJson(task));
    }


    private JSONObject handleList(JSONObject request) {
        // Get filter (defaults to "all")
        String filter = request.optString("filter", "all");

        List<Task> tasks;
        switch (filter) {
            case "all":
                tasks = taskList.getAllTasks();
                break;
            case "pending":
                tasks = taskList.getPendingTasks();
                break;
            case "completed":
                tasks = taskList.getCompletedTasks();
                break;
            default:
                return JsonUtils.createErrorResponse("list", "Invalid filter value. Must be 'all', 'pending', or 'completed'");
        }

        // Convert tasks to JSON array
        JSONArray taskArray = new JSONArray();
        for (Task task : tasks) {
            taskArray.put(JsonUtils.taskToJson(task));
        }

        // Create response data
        JSONObject data = new JSONObject();
        data.put("tasks", taskArray);
        data.put("count", tasks.size());

        return JsonUtils.createSuccessResponse("list", data);
    }


    private JSONObject handleComplete(JSONObject request) {
        // Validate required field
        if (!request.has("id")) {
            return JsonUtils.createErrorResponse("complete", "Missing 'id' field");
        }

        // Get and validate ID
        int id;
        try {
            id = request.getInt("id");
        } catch (Exception e) {
            return JsonUtils.createErrorResponse("complete", "Invalid 'id' value. Must be an integer");
        }

        // Mark task as completed
        boolean success = taskList.completeTask(id);

        if (success) {
            JSONObject data = new JSONObject();
            data.put("message", "Task #" + id + " marked as completed");
            return JsonUtils.createSuccessResponse("complete", data);
        } else {
            return JsonUtils.createErrorResponse("complete", "Task not found with ID: " + id);
        }
    }


    private JSONObject handleAssign(JSONObject request) {
        // Validate required fields
        if (!request.has("id")) {
            return JsonUtils.createErrorResponse("assign", "Missing 'id' field");
        }
        if (!request.has("assignee")) {
            return JsonUtils.createErrorResponse("assign", "Missing 'assignee' field");
        }

        // Get and validate ID
        int id;
        try {
            id = request.getInt("id");
        } catch (Exception e) {
            return JsonUtils.createErrorResponse("assign", "Invalid 'id' value. Must be an integer");
        }

        String assignee = request.getString("assignee");

        // Validate assignee not empty
        if (assignee.trim().isEmpty()) {
            return JsonUtils.createErrorResponse("assign", "Assignee name cannot be empty");
        }

        // Assign task
        boolean success = taskList.assignTask(id, assignee);

        if (success) {
            JSONObject data = new JSONObject();
            data.put("message", "Task #" + id + " assigned to " + assignee);
            return JsonUtils.createSuccessResponse("assign", data);
        } else {
            return JsonUtils.createErrorResponse("assign", "Task not found with ID: " + id);
        }
    }


    private JSONObject handleQuit() {
        JSONObject data = new JSONObject();
        data.put("message", "Goodbye!");
        return JsonUtils.createSuccessResponse("quit", data);
    }
}
