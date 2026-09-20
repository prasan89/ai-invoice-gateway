package com.aiinvoice.copilot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "copilot_messages")
@Getter @Setter @NoArgsConstructor
public class CopilotMessage {
    @Id private UUID id;
    @Column(name = "session_id", nullable = false) private UUID sessionId;
    @Column(nullable = false) private String role;
    @Column(nullable = false, columnDefinition = "text") private String content;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") private String metadata;
    @Column(name = "created_at") private Instant createdAt;
}
