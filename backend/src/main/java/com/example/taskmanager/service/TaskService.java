package com.example.taskmanager.service;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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
        return taskRepository.save(task);
    }

    public Task update(Long id, Task updatedTask) {
        Task existing = findById(id);
        existing.setTitle(updatedTask.getTitle());
        existing.setDescription(updatedTask.getDescription());
        existing.setDueDate(updatedTask.getDueDate());
        existing.setStatus(updatedTask.getStatus());
        return taskRepository.save(existing);
    }

    public Task updateStatus(Long id, TaskStatus status) {
        Task existing = findById(id);
        existing.setStatus(status);
        return taskRepository.save(existing);
    }

    public void delete(Long id) {
        Task existing = findById(id);
        taskRepository.delete(existing);
    }

    private Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));
    }
}
