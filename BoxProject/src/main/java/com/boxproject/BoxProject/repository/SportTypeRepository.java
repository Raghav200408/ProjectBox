package com.boxproject.BoxProject.repository;
import com.boxproject.BoxProject.entity.SportType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SportTypeRepository extends JpaRepository<SportType, Long> {
    List<SportType> findAllByActiveTrueOrderByNameAscIdAsc();
}
