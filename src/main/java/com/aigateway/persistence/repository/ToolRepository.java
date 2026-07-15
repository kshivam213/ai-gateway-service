package com.aigateway.persistence.repository;

import com.aigateway.persistence.entity.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolRepository extends JpaRepository<ToolEntity, Long> {

    java.util.Optional<ToolEntity> findByName(String name);
}
