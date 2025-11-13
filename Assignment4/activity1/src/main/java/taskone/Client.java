package taskone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Task Management Client.
 * Provides a menu-based interface to interact with the task server.
 * You will need to edit this when you change it to proto
 */
public class Client {
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;
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
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Read welcome message
            String welcomeMsg = in.readLine();
            if (welcomeMsg != null) {
                JSONObject welcome = new JSONObject(welcomeMsg);
                System.out.println(welcome);
                if (welcome.getBoolean("ok")) {
                    System.out.println(welcome.getString("data"));
                }
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
        JSONObject request = new JSONObject();
        request.put("type", "add");
        request.put("description", description);
        request.put("priority", priority);

        // Send request and get response
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                JSONObject taskData = response.getJSONObject("data");
                System.out.println("✓ Task added successfully!");
                System.out.println("  ID: " + taskData.getInt("id"));
                System.out.println("  Description: " + taskData.getString("description"));
                System.out.println("  Priority: " + taskData.getString("priority"));
            } else {
                JSONObject error = response.getJSONObject("data");
                System.out.println("✗ Error: " + error.getString("error"));
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
        JSONObject request = new JSONObject();
        request.put("type", "list");
        request.put("filter", filter);

        // Send request and get response
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                JSONObject data = response.getJSONObject("data");
                JSONArray tasks = data.getJSONArray("tasks");
                int count = data.getInt("count");

                System.out.println("\n" + filter.toUpperCase() + " TASKS (" + count + "):");
                System.out.println("─────────────────────────────────────────────────");

                if (count == 0) {
                    System.out.println("No tasks found.");
                } else {
                    for (int i = 0; i < tasks.length(); i++) {
                        JSONObject task = tasks.getJSONObject(i);
                        System.out.println(formatTask(task));
                    }
                }
            } else {
                JSONObject error = response.getJSONObject("data");
                System.out.println("✗ Error: " + error.getString("error"));
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
        JSONObject request = new JSONObject();
        request.put("type", "complete");
        request.put("id", id);

        // Send request and get response
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                JSONObject data = response.getJSONObject("data");
                System.out.println("✓ " + data.getString("message"));
            } else {
                JSONObject error = response.getJSONObject("data");
                System.out.println("✗ Error: " + error.getString("error"));
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
        JSONObject request = new JSONObject();
        request.put("type", "assign");
        request.put("id", id);
        request.put("assignee", assignee);

        // Send request and get response
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                JSONObject data = response.getJSONObject("data");
                System.out.println("✓ " + data.getString("message"));
            } else {
                JSONObject error = response.getJSONObject("data");
                System.out.println("✗ Error: " + error.getString("error"));
            }
        }
    }

    /**
     * Quit the application.
     */
    private static void quit() {
        System.out.println("\n--- Quitting ---");

        // Create request
        JSONObject request = new JSONObject();
        request.put("type", "quit");

        // Send request and get response
        JSONObject response = sendRequest(request);
        if (response != null && response.getBoolean("ok")) {
            JSONObject data = response.getJSONObject("data");
            System.out.println(data.getString("message"));
        }
    }

    /**
     * Send a request to the server and receive response.
     */
    private static JSONObject sendRequest(JSONObject request) {
        try {
            // Send request
            out.println(request.toString());

            // Receive response
            String responseLine = in.readLine();
            if (responseLine != null) {
                return new JSONObject(responseLine);
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
    private static String formatTask(JSONObject task) {
        int id = task.getInt("id");
        String description = task.getString("description");
        String priority = task.getString("priority");
        String assignee = task.getString("assignee");
        boolean completed = task.getBoolean("completed");

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
