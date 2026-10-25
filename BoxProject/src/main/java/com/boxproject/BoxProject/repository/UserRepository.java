package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Serialize updates per user, including the first insert into player_locations.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.userId = :id and u.deletedAt is null")
    Optional<User> lockActiveLocationUser(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "role")
    Optional<User> findByUserIdAndDeletedAtIsNull(Long userId);
    List<User> findAllByDeletedAtIsNullOrderByCreatedAtDescUserIdDesc();
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhoneAndUserIdNot(String phone, Long userId);
    boolean existsByEmailAndUserIdNot(String email, Long userId);
}
