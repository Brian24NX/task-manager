package com.example.taskmanager.model;

public class NotificationRequest {

    private String email;
    private String phone;
    private boolean notifyEmail;
    private boolean notifySms;
    private String taskTitle;
    private String eventType; // "CREATED" or "COMPLETED"

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isNotifyEmail() { return notifyEmail; }
    public void setNotifyEmail(boolean notifyEmail) { this.notifyEmail = notifyEmail; }
    public boolean isNotifySms() { return notifySms; }
    public void setNotifySms(boolean notifySms) { this.notifySms = notifySms; }
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}
