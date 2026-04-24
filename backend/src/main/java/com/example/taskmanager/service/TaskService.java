package com.example.taskmanager.service;

import com.example.taskmanager.model.Recurrence;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> listTasks(LocalDate dueDate) {
        if (dueDate != null) {
            return taskRepository.findByDueDate(dueDate);
        }
        return taskRepository.findAll();
    }

    public Task create(Task task) {
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        if (task.getPriority() == null) {
            task.setPriority(TaskPriority.MEDIUM);
        }
        return taskRepository.save(task);
    }

    public Task update(Long id, Task updatedTask) {
        Task existing = findById(id);
        existing.setTitle(updatedTask.getTitle());
        existing.setDescription(updatedTask.getDescription());
        existing.setDueDate(updatedTask.getDueDate());
        existing.setStatus(updatedTask.getStatus());
        existing.setPriority(updatedTask.getPriority());
        existing.setRecurrence(updatedTask.getRecurrence() != null ? updatedTask.getRecurrence() : Recurrence.NONE);
        existing.setRecurrenceInterval(updatedTask.getRecurrenceInterval() != null ? updatedTask.getRecurrenceInterval() : 1);
        return taskRepository.save(existing);
    }

    public record StatusChangeResult(Task task, Task next) {}

    public StatusChangeResult updateStatus(Long id, TaskStatus status) {
        Task existing = findById(id);
        TaskStatus previous = existing.getStatus();
        existing.setStatus(status);
        Task saved = taskRepository.save(existing);

        Task next = null;
        boolean justCompleted = status == TaskStatus.DONE && previous != TaskStatus.DONE;
        if (justCompleted && saved.getRecurrence() != null && saved.getRecurrence() != Recurrence.NONE) {
            next = createNextInstance(saved);
        }
        return new StatusChangeResult(saved, next);
    }

    private Task createNextInstance(Task completed) {
        int interval = completed.getRecurrenceInterval() != null && completed.getRecurrenceInterval() > 0
                ? completed.getRecurrenceInterval()
                : 1;
        LocalDate today = LocalDate.now();
        LocalDate base = completed.getDueDate() != null && !completed.getDueDate().isBefore(today)
                ? completed.getDueDate()
                : today;
        LocalDate nextDueDate = switch (completed.getRecurrence()) {
            case DAILY -> base.plusDays(interval);
            case WEEKLY -> base.plusWeeks(interval);
            case MONTHLY -> base.plusMonths(interval);
            default -> null;
        };
        if (nextDueDate == null) return null;

        Task next = new Task();
        next.setTitle(completed.getTitle());
        next.setDescription(completed.getDescription());
        next.setDueDate(nextDueDate);
        next.setStatus(TaskStatus.TODO);
        next.setPriority(completed.getPriority());
        next.setRecurrence(completed.getRecurrence());
        next.setRecurrenceInterval(interval);
        return taskRepository.save(next);
    }

    public void delete(Long id) {
        Task existing = findById(id);
        taskRepository.delete(existing);
    }

    public List<Task> searchTasks(String query) {
        return taskRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
    }

    public Map<String, Object> getStats() {
        List<Object[]> rows = taskRepository.getStatsAggregated(LocalDate.now());
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        if (rows.isEmpty() || rows.get(0)[0] == null) {
            stats.put("total", 0L);
            stats.put("todo", 0L);
            stats.put("inProgress", 0L);
            stats.put("done", 0L);
            stats.put("overdue", 0L);
        } else {
            Object[] row = rows.get(0);
            stats.put("total", ((Number) row[0]).longValue());
            stats.put("todo", ((Number) row[1]).longValue());
            stats.put("inProgress", ((Number) row[2]).longValue());
            stats.put("done", ((Number) row[3]).longValue());
            stats.put("overdue", ((Number) row[4]).longValue());
        }
        return stats;
    }

    private Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));
    }
}
