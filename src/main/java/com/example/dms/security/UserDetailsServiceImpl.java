package com.example.dms.security;

import com.example.dms.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security {@link UserDetailsService} implementation. Loads a user by email
 * from the database and converts it into a Spring Security {@code UserDetails} object,
 * including the user's role as a {@code GrantedAuthority} and the {@code active} flag
 * as the enabled state.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override //Spring Security calls this method automatically during login, passing the email as email. It returns UserDetails — Spring Security's standard representation of a user.
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)//Goes to the database and looks up the user by email. If no user exists with that email, throws UsernameNotFoundException — which AuthenticationManager catches and converts into a failed login.
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isActive(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );//convert User into a Spring Sec UserDetails object
    }
}