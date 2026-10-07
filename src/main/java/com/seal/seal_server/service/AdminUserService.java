package com.seal.seal_server.service;

import com.seal.seal_server.dto.CreateUserRequest;
import com.seal.seal_server.dto.UserResponse;
import com.seal.seal_server.model.User;
import com.seal.seal_server.model.UserRole;
import com.seal.seal_server.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private  final SessionService sessionService;

    public AdminUserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SessionService sessionService
    ){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.sessionService = sessionService;
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (request == null
                || request.fullName() == null || request.fullName().isBlank()
                || request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()
                || request.role() == null) {
            throw new IllegalArgumentException("Full name, username, password and role are required.");
        }

        if (request.role() == UserRole.ADMIN)
            throw new IllegalArgumentException("Only TEACHER and STUDENT accounts can be created.");
        String username = request.username().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);

        User savedUser = userRepository.save(user);
        return new UserResponse(
                savedUser.getId(), savedUser.getFullName(), savedUser.getUsername(),
                savedUser.getRole(), savedUser.isActive()
        );
    }

    public List<UserResponse> getUsers() {
// findall gives us list
        //stream lets us process these users as a pipeline
        //map converts user entity to safe userresponse DTO
        //list collects the result info
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getUsername(),
                        user.getRole(),
                        user.isActive()
                ))
                .toList();
    }

    public UserResponse setUserActive(Long userId,boolean active) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new IllegalArgumentException("User not found")
        );
        if(user.getRole()==UserRole.ADMIN){
            throw  new IllegalArgumentException("Admin account cannot be modified here");
        }
        user.setActive(active);
        User savedUser = userRepository.save(user);

        if (!active) sessionService.removeUserSessions(userId);
        return new UserResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getUsername(),
                savedUser.getRole(),
                savedUser.isActive()
        );
    }
}
