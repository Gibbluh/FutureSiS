package com.example.auth1.config;

import com.example.auth1.service.CustomStudentDetailsService;
import com.example.auth1.service.CustomAdminDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final CustomAdminDetailsService adminDetailsService;
    private final CustomStudentDetailsService studentDetailsService;

    public SecurityConfig(CustomAdminDetailsService adminDetailsService,
                         CustomStudentDetailsService studentDetailsService) {
        this.adminDetailsService = adminDetailsService;
        this.studentDetailsService = studentDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider adminAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(adminDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public DaoAuthenticationProvider studentAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(studentDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")
            .authenticationProvider(adminAuthenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .usernameParameter("email")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin/home")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/")
            );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain studentFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/student/**")
            .authenticationProvider(studentAuthenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasRole("STUDENT")
            )
            .formLogin(form -> form
                .loginPage("/student/login")
                .usernameParameter("studentNumber") // Make sure this matches the form field name
                .loginProcessingUrl("/student/login")
                .defaultSuccessUrl("/student/home")
                .failureUrl("/student/login?error=true") // Add this to see login failures
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/student/logout")
                .logoutSuccessUrl("/")
            );

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**", "/js/**", "/images/**",
                                "/admin/login", "/student/login").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}