package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.model.LoginRequestDTO;
import com.boxproject.BoxProject.model.LoginResponseDTO;
import com.boxproject.BoxProject.security.JwtUtil;
import com.boxproject.BoxProject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        var user = userService.login(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new LoginResponseDTO(jwtUtil.generateToken(user), "Bearer", jwtUtil.getExpirationSeconds(), user));
    }
}
