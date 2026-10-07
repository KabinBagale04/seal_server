package com.seal.seal_server.service;

import com.seal.seal_server.model.*;
import com.seal.seal_server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

/** The existing manual bearer-session check, shared only by the new exam endpoints. */
@Service
public class ExamAuthorization {
    private final SessionService sessions;
    private final UserRepository users;
    public ExamAuthorization(SessionService sessions, UserRepository users) {
        this.sessions = sessions;
        this.users = users;
    }
    public User require(String header, UserRole role) {
        if (header == null || !header.startsWith("Bearer ") || header.substring(7).isBlank())
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required.");
        Long id = sessions.getUserId(header.substring(7))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid session."));
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid session."));
        if (!user.isActive()) throw new ResponseStatusException(UNAUTHORIZED, "Account is inactive.");
        if (user.getRole() != role) throw new ResponseStatusException(FORBIDDEN, role + " access required.");
        return user;
    }
}
