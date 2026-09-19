package com.aiinvoice.po.repository;

import com.aiinvoice.po.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

    List<GoodsReceipt> findByOrganizationIdOrderByCreatedAtDesc(UUID orgId);

    Optional<GoodsReceipt> findByOrganizationIdAndGrnNumber(UUID orgId, String grnNumber);

    @Query("SELECT g FROM GoodsReceipt g LEFT JOIN FETCH g.lines WHERE g.id = :id")
    Optional<GoodsReceipt> findByIdWithLines(UUID id);

    List<GoodsReceipt> findByPoId(UUID poId);
}
