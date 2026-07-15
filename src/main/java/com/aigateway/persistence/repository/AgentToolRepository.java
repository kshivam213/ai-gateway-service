package com.aigateway.persistence.repository;

import com.aigateway.persistence.entity.AgentToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentToolRepository extends JpaRepository<AgentToolEntity, Long> {

    List<AgentToolEntity> findByAgentId(String agentId);

    @Modifying
    @Query("DELETE FROM AgentToolEntity a WHERE a.agentId = :agentId")
    void deleteByAgentId(String agentId);
}
