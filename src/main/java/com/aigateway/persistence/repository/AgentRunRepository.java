package com.aigateway.persistence.repository;

import com.aigateway.persistence.entity.AgentRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRunEntity, String> {

    List<AgentRunEntity> findByAgentIdOrderByCreatedAtDesc(String agentId);

    List<AgentRunEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
