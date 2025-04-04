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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
                .requestMatchers("/admin/login", "/admin/students/add").permitAll()
                .anyRequest().hasAuthority("ROLE_ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin/home", true)
                .failureUrl("/admin/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
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
                .requestMatchers("/student/login").permitAll()
                .anyRequest().hasAuthority("ROLE_STUDENT")
            )
            .formLogin(form -> form
                .loginPage("/student/login")
                .usernameParameter("studentNumber")
                .passwordParameter("password")
                .loginProcessingUrl("/student/login")
                .defaultSuccessUrl("/student/home", true)
                .failureUrl("/student/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/student/logout")
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/error",
                    "/access-denied"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/admin/login", "POST"),
                    new AntPathRequestMatcher("/student/login", "POST"),
                    new AntPathRequestMatcher("/admin/students/add", "POST"),
                    new AntPathRequestMatcher("/admin/students/delete/**", "POST")
                ));

        return http.build();
    }
}