package com.aiinvoice.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "erp_connections")
@Getter @Setter @NoArgsConstructor
public class ErpConnection {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "erp_system", nullable = false) private String erpSystem;
    @Column(name = "display_name", nullable = false) private String displayName;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") private String config;
    @Column(nullable = false) private String status = "DISCONNECTED";
    @Column(name = "last_synced_at") private Instant lastSyncedAt;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
