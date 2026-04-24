package com.example.taskmanager.controller;

import com.example.taskmanager.model.ReminderConfig;
import com.example.taskmanager.service.ReminderConfigService;
import com.example.taskmanager.service.ReminderScheduler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderConfigService configService;
    private final ReminderScheduler scheduler;

    public ReminderController(ReminderConfigService configService, ReminderScheduler scheduler) {
        this.configService = configService;
        this.scheduler = scheduler;
    }

    @GetMapping("/config")
    public ReminderConfig getConfig() {
        return configService.get();
    }

    @PutMapping("/config")
    public ReminderConfig updateConfig(@RequestBody ReminderConfig update) {
        return configService.update(update);
    }

    @PostMapping("/test")
    public Map<String, String> runNow() {
        scheduler.sendDailyReminders();
        return Map.of("status", "triggered");
    }
}
