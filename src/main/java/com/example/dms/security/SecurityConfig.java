package com.example.dms.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration. Sets up a stateless JWT-based security policy:
 * CSRF is disabled (not needed for token-based APIs), sessions are never created,
 * and all requests except {@code /auth/**} require a valid token.
 */
@Configuration //tells spring this class contains bean definitions
@EnableWebSecurity //activates spring Security for the whole application
@EnableMethodSecurity //allows me to use @PreAuthorize("hasRole('ADMIN')") directly on controller methods later
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter){
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http.csrf(AbstractHttpConfigurer::disable) //cross-ste request forgery protection is a security mechanism for browser-based apps that use cookies for authentication. Since our API uses JWT tokens in headers instead of cookies, CSRF protection is unnecessary and would actually break our requests. So we disable it.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) //By default Spring Security creates an HTTP session to remember who you are between requests. We don't want that — JWT is designed to be stateless. Every request carries the token and is authenticated independently. This tells Spring to never create a session.
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated()
                )//auth/** — anything under /auth/ (so /auth/register, /auth/login) is public. No token needed.
                //.anyRequest().authenticated() — everything else requires a valid token.
                .addFilterBefore(jwtAuthFilter,UsernamePasswordAuthenticationFilter.class);//This registers your JwtAuthFilter into Spring Security's filter chain, placing it before Spring's default UsernamePasswordAuthenticationFilter. This ensures your JWT check runs first on every request.

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
