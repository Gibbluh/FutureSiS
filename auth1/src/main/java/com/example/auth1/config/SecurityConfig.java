package com.example.auth1.config;

import com.example.auth1.repository.AdminRepository;
import com.example.auth1.repository.FacultyRepository;
import com.example.auth1.repository.StudentRepository;
import com.example.auth1.service.CustomStudentDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final FacultyRepository facultyRepository;
    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;

    public SecurityConfig(FacultyRepository facultyRepository, 
                        AdminRepository adminRepository,
                        StudentRepository studentRepository) {
        this.facultyRepository = facultyRepository;
        this.adminRepository = adminRepository;
        this.studentRepository = studentRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider adminAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(adminUserDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public DaoAuthenticationProvider facultyAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(facultyUserDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public DaoAuthenticationProvider studentAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customStudentDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/login", "/admin/login?error=true").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN"))
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/admin/home", true)
                .failureUrl("/admin/login?error=true")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/")
                .permitAll())
            .authenticationProvider(adminAuthenticationProvider());
        
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain facultyFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/faculty/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/faculty/login", "/faculty/login?error=true").permitAll()
                .requestMatchers("/faculty/**").hasRole("FACULTY"))
            .formLogin(form -> form
                .loginPage("/faculty/login")
                .loginProcessingUrl("/faculty/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/faculty/home", true)
                .failureUrl("/faculty/login?error=true")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/faculty/logout")
                .logoutSuccessUrl("/")
                .permitAll())
            .authenticationProvider(facultyAuthenticationProvider());
        
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain studentFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/student/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/student/login", "/student/login?error=true").permitAll()
                .requestMatchers("/student/**").hasRole("STUDENT"))
            .formLogin(form -> form
                .loginPage("/student/login")
                .loginProcessingUrl("/student/login")
                .usernameParameter("studentNumber")
                .passwordParameter("password")
                .defaultSuccessUrl("/student/home", true)
                .failureUrl("/student/login?error=true")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/student/logout")
                .logoutSuccessUrl("/")
                .permitAll())
            .authenticationProvider(studentAuthenticationProvider());
        
        return http.build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home", "/css/**", "/js/**", "/images/**", "/error", "/forgot-password", "/reset-password").permitAll()
                .anyRequest().authenticated())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        
        return http.build();
    }

    @Bean
    public UserDetailsService adminUserDetailsService() {
        return username -> {
            logger.debug("Attempting to load admin user: {}", username);
            return adminRepository.findByEmail(username)
                .map(admin -> {
                    logger.debug("Found admin user: {}", admin.getEmail());
                    return org.springframework.security.core.userdetails.User
                        .withUsername(admin.getEmail())
                        .password(admin.getPassword())
                        .roles("ADMIN")
                        .build();
                })
                .orElseThrow(() -> {
                    logger.error("Admin user not found: {}", username);
                    return new RuntimeException("Admin not found");
                });
        };
    }

    @Bean
    public UserDetailsService facultyUserDetailsService() {
        return username -> {
            logger.debug("Attempting to load faculty user: {}", username);
            return facultyRepository.findByFacultyId(username)
                .map(faculty -> {
                    logger.debug("Found faculty user: {}", faculty.getFacultyId());
                    return org.springframework.security.core.userdetails.User
                        .withUsername(faculty.getFacultyId())
                        .password(faculty.getPassword())
                        .roles("FACULTY")
                        .build();
                })
                .orElseThrow(() -> {
                    logger.error("Faculty user not found: {}", username);
                    return new RuntimeException("Faculty not found");
                });
        };
    }

    @Bean
    public CustomStudentDetailsService customStudentDetailsService() {
        return new CustomStudentDetailsService(studentRepository);
    }
}