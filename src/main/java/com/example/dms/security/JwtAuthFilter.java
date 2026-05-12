package com.example.dms.security;

import com.example.dms.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
// OncePerRequestFilter = guarantees this filter runs exactly once per request

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuditService auditService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService,
                         AuditService auditService) {
        this.jwtService = jwtService; //to validate the token
        this.userDetailsService = userDetailsService; //to load the user from the database
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        //Client sends token like this: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ...
        //if the header is missing or doesn't start with Bearer, pass the request along and return early
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        final String email;

        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        //if we got the email and the request hasn't been authenticated yet
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                //check token signature is correct and not expired and account hasn't been deactivated by admin
                if (jwtService.isTokenValid(token, email) && userDetails.isEnabled()) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else if (!userDetails.isEnabled()) {
                    auditService.log(email, "AUTH_DENIED_DEACTIVATED", "USER", null, null,
                            "JWT rejected: account disabled");
                }
            } catch (UsernameNotFoundException e) {
                log.debug("JWT references unknown user: {}", email);
            }
        }

        filterChain.doFilter(request, response); //it passes the request to the next filter in the chain
    }
}