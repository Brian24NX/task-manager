package com.example.taskmanager.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @PostMapping("/login")
    public Map<String, String> login(@AuthenticationPrincipal UserDetails user) {
        return Map.of("username", user.getUsername());
    }

    @GetMapping("/me")
    public Map<String, String> me(@AuthenticationPrincipal UserDetails user) {
        return Map.of("username", user.getUsername());
    }
}
