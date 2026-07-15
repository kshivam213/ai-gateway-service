package com.aigateway.persistence.repository;

import com.aigateway.persistence.entity.SessionMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface SessionMessageRepository extends JpaRepository<SessionMessageEntity, Long> {

    List<SessionMessageEntity> findBySessionIdOrderBySequenceNumAsc(String sessionId);

    @Modifying
    @Query("DELETE FROM SessionMessageEntity m WHERE m.sessionId = :sessionId")
    void deleteBySessionId(String sessionId);
}
