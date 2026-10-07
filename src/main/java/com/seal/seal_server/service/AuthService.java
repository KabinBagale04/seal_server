package com.seal.seal_server.service;
// Auth service is used to authenticate the login request from the user
// and check with the database and send the response
// but our desktop can't communicate directly to the spring on https so we'll be using
// auth controller to manage https connection
// so authservice to validate and authcontroler to connect


import com.seal.seal_server.dto.LoginRequest;
import com.seal.seal_server.dto.LoginResponse;
import com.seal.seal_server.model.User;
import com.seal.seal_server.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

// the annotation's meaning: this class contains application/business login and should be managed by spring
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       SessionService sessionService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService=sessionService;
    }
    public LoginResponse login(LoginRequest request){
        Optional<User> optionalUser =
                userRepository.findByUsername(request.username());

        if(optionalUser.isEmpty()){
            return new LoginResponse(
                    false,
                    null,
                    null,
                    null,
                    null,
                    "Incorrect username or password"
            );
        }

        User user = optionalUser.get();

        boolean passwordMatches = passwordEncoder.matches(
                request.password(), user.getPasswordHash()
        );

        if(!passwordMatches){
            return new LoginResponse(
                    false,
                    null,
                    null,
                    null,
                    null,
                    "Incorrect username or password"
            );
        }

        if(!user.isActive()){
            return new LoginResponse(
                    false,
                    null,
                    null,
                    null,
                    null,
                    "Account is inactive"
            );
        }

        String token = sessionService.createSession(user.getId());
        return new LoginResponse(
                true,
                user.getId(),
                user.getFullName(),
                user.getRole(),
                token,
                null
        );
    }
}
