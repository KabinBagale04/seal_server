package com.seal.seal_server.dto;

import com.seal.seal_server.model.UserRole;

public record LoginResponse(
        boolean success,
        Long userId,
        String fullName,
        UserRole role,
        String token,
        String message
) {
}
