package com.boxproject.BoxProject.model;

import com.boxproject.BoxProject.constants.UserRole;
import com.boxproject.BoxProject.constants.UserStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserResponseDTO {
    private Long userId;
    private String fullName;
    private String phone;
    private String email;
    private String role;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
