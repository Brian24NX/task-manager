package com.example.taskmanager.service;

import com.example.taskmanager.model.NotificationRequest;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${twilio.phone-number:}")
    private String twilioPhoneNumber;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<String> sendNotifications(NotificationRequest request) {
        List<String> results = new ArrayList<>();
        String subject = buildSubject(request);
        String body = buildBody(request);

        if (request.isNotifyEmail() && request.getEmail() != null && !request.getEmail().isBlank()) {
            results.add(sendEmail(request.getEmail(), subject, body));
        }

        if (request.isNotifySms() && request.getPhone() != null && !request.getPhone().isBlank()) {
            results.add(sendSms(request.getPhone(), body));
        }

        return results;
    }

    private String sendEmail(String to, String subject, String body) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Resend API key not configured — skipping email notification");
            return "Email not configured. Set RESEND_API_KEY environment variable.";
        }
        try {
            String jsonBody = """
                    {
                      "from": "Task Manager <onboarding@resend.dev>",
                      "to": ["%s"],
                      "subject": "%s",
                      "text": "%s"
                    }
                    """.formatted(
                    to.replace("\"", "\\\""),
                    subject.replace("\"", "\\\""),
                    body.replace("\"", "\\\"")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Email sent to {}", to);
                return "Email sent to " + to;
            } else {
                log.error("Resend API error ({}): {}", response.statusCode(), response.body());
                return "Email failed: " + response.body();
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            return "Email failed: " + e.getMessage();
        }
    }

    private String sendSms(String to, String body) {
        if (twilioAccountSid == null || twilioAccountSid.isBlank()) {
            log.warn("Twilio not configured — skipping SMS notification");
            return "SMS not configured. Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_PHONE_NUMBER.";
        }
        try {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(twilioPhoneNumber),
                    body
            ).create();
            log.info("SMS sent to {}, SID: {}", to, message.getSid());
            return "SMS sent to " + to;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage());
            return "SMS failed: " + e.getMessage();
        }
    }

    private String buildSubject(NotificationRequest request) {
        if ("COMPLETED".equals(request.getEventType())) {
            return "Task Completed: " + request.getTaskTitle();
        }
        return "New Task Created: " + request.getTaskTitle();
    }

    private String buildBody(NotificationRequest request) {
        if ("COMPLETED".equals(request.getEventType())) {
            return "Your task \"" + request.getTaskTitle() + "\" has been marked as done. Great job!";
        }
        return "A new task \"" + request.getTaskTitle() + "\" has been created in Task Manager.";
    }
}
