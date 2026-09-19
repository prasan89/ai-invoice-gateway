package com.aiinvoice.bulk.repository;

import com.aiinvoice.bulk.entity.BulkJobItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface BulkJobItemRepository extends JpaRepository<BulkJobItem, UUID> {

    @Query(value = """
        SELECT * FROM bulk_job_items
        WHERE status = 'PENDING'
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<BulkJobItem> claimPendingItems(@Param("limit") int limit);

    int countByJobIdAndStatus(UUID jobId, String status);
}
