package taskone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task list that manages tasks.
 *
 * NOTE: This class is NOT thread-safe yet. You need to make it thread safe
 * for Part B when implementing the threaded server.
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
    public Task addTask(String description, String priority) {
        Task task = new Task(nextId.getAndIncrement(), description, priority);
        tasks.add(task);
        return task;
        //maybe we make a timeout in here with a random number not too long but so that thread safety becomes more important. Maybe even some prints for us to see that we are not in the same method at the same time in two threads
    }

    /**
     * Get all tasks.
     * @return Copy of task list
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * Get pending (incomplete) tasks.
     * @return List of pending tasks
     */
    public List<Task> getPendingTasks() {
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
    public List<Task> getCompletedTasks() {
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
    public Task findTaskById(int id) {
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
    public boolean completeTask(int id) {
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
    public boolean assignTask(int id, String assignee) {
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
    public int getTaskCount() {
        return tasks.size();
    }
}
