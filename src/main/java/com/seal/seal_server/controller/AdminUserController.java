package com.seal.seal_server.controller;

import com.seal.seal_server.dto.CreateUserRequest;
import com.seal.seal_server.dto.UpdateUserStatusRequest;
import com.seal.seal_server.dto.UserResponse;
import com.seal.seal_server.model.User;
import com.seal.seal_server.model.UserRole;
import com.seal.seal_server.repository.UserRepository;
import com.seal.seal_server.service.AdminUserService;
import com.seal.seal_server.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;
    private final SessionService sessionService;
    private final UserRepository userRepository;

    public AdminUserController(
            AdminUserService adminUserService,
            SessionService sessionService,
            UserRepository userRepository
    ) {
        this.adminUserService = adminUserService;
        this.sessionService = sessionService;
        this.userRepository = userRepository;
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @RequestBody UpdateUserStatusRequest request
    ) {

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            return ResponseEntity
                    .status(401)
                    .body("Authentication required.");
        }

        String token = authorizationHeader.substring(7);

        Optional<Long> adminId =
                sessionService.getUserId(token);

        if (adminId.isEmpty()) {
            return ResponseEntity
                    .status(401)
                    .body("Invalid session.");
        }

        Optional<User> currentUser =
                userRepository.findById(adminId.get());

        if (currentUser.isEmpty()) {
            return ResponseEntity
                    .status(401)
                    .body("Invalid session.");
        }

        if (currentUser.get().getRole() != UserRole.ADMIN) {
            return ResponseEntity
                    .status(403)
                    .body("Admin access required.");
        }

        try {

            UserResponse updatedUser =
                    adminUserService.setUserActive(
                            id,
                            request.active()
                    );

            return ResponseEntity.ok(updatedUser);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestHeader(value = "Authorization",required = false)
            String authorizationHeader,
            @RequestBody CreateUserRequest request
            ){
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            return ResponseEntity.status(401).body("Authentication required");
        }
        String token = authorizationHeader.substring(7);
        Optional<Long> userId =sessionService.getUserId(token);

        if(userId.isEmpty()){
            return ResponseEntity.status(401).body("Invalid session.");
        }

        Optional<User> currentUser =
                userRepository.findById(userId.get());

        if (currentUser.isEmpty()) {
            return ResponseEntity
                    .status(401)
                    .body("Invalid session.");
        }

        //auth only admin can create accounts
        if (currentUser.get().getRole() != UserRole.ADMIN) {
            return ResponseEntity
                    .status(403)
                    .body("Admin access required.");
        }

        try {
            UserResponse createdUser =
                    adminUserService.createUser(request);

            return ResponseEntity
                    .status(201)
                    .body(createdUser);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUsers(
        @RequestHeader(value = "Authorization",required = false)
    String authorizationHeader
    ){
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            return ResponseEntity
                    .status(401)
                    .body("Authentication required.");
        }

        String token = authorizationHeader.substring(7);

        Optional<Long> userId =
                sessionService.getUserId(token);

        if (userId.isEmpty()) {
            return ResponseEntity
                    .status(401)
                    .body("Invalid session.");
        }

        Optional<User> currentUser =
                userRepository.findById(userId.get());

        if (currentUser.isEmpty()) {
            return ResponseEntity
                    .status(401)
                    .body("Invalid session.");
        }

        if (currentUser.get().getRole() != UserRole.ADMIN) {
            return ResponseEntity
                    .status(403)
                    .body("Admin access required.");
        }

        List<UserResponse> users =
                adminUserService.getUsers();

        return ResponseEntity.ok(users);
    }
}
