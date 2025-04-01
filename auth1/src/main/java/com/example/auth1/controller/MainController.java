package com.example.auth1.controller;



import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin/admin_login";
    }

    @GetMapping("/admin/home")
    public String adminHome() {
        return "admin/admin_home";
    }

    @GetMapping("/user/login")
    public String userLogin() {
        return "user/user_login";
    }

    @GetMapping("/user/home")
    public String userHome() {
        return "user/user_home";
    }
}