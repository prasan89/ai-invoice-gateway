package com.aiinvoice.bulk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "bulk_jobs")
@Getter @Setter @NoArgsConstructor
public class BulkJob {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    @Column(name = "total_count") private int totalCount;
    @Column(name = "processed_count") private int processedCount;
    @Column(name = "failed_count") private int failedCount;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
