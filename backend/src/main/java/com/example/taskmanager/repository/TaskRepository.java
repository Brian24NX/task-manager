package com.example.taskmanager.repository;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByDueDate(LocalDate dueDate);
    List<Task> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
    long countByStatus(TaskStatus status);
    long countByDueDateBeforeAndStatusNot(LocalDate date, TaskStatus status);

    @Query("SELECT COUNT(t) AS total, " +
           "SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END) AS todo, " +
           "SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS inProgress, " +
           "SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) AS done, " +
           "SUM(CASE WHEN t.dueDate < :today AND t.status <> 'DONE' THEN 1 ELSE 0 END) AS overdue " +
           "FROM Task t")
    List<Object[]> getStatsAggregated(@Param("today") LocalDate today);
}
