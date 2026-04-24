package com.example.taskmanager.service;

import com.example.taskmanager.model.ReminderConfig;
import com.example.taskmanager.repository.ReminderConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReminderConfigService {

    private static final Long SINGLETON_ID = 1L;

    private final ReminderConfigRepository repository;

    @Value("${reminder.enabled:false}")
    private boolean defaultEnabled;

    @Value("${reminder.email:}")
    private String defaultEmail;

    @Value("${reminder.phone:}")
    private String defaultPhone;

    public ReminderConfigService(ReminderConfigRepository repository) {
        this.repository = repository;
    }

    public ReminderConfig get() {
        return repository.findById(SINGLETON_ID).orElseGet(() -> {
            ReminderConfig seeded = new ReminderConfig();
            seeded.setId(SINGLETON_ID);
            seeded.setEnabled(defaultEnabled);
            seeded.setEmail(blankToNull(defaultEmail));
            seeded.setPhone(blankToNull(defaultPhone));
            return repository.save(seeded);
        });
    }

    public ReminderConfig update(ReminderConfig updates) {
        ReminderConfig existing = get();
        existing.setEnabled(updates.isEnabled());
        existing.setEmail(blankToNull(updates.getEmail()));
        existing.setPhone(blankToNull(updates.getPhone()));
        return repository.save(existing);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
