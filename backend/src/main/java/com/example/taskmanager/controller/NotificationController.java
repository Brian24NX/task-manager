package com.example.taskmanager.controller;

import com.example.taskmanager.model.NotificationRequest;
import com.example.taskmanager.service.NotificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public Map<String, Object> sendNotification(@RequestBody NotificationRequest request) {
        List<String> results = notificationService.sendNotifications(request);
        return Map.of("results", results);
    }
}
