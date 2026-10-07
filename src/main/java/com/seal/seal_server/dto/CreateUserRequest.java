package com.seal.seal_server.dto;

import com.seal.seal_server.model.UserRole;

public record CreateUserRequest(
        String fullName,
        String username,
        String password,
        UserRole role
) {

}
