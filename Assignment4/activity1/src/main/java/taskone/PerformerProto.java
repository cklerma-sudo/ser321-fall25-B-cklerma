package taskone;

import taskone.proto.task.Request;
import taskone.proto.task.Response;
import taskone.proto.task.Response.ResponseType;
import taskone.proto.task.Request.RequestType;
import taskone.proto.task.Data;
import taskone.proto.task.Task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Performer class handles client requests using protobuffer protocol.
 * This version uses protobuffer for serialization.
 */
public class PerformerProto {
    private final Socket clientSocket;
    private final TaskList taskList;
    private InputStream in;
    private OutputStream out;

    public PerformerProto(Socket clientSocket, TaskList taskList) {
        this.clientSocket = clientSocket;
        this.taskList = taskList;
    }

    /**
     * Main method to process client requests.
     * Reads requests, processes them, and sends responses.
     */
    public void doPerform() {
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();

            // Send welcome message
           Response welcome = Response.newBuilder()
                    .setType(ResponseType.SUCCESS)
                    .setOk(true)
                    .setData(
                            Data.newBuilder()
                                .setMessage("Connected to Task Management Server")
                                .build()
                    )
                    .build();
            welcome.writeDelimitedTo(out);
            out.flush();

            // Process requests
           while (true) {
                Request request = Request.parseDelimitedFrom(in);
                if (request == null) {
                    break;
                }

                Response response = handleRequest(request);
                response.writeDelimitedTo(out);
                out.flush();

                if (request.getType() == RequestType.QUIT) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Handle a single request from the client.
     * @param requestStr JSON request string
     * @return JSON response object
     */
    private Response handleRequest(Request request) {
        try {
            RequestType type = request.getType();
            // Route to appropriate handler
            switch (type) {
                case "ADD":
                    return handleAdd(request);
                case "LIST":
                    return handleList(request);
                case "COMPLETE":
                    return handleComplete(request);
                case "ASSIGN":
                    return handleAssign(request);
                case "QUIT":
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
