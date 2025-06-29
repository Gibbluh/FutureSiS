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

    public void sendFacultyCredentialsEmail(String to, String facultyId, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Faculty Account Created - Your Credentials");
        message.setText("Your faculty account has been created.\n\nFaculty ID: " + facultyId + "\nPassword: " + password +
                "\n\nPlease login with these credentials and change your password immediately for security reasons.");
        emailSender.send(message);
    }
}