package com.boxproject.BoxProject.model;

import com.boxproject.BoxProject.constants.UserStatus;
import com.boxproject.BoxProject.validation.Create;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class PlayerRequestDTO {

    @NotBlank(message = "Full name is required.")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "\\+?[1-9][0-9]{7,14}", message = "Enter 8 to 15 digits, optionally prefixed by +.")
    private String phone;

    @Email(message = "Enter a valid email address.")
    @Size(max = 255)
    private String email;

    @NotBlank(groups = Create.class, message = "Password is required.")
    @Size(min = 8, max = 72)
    @Pattern(regexp = "(?s).*\\S.*", message = "Password cannot be blank.")
    private String password;

    @Pattern(regexp = "active|blocked|deleted", message = "Status must be active, blocked or deleted.")
    private String status;

    @AssertTrue(message = "Use DELETE to delete a user.")
    public boolean isStatusAllowed() {
        return !UserStatus.DELETED.equals(status);
    }

    @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes.")
    public boolean isPasswordWithinEncodingLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
