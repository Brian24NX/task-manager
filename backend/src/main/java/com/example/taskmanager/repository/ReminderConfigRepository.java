package com.example.taskmanager.repository;

import com.example.taskmanager.model.ReminderConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderConfigRepository extends JpaRepository<ReminderConfig, Long> {
}
