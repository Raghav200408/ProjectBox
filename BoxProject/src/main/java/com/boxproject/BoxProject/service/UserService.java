package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.entity.User;
import com.boxproject.BoxProject.entity.Role;
import com.boxproject.BoxProject.constants.UserRole;
import com.boxproject.BoxProject.constants.UserStatus;
import com.boxproject.BoxProject.exception.DuplicateUserException;
import com.boxproject.BoxProject.exception.UserNotFoundException;
import com.boxproject.BoxProject.model.AppOwnerRequestDTO;
import com.boxproject.BoxProject.model.VenueOwnerRequestDTO;
import com.boxproject.BoxProject.model.PlayerRequestDTO;
import com.boxproject.BoxProject.model.UserResponseDTO;
import com.boxproject.BoxProject.repository.UserRepository;
import com.boxproject.BoxProject.repository.RoleRepository;
import com.boxproject.BoxProject.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.roleRepository = roleRepository;
    }

    public UserResponseDTO createAppOwner(AppOwnerRequestDTO request) {
        return createUser(request.getFullName(), request.getPhone(), request.getEmail(), request.getPassword(), UserRole.APP_OWNER);
    }

    public UserResponseDTO createVenueOwner(VenueOwnerRequestDTO request) {
        return createUser(request.getFullName(), request.getPhone(), request.getEmail(), request.getPassword(), UserRole.VENUE_OWNER);
    }

    public UserResponseDTO createPlayer(PlayerRequestDTO request) {
        return createUser(request.getFullName(), request.getPhone(), request.getEmail(), request.getPassword(), UserRole.PLAYER);
    }

    private UserResponseDTO createUser(String fullName, String requestedPhone, String requestedEmail, String password, String role) {
        String email = normalizeEmail(requestedEmail);
        String phone = normalizePhone(requestedPhone);
        validateUniqueContact(null, phone, email);

        User user = new User();
        user.setFullName(fullName.trim());
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(passwordHasher.hash(password));
        Role assignedRole = roleRepository.findByName(role).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(role);
            return roleRepository.saveAndFlush(newRole);
        });
        user.setRole(assignedRole);
        user.setStatus(UserStatus.ACTIVE);

        return convertToResponse(userRepository.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAllByDeletedAtIsNullOrderByCreatedAtDescUserIdDesc()
                .stream().map(this::convertToResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long userId) {
        return convertToResponse(getExistingUser(userId));
    }

    public UserResponseDTO updateAppOwner(Long userId, AppOwnerRequestDTO request) {
        return updateUser(userId, UserRole.APP_OWNER, request.getFullName(), request.getPhone(), request.getEmail(), request.getPassword(), request.getStatus());
    }

    public UserResponseDTO updateVenueOwner(Long userId, VenueOwnerRequestDTO request) {
        return updateUser(userId, UserRole.VENUE_OWNER, request.getFullName(), request.getPhone(), request.getEmail(), request.getPassword(), request.getStatus());
    }

    public UserResponseDTO updatePlayer(Long userId, PlayerRequestDTO request) {
        return updateUser(userId, UserRole.PLAYER, request.getFullName(), request.getPhone(), request.getEmail(), request.getPassword(), request.getStatus());
    }

    private UserResponseDTO updateUser(Long userId, String expectedRole, String fullName, String requestedPhone,
                                      String requestedEmail, String password, String status) {
        User user = getExistingUser(userId);
        if (!Objects.equals(user.getRole().getName(), expectedRole)) throw new UserNotFoundException(userId);
        String email = normalizeEmail(requestedEmail);
        String phone = normalizePhone(requestedPhone);
        validateUniqueContact(userId, phone, email);

        user.setFullName(fullName.trim());
        user.setPhone(phone);
        user.setEmail(email);
        if (password != null) user.setPasswordHash(passwordHasher.hash(password));
        if (status != null) user.setStatus(status);

        return convertToResponse(userRepository.saveAndFlush(user));
    }

    public void deleteUser(Long userId) {
        User user = getExistingUser(userId);
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }

    private User getExistingUser(Long userId) {
        return userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void validateUniqueContact(Long userId, String phone, String email) {
        boolean phoneExists = userId == null ? userRepository.existsByPhone(phone)
                : userRepository.existsByPhoneAndUserIdNot(phone, userId);
        if (phoneExists) throw new DuplicateUserException("Phone number is already registered.");

        if (email != null) {
            boolean emailExists = userId == null ? userRepository.existsByEmail(email)
                    : userRepository.existsByEmailAndUserIdNot(email, userId);
            if (emailExists) throw new DuplicateUserException("Email address is already registered.");
        }
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        // Treat international numbers with and without the optional + as the same contact.
        return "+" + phone.replaceFirst("^\\+", "");
    }

    private UserResponseDTO convertToResponse(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().getName());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
