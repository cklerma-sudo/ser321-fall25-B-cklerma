package taskone;

import taskone.proto.Request;
import taskone.proto.Response;
import taskone.proto.Response.ResponseType;
import taskone.proto.Request.RequestType;
import taskone.proto.Data;
import taskone.proto.Task_proto;
import taskone.proto.TaskList_proto;

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
                                .setSuccessMessage("Connected to Task Management Server")
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
        } 
        finally {
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
                case ADD:
                    return handleAdd(request);
                case LIST:
                    return handleList(request);
                case COMPLETE:
                    return handleComplete(request);
                case ASSIGN:
                    return handleAssign(request);
                case QUIT:
                    return handleQuit();
                default:
                    return createErrorResponse(type, "Unknown request type: " + type);
            }
        } catch (Exception e) {
            return createErrorResponse("error", "Invalid request format: " + e.getMessage());
        }
    }

    private Response handleAdd(Request request) {
        String description = request.getDescription();
        String priority = request.getPriority();
        
        // Validate required fields
        if (description.isEmpty()) {
            return createErrorResponse("add", "Missing 'description' field");
        }
        if (priority.isEmpty()) {
            return createErrorResponse("add", "Missing 'priority' field");
        }

        // Validate priority value
        if (!priority.equals("low") && !priority.equals("medium") && !priority.equals("high")) {
            return createErrorResponse("add", "Invalid priority value. Must be 'low', 'medium', or 'high'");
        }

        // Add task
        Task task = taskList.addTask(description, priority);
        Data data = Data.newBuilder()
            .setId(task.getId())
            .setPriority(priority)
            .setDescription(description)
            .setAssignee(task.getAssignee())
            .setCompleted(false)
            .build();

        // Return success response with created task
        return createSuccessResponse("add", data);
    }


    private Response handleList(Request request) {
        // Get filter (defaults to "all")
        String filter = request.getFilter();
        if (!filter.equals("pending") && !filter.equals("completed")) filter = "all";
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
                return createErrorResponse("list", "Invalid filter value. Must be 'all', 'pending', or 'completed'");
        }

       TaskList_proto.Builder listBuilder = TaskList_proto.newBuilder();
        for (Task task : tasks) {
            Task_proto task_proto = Task_proto.newBuilder()
                .setId(task.getId())
                .setPriority(task.getPriority())
                .setDescription(task.getDescription())
                .setAssignee(task.getAssignee())
                .setCompleted(task.isCompleted())
                .build();
            listBuilder.addTasks(task_proto);
        }
        TaskList_proto list = listBuilder.setCount(tasks.size()).build();

        // Create response data
        Data data = Data.newBuilder()
            .setTasks(list)
            .build();

        return createSuccessResponse("list", data);
    }


    private Response handleComplete(Request request) {
        int id = request.getId();
        
        // Validate required field
        if (id == 0) {
            return createErrorResponse("complete", "Missing 'id' field");
        }


        // Mark task as completed
        boolean success = taskList.completeTask(id);

        if (success) {
            return createSuccessResponse("complete", "Task #" + id + " marked as completed");
        } else {
            return createErrorResponse("complete", "Task not found with ID: " + id);
        }
    }


    private Response handleAssign(Request request) {
        int id = request.getId();
        String assignee = request.getAssignee();
        // Validate required field
        if (id == 0) {
            return createErrorResponse("complete", "Missing 'id' field");
        }
        if (assignee.isEmpty()) {
            return createErrorResponse("assign", "Missing 'assignee' field");
        }

        // Assign task
        boolean success = taskList.assignTask(id, assignee);

        if (success) {
            return createSuccessResponse("assign", "Task #" + id + " assigned to " + assignee);
        } else {
            return createErrorResponse("assign", "Task not found with ID: " + id);
        }
    }


    private JSONObject handleQuit() {
        return createSuccessResponse("quit", "Goodbye!");
    }

    private Response createErrorResponse(String type, String ErrorMessage) {
        Response errorRes = Response.newBuilder()
            .setType(ResponseType.ERROR)
            .setOk(false)
            .setReqType(type)
            .setData(
                Data.newBuilder()
                .setErrorMessage(ErrorMessage)
                .build()
            )
            .build();
        return errorRes;
    }

     private Response createSuccessResponse(String type, String SuccessMessage) {
        Response successRes = Response.newBuilder()
            .setType(ResponseType.SUCCESS)
            .setOk(true)
            .setReqType(type)
            .setData(
                Data.newBuilder()
                .setSuccessMessage(SuccessMessage)
                .build()
            )
            .build();
        return successRes;
    }

     private Response createSuccessResponse(String type, Data data) {
        Response successRes = Response.newBuilder()
            .setType(ResponseType.SUCCESS)
            .setOk(true)
            .setReqType(type)
            .setData(data)
            .build();
        return successRes;
    }
}
