package com.seal.seal_server.dto;

import com.seal.seal_server.model.UserRole;

public record UserResponse(
        Long id,
        String fullName,
        String username,
        UserRole role,
        boolean active
) {
}
