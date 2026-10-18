package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    Optional<User> findByUserIdAndDeletedAtIsNull(Long userId);
    List<User> findAllByDeletedAtIsNullOrderByCreatedAtDescUserIdDesc();
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhoneAndUserIdNot(String phone, Long userId);
    boolean existsByEmailAndUserIdNot(String email, Long userId);
}
