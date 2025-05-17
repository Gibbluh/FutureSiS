package com.example.auth1.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Get error details
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        // Add status code
        Integer statusCode = status != null ? Integer.valueOf(status.toString()) : 500;
        HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
        model.addAttribute("status", httpStatus.value() + " - " + httpStatus.getReasonPhrase());
        
        // Add error message
        if (message != null && !message.toString().isEmpty()) {
            model.addAttribute("message", message);
        } else if (exception != null) {
            Throwable throwable = (Throwable) exception;
            model.addAttribute("message", throwable.getMessage());
        }
        
        // Add error details for debugging
        if (exception != null) {
            Throwable throwable = (Throwable) exception;
            model.addAttribute("error", throwable.getClass().getName());
            model.addAttribute("trace", getStackTrace(throwable));
        }
        
        // Add request path
        if (path != null) {
            model.addAttribute("path", path);
        }
        
        return "error";
    }
    
    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            // Only show first 10 lines of stack trace
            if (sb.toString().split("\n").length > 10) {
                sb.append("... (truncated)");
                break;
            }
        }
        return sb.toString();
    }
} 