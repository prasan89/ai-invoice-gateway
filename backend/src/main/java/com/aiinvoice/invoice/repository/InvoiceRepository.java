package com.aiinvoice.invoice.repository;

import com.aiinvoice.invoice.domain.InvoiceStatus;
import com.aiinvoice.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

  @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines WHERE i.organizationId = :orgId ORDER BY i.createdAt DESC")
  List<Invoice> findAllWithLinesByOrgId(@Param("orgId") UUID orgId);

  @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines ORDER BY i.createdAt DESC")
  List<Invoice> findAllByOrderByCreatedAtDesc();

  @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.lines WHERE i.id = :id AND i.organizationId = :orgId")
  Optional<Invoice> findByIdWithLinesAndOrganizationId(@Param("id") UUID id, @Param("orgId") UUID orgId);

  @Query("""
    SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines
    WHERE i.organizationId = :orgId
      AND (:status IS NULL OR i.status = :status)
      AND (:supplierGstin IS NULL OR i.supplierGstin = :supplierGstin)
      AND (:invoiceNumber IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%',:invoiceNumber,'%')))
      AND (:search IS NULL OR
            LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%',:search,'%')) OR
            LOWER(COALESCE(i.supplierName,'')) LIKE LOWER(CONCAT('%',:search,'%')) OR
            LOWER(COALESCE(i.customerName,'')) LIKE LOWER(CONCAT('%',:search,'%')) OR
            LOWER(COALESCE(i.supplierGstin,'')) LIKE LOWER(CONCAT('%',:search,'%')))
    ORDER BY i.createdAt DESC
    """)
  List<Invoice> search(
    @Param("orgId") UUID orgId,
    @Param("status") InvoiceStatus status,
    @Param("supplierGstin") String supplierGstin,
    @Param("invoiceNumber") String invoiceNumber,
    @Param("search") String search);

  @Query("""
    SELECT
      COUNT(i),
      COALESCE(SUM(i.totalAmount), 0),
      SUM(CASE WHEN i.status = 'REVIEW_REQUIRED' THEN 1 ELSE 0 END),
      SUM(CASE WHEN i.status = 'APPROVED' THEN 1 ELSE 0 END),
      SUM(CASE WHEN i.status = 'FAILED' THEN 1 ELSE 0 END),
      SUM(CASE WHEN i.status = 'REJECTED' THEN 1 ELSE 0 END),
      SUM(CASE WHEN i.status = 'AUTO_APPROVED' THEN 1 ELSE 0 END),
      SUM(CASE WHEN i.duplicateScore >= 80 THEN 1 ELSE 0 END),
      AVG(i.extractionConfidence)
    FROM Invoice i
    WHERE i.organizationId = :orgId
    """)
  List<Object[]> dashboardStats(@Param("orgId") UUID orgId);

  @Query("""
    SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines
    WHERE i.organizationId = :orgId AND i.status = 'REVIEW_REQUIRED'
    ORDER BY
      CASE WHEN i.duplicateScore >= 80 THEN 0 ELSE 1 END ASC,
      CASE WHEN i.arithmeticStatus = 'FAIL' THEN 0 ELSE 1 END ASC,
      i.extractionConfidence ASC NULLS LAST,
      i.totalAmount DESC NULLS LAST,
      i.createdAt ASC
    """)
  List<Invoice> findReviewQueueOrdered(@Param("orgId") UUID orgId);

  Optional<Invoice> findFirstByDocumentHashAndIdNot(String documentHash, UUID id);

  @Query("""
    SELECT i FROM Invoice i
    WHERE i.organizationId = :orgId
      AND i.id <> :excludeId
      AND i.status <> 'REJECTED'
      AND (i.invoiceNumber = :invNum OR i.supplierGstin = :gstin)
    """)
  List<Invoice> findDuplicateCandidates(
    @Param("orgId") UUID orgId,
    @Param("excludeId") UUID excludeId,
    @Param("invNum") String invNum,
    @Param("gstin") String gstin);
}
