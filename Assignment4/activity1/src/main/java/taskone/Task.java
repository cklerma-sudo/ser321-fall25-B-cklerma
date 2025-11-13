package taskone;

/**
 * Represents a task in the task management system.
 */
public class Task {
    private final int id;
    private final String description;
    private final String priority;
    private String assignee;
    private boolean completed;

    public Task(int id, String description, String priority) {
        this.id = id;
        this.description = description;
        this.priority = priority;
        this.assignee = "unassigned";
        this.completed = false;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getPriority() {
        return priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return String.format("Task #%d: %s [Priority: %s, Assigned to: %s, Completed: %s]",
                id, description, priority, assignee, completed);
    }
}
