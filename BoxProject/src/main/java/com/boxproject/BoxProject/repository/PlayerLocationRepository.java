package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.PlayerLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerLocationRepository extends JpaRepository<PlayerLocation, Long> {
}
