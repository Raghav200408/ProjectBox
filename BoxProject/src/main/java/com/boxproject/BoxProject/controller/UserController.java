package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.model.AppOwnerRequestDTO;
import com.boxproject.BoxProject.model.VenueOwnerRequestDTO;
import com.boxproject.BoxProject.model.PlayerRequestDTO;
import com.boxproject.BoxProject.model.UserResponseDTO;
import com.boxproject.BoxProject.service.UserService;
import com.boxproject.BoxProject.validation.Create;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/appOwner")
    public ResponseEntity<UserResponseDTO> createAppOwner(@Validated(Create.class) @RequestBody AppOwnerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createAppOwner(request));
    }

    @PostMapping("/venueOwner")
    public ResponseEntity<UserResponseDTO> createVenueOwner(@Validated(Create.class) @RequestBody VenueOwnerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createVenueOwner(request));
    }

    @PostMapping({"", "/player"})
    public ResponseEntity<UserResponseDTO> createPlayer(@Validated(Create.class) @RequestBody PlayerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createPlayer(request));
    }



    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping("/appOwner/{userId}")
    public ResponseEntity<UserResponseDTO> updateAppOwner(
            @PathVariable Long userId, @Valid @RequestBody AppOwnerRequestDTO request) {
        return ResponseEntity.ok(userService.updateAppOwner(userId, request));
    }

    @PutMapping("/venueOwner/{userId}")
    public ResponseEntity<UserResponseDTO> updateVenueOwner(
            @PathVariable Long userId, @Valid @RequestBody VenueOwnerRequestDTO request) {
        return ResponseEntity.ok(userService.updateVenueOwner(userId, request));
    }

    @PutMapping("/player/{userId}")
    public ResponseEntity<UserResponseDTO> updatePlayer(
            @PathVariable Long userId, @Valid @RequestBody PlayerRequestDTO request) {
        return ResponseEntity.ok(userService.updatePlayer(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
