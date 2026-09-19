package com.aiinvoice.invoice.repository;

import com.aiinvoice.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
  @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines ORDER BY i.createdAt DESC")
  List<Invoice> findAllByOrderByCreatedAtDesc();

  @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.lines WHERE i.id = :id")
  Optional<Invoice> findByIdWithLines(UUID id);
}
