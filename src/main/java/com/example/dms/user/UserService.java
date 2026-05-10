package com.example.dms.user;

import com.example.dms.exception.ResourceNotFoundException;
import com.example.dms.user.dto.UpdateProfileRequest;
import com.example.dms.user.dto.UserResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public UserResponse getMe(Authentication authentication){
        var user = getUserFromAuthentication(authentication);
        return toResponse(user);
    }

    public UserResponse updateMe(Authentication authentication, UpdateProfileRequest request){
        var user = getUserFromAuthentication(authentication);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        userRepository.save(user);
        return toResponse(user);
    }

    public List<UserResponse> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse updateRole(Long id, Role role){
        var user = findUserById(id);
        user.setRole(role);
        userRepository.save(user);
        return toResponse(user);
    }

    public UserResponse deactivateUser(Long id){
        var user = findUserById(id);
        user.setActive(false);
        userRepository.save(user);
        return toResponse(user);
    }

    private User findUserById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private User getUserFromAuthentication(Authentication authentication){
        String email = authentication.getName(); // getName() returns email in our app
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private UserResponse toResponse(User user){
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .active(user.isActive())
                .build();

    }
}
