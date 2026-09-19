package com.aiinvoice.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "erp_sync_jobs")
@Getter @Setter @NoArgsConstructor
public class ErpSyncJob {
    @Id private UUID id;
    @Column(name = "invoice_id", nullable = false) private UUID invoiceId;
    @Column(name = "erp_system", nullable = false, length = 40) private String erpSystem;
    @Column(name = "sync_status", nullable = false, length = 20) private String syncStatus = "PENDING";
    @Column(name = "synced_at") private Instant syncedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") private String response;
    @Column(name = "error_detail", columnDefinition = "TEXT") private String errorDetail;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
