package com.example.taskmanager.service;

import com.example.taskmanager.model.ReminderConfig;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final ReminderConfigService configService;

    @Value("${reminder.timezone:UTC}")
    private String timezone;

    public ReminderScheduler(TaskRepository taskRepository,
                             NotificationService notificationService,
                             ReminderConfigService configService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.configService = configService;
    }

    @Scheduled(cron = "${reminder.cron:0 0 8 * * *}", zone = "${reminder.timezone:UTC}")
    public void sendDailyReminders() {
        ReminderConfig config = configService.get();
        if (!config.isEnabled()) {
            log.debug("Daily reminders disabled — skipping");
            return;
        }
        boolean emailTarget = config.getEmail() != null && !config.getEmail().isBlank();
        boolean smsTarget = config.getPhone() != null && !config.getPhone().isBlank();
        if (!emailTarget && !smsTarget) {
            log.warn("Reminder enabled but no email or phone configured");
            return;
        }

        LocalDate today;
        try {
            today = LocalDate.now(ZoneId.of(timezone));
        } catch (Exception e) {
            log.warn("Invalid reminder.timezone '{}', falling back to UTC", timezone);
            today = LocalDate.now(ZoneId.of("UTC"));
        }

        List<Task> dueToday = taskRepository.findByDueDate(today).stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .sorted(Comparator.comparing(Task::getPriority))
                .toList();

        if (dueToday.isEmpty()) {
            log.info("No tasks due today — no reminder sent");
            return;
        }

        String subject = dueToday.size() == 1
                ? "1 task due today"
                : dueToday.size() + " tasks due today";

        StringBuilder body = new StringBuilder();
        body.append("You have ")
                .append(dueToday.size())
                .append(dueToday.size() == 1 ? " task due today:\n\n" : " tasks due today:\n\n");
        for (Task t : dueToday) {
            body.append("- ").append(t.getTitle());
            if (t.getPriority() != null) body.append(" [").append(t.getPriority()).append("]");
            body.append('\n');
        }
        body.append("\nGet after it.");

        String bodyText = body.toString();

        if (emailTarget) {
            String result = notificationService.deliverEmail(config.getEmail(), subject, bodyText);
            log.info("Daily reminder email: {}", result);
        }
        if (smsTarget) {
            String smsBody = subject + "\n\n" + bodyText;
            String result = notificationService.deliverSms(config.getPhone(), smsBody);
            log.info("Daily reminder SMS: {}", result);
        }
    }
}
