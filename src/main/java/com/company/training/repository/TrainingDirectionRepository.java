package com.company.training.repository;

import com.company.training.entity.TrainingDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrainingDirectionRepository extends JpaRepository<TrainingDirection, Long> {
    List<TrainingDirection> findAllByOrderByNameAsc();
    boolean existsByName(String name);
}