package com.example.auth1.controller;

import com.example.auth1.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot_password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
                            @RequestParam String userType,
                            RedirectAttributes redirectAttributes) {
        boolean success = false;
        
        if ("student".equals(userType)) {
            success = passwordResetService.resetStudentPassword(email);
        } else if ("faculty".equals(userType)) {
            success = passwordResetService.resetFacultyPassword(email);
        }

        if (success) {
            redirectAttributes.addFlashAttribute("message",
                "Password reset email has been sent to your email address.");
        } else {
            redirectAttributes.addFlashAttribute("error",
                "No account found with the provided email address.");
        }

        return "redirect:/forgot-password";
    }
} 