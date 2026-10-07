package com.seal.seal_server.service;

import com.seal.seal_server.controller.AdminUserController;
import com.seal.seal_server.dto.CreateUserRequest;
import com.seal.seal_server.dto.UserResponse;
import com.seal.seal_server.model.User;
import com.seal.seal_server.model.UserRole;
import com.seal.seal_server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminUserServiceTests {
    private final UserRepository repository = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SessionService sessions = new SessionService();
    private final AdminUserService service = new AdminUserService(repository, encoder, sessions);
    private final CreateUserRequest request = new CreateUserRequest(
            " Test Teacher ", " teacher ", "test-password", UserRole.TEACHER);

    @Test
    void adminBearerSessionCreatesUserWithHashedPassword() {
        User admin = new User();
        admin.setRole(UserRole.ADMIN);
        when(repository.findById(1L)).thenReturn(Optional.of(admin));
        when(repository.findByUsername("teacher")).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertTrue(encoder.matches("test-password", user.getPasswordHash()));
            assertNotEquals("test-password", user.getPasswordHash());
            user.setId(2L);
            return user;
        });
        var controller = new AdminUserController(service, sessions, repository);
        var result = controller.createUser("Bearer " + sessions.createSession(1L), request);
        assertEquals(201, result.getStatusCode().value());
        assertEquals(new UserResponse(2L, "Test Teacher", "teacher", UserRole.TEACHER, true), result.getBody());
        verify(repository, times(1)).save(any(User.class));
    }

    @Test
    void duplicateUsernameIsRejectedWithoutSaving() {
        when(repository.findByUsername("teacher")).thenReturn(Optional.of(new User()));
        assertThrows(IllegalArgumentException.class, () -> service.createUser(request));
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void incompleteRequestsAreRejectedWithoutDatabaseCalls() {
        for (CreateUserRequest invalid : new CreateUserRequest[] {
                null, new CreateUserRequest(null, "teacher", "pass", UserRole.TEACHER),
                new CreateUserRequest("Name", " ", "pass", UserRole.TEACHER),
                new CreateUserRequest("Name", "teacher", " ", UserRole.TEACHER),
                new CreateUserRequest("Name", "teacher", "pass", null)}) {
            assertThrows(IllegalArgumentException.class, () -> service.createUser(invalid));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void nonAdminSessionCannotCreateUser() {
        User student = new User();
        student.setRole(UserRole.STUDENT);
        when(repository.findById(3L)).thenReturn(Optional.of(student));
        var controller = new AdminUserController(service, sessions, repository);
        var response = controller.createUser("Bearer " + sessions.createSession(3L), request);
        assertEquals(403, response.getStatusCode().value());
        verify(repository, never()).save(any(User.class));
    }
}
