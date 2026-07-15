package com.aigateway.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agent_tools", schema = "aigateway")
public class AgentToolEntity {

    @EmbeddedId
    private AgentToolId id;

    @MapsId("agentId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AgentEntity agent;

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentToolId implements Serializable {

        @Column(name = "agent_id", length = 128)
        private String agentId;

        @Column(name = "tool_name", length = 128)
        private String toolName;
    }
}
