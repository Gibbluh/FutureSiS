package com.example.auth1.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender emailSender;

    public void sendPasswordResetEmail(String to, String newPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset - Your New Password");
        message.setText("Your password has been reset. Your new password is: " + newPassword + 
                       "\n\nPlease login with this password and change it immediately for security reasons.");
        
        emailSender.send(message);
    }
} 