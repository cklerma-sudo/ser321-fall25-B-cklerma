package taskone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

/**
 * Task list that manages tasks.
 */
public class TaskList {
    private final List<Task> tasks;
    private final AtomicInteger nextId;

    public TaskList() {
        this.tasks = new ArrayList<>();
        this.nextId = new AtomicInteger(1);
    }

    /**
     * Add a new task to the list.
     * @param description Task description
     * @param priority Task priority (low, medium, high)
     * @return The created Task object
     */
    public synchronized Task addTask(String description, String priority) {
        System.out.println("A thread has entered the add task Method");
        Task task = new Task(nextId.getAndIncrement(), description, priority);
        tasks.add(task);
        Random rand = new Random();
        int n = rand.nextInt(1000);
        try {
            Thread.sleep(n); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("A thread has exited the add task Method");
        return task;
    }

    /**
     * Get all tasks.
     * @return Copy of task list
     */
    public synchronized List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * Get pending (incomplete) tasks.
     * @return List of pending tasks
     */
    public synchronized List<Task> getPendingTasks() {
        List<Task> pending = new ArrayList<>();
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                pending.add(task);
            }
        }
        return pending;
    }

    /**
     * Get completed tasks.
     * @return List of completed tasks
     */
    public synchronized List<Task> getCompletedTasks() {
        List<Task> completed = new ArrayList<>();
        for (Task task : tasks) {
            if (task.isCompleted()) {
                completed.add(task);
            }
        }
        return completed;
    }

    /**
     * Find a task by ID.
     * @param id Task ID
     * @return Task object or null if not found
     */
    public synchronized Task findTaskById(int id) {
        for (Task task : tasks) {
            if (task.getId() == id) {
                return task;
            }
        }
        return null;
    }

    /**
     * Mark a task as completed.
     * @param id Task ID
     * @return true if successful, false if task not found
     */
    public synchronized boolean completeTask(int id) {
        Task task = findTaskById(id);
        if (task != null) {
            task.setCompleted(true);
            return true;
        }
        return false;
    }

    /**
     * Assign a task to someone.
     * @param id Task ID
     * @param assignee Person to assign to
     * @return true if successful, false if task not found
     */
    public synchronized boolean assignTask(int id, String assignee) {
        Task task = findTaskById(id);
        if (task != null) {
            task.setAssignee(assignee);
            return true;
        }
        return false;
    }

    /**
     * Get count of tasks.
     * @return Number of tasks
     */
    public synchronized int getTaskCount() {
        return tasks.size();
    }
}
