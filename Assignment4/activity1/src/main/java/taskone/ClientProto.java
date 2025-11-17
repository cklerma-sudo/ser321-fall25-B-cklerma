package taskone;

import taskone.proto.Request;
import taskone.proto.Response;
import taskone.proto.Response.ResponseType;
import taskone.proto.Request.RequestType;
import taskone.proto.Data;
import taskone.proto.Task_proto;
import taskone.proto.TaskList_proto;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;


/**
 * Task Management Client.
 * Provides a menu-based interface to interact with the task server.
 * You will need to edit this when you change it to proto
 */
public class ClientProto {
    private static Socket socket;
    private static InputStream in;
    private static OutputStream out;
    private static Scanner scanner;

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8888;

        // Parse command line arguments
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: 8888");
            }
        }

        scanner = new Scanner(System.in);

        try {
            // Connect to server
            System.out.println("Connecting to Task Management Server at " + host + ":" + port);
            socket = new Socket(host, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();

            // Read welcome message
            Response response = Response.parseDelimitedFrom(in);
            if (!response != null) {
                String welcomeMsg = response.getData().getSuccessMessage();
                if (!welcomeMsg.isEmpty()) System.out.println(welcomeMsg);
            }
            else {
                System.out.println("Server closed connection.");
                return;
            }

            // Main menu loop
            boolean running = true;
            while (running) {
                displayMenu();
                int choice = getMenuChoice();

                switch (choice) {
                    case 1:
                        addTask();
                        break;
                    case 2:
                        listTasks();
                        break;
                    case 3:
                        completeTask();
                        break;
                    case 4:
                        assignTask();
                        break;
                    case 0:
                        quit();
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Display the main menu.
     */
    private static void displayMenu() {
        System.out.println("\n========== Task Management Menu ==========");
        System.out.println("1. Add Task");
        System.out.println("2. List Tasks");
        System.out.println("3. Complete Task");
        System.out.println("4. Assign Task");
        System.out.println("0. Quit");
        System.out.println("==========================================");
        System.out.print("Enter your choice: ");
    }

    /**
     * Get user's menu choice.
     */
    private static int getMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Add a new task.
     */
    private static void addTask() {
        System.out.println("\n--- Add Task ---");
        System.out.print("Enter task description: ");
        String description = scanner.nextLine().trim();

        if (description.isEmpty()) {
            System.out.println("Error: Description cannot be empty");
            return;
        }

        System.out.print("Enter priority (low/medium/high): ");
        String priority = scanner.nextLine().trim().toLowerCase();

        if (!priority.equals("low") && !priority.equals("medium") && !priority.equals("high")) {
            System.out.println("Error: Invalid priority. Must be 'low', 'medium', or 'high'");
            return;
        }

        // Create request
        Request request = Request.newBuilder()
            .setType(ADD)
            .setDescription(description)
            .setPriority(priority)
            .build();
        // Send request and get response
        Response response = sendRequest(request);
        if (response != null) {
            if (response.getOk()) {
                System.out.println("✓ Task added successfully!");
                System.out.println("  ID: " + response.getData().getId());
                System.out.println("  Description: " + response.getData().getDescription());
                System.out.println("  Priority: " + response.getData().getPriority());
            } else {
                System.out.println("✗ Error: " + response.getData().getErrorMessage());
            }
        }
    }

    /**
     * List tasks with filter options.
     */
    private static void listTasks() {
        System.out.println("\n--- List Tasks ---");
        System.out.println("1. All tasks");
        System.out.println("2. Pending tasks");
        System.out.println("3. Completed tasks");
        System.out.print("Enter your choice: ");

        int choice = getMenuChoice();
        String filter;

        switch (choice) {
            case 1:
                filter = "all";
                break;
            case 2:
                filter = "pending";
                break;
            case 3:
                filter = "completed";
                break;
            default:
                System.out.println("Invalid choice");
                return;
        }

        // Create request
        Request request = Request.newBuilder()
            .setType(LIST)
            .setFilter(filter)
            .build();

        // Send request and get response
        Response response = sendRequest(request);
        if (response != null) {
            if (response.getOk()) {
                TaskList_proto list = response.getData().getTasks();

                System.out.println("\n" + filter.toUpperCase() + " TASKS (" + list.getCount() + "):");
                System.out.println("─────────────────────────────────────────────────");

                if (count == 0) {
                    System.out.println("No tasks found.");
                } else {
                    for (int i = 0; i < list.getCount(); i++) {
                        Task_proto task = list.getTasks(i);
                        System.out.println(formatTask(task));
                    }
                }
            } else {
                System.out.println("✗ Error: " + response.getData().getErrorMessage());
            }
        }
    }

    /**
     * Mark a task as completed.
     */
    private static void completeTask() {
        System.out.println("\n--- Complete Task ---");
        System.out.print("Enter task ID to complete: ");

        int id;
        try {
            id = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid task ID");
            return;
        }

        // Create request
        Request request = Request.newBuilder()
            .setType(COMPLETE)
            .setId(id)
            .build();

        // Send request and get response
        Response response = sendRequest(request);
        if (response != null) {
            if (response.getOk()) {
                Data data = response.getData();
                System.out.println("✓ " + data.getSuccessMessage());
            } else {
                Data error = response.getData();
                System.out.println("✗ Error: " + error.getErrorMessage());
            }
        }
    }

    /**
     * Assign a task to someone.
     */
    private static void assignTask() {
        System.out.println("\n--- Assign Task ---");
        System.out.print("Enter task ID to assign: ");

        int id;
        try {
            id = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid task ID");
            return;
        }

        System.out.print("Enter assignee name: ");
        String assignee = scanner.nextLine().trim();

        if (assignee.isEmpty()) {
            System.out.println("Error: Assignee name cannot be empty");
            return;
        }

        // Create request
        Request request = Request.newBuilder()
            .setType(ASSIGN)
            .setId(id)
            .setAssignee(assignee)
            .build();

        // Send request and get response
        Response response = sendRequest(request);
        if (response != null) {
            if (response.getOk()) {
                Data data = response.getData();
                System.out.println("✓ " + data.getSuccessMessage());
            } else {
                Data error = response.getData();
                System.out.println("✗ Error: " + error.getErrorMessage());
            }
        }
    }

    /**
     * Quit the application.
     */
    private static void quit() {
        System.out.println("\n--- Quitting ---");

        // Create request
        Request request = Request.newBuilder()
            .setType(QUIT)
            .build();

        // Send request and get response
        Response response = sendRequest(request);
        if (response != null && response.getOk()) {
            Data data = response.getData();
            System.out.println(data.getSuccessMessage());
        }
    }

    /**
     * Send a request to the server and receive response.
     */
    private static Response sendRequest(Request request) {
        try {
            // Send request
            request.writeDelimitedTo(out);
            out.flush();

            // Receive response
            Response response = Response.parseDelimitedFrom(in);
            if (response != null) {
                return response;
            } else {
                System.out.println("Error: No response from server");
                return null;
            }
        } catch (IOException e) {
            System.out.println("Error communicating with server: " + e.getMessage());
            return null;
        }
    }

    /**
     * Format a task for display.
     */
    private static String formatTask(Task_proto task) {
        int id = task.getId();
        String description = task.getDescription();
        String priority = task.getPriority();
        String assignee = task.getAssignee();
        boolean completed = task.getCompleted();

        String status = completed ? "[✓] DONE" : "[ ] PENDING";
        String prioritySymbol;
        switch (priority) {
            case "high":
                prioritySymbol = "!!!";
                break;
            case "medium":
                prioritySymbol = "!! ";
                break;
            default:
                prioritySymbol = "!  ";
                break;
        }

        return String.format("%s #%d [%s] %s - Assigned to: %s",
                status, id, prioritySymbol, description, assignee);
    }

    /**
     * Clean up resources.
     */
    private static void cleanup() {
        try {
            if (scanner != null) {
                scanner.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
}
