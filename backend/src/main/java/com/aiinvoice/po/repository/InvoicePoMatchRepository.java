package com.aiinvoice.po.repository;

import com.aiinvoice.po.entity.InvoicePoMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoicePoMatchRepository extends JpaRepository<InvoicePoMatch, UUID> {
    List<InvoicePoMatch> findByInvoiceId(UUID invoiceId);
    Optional<InvoicePoMatch> findFirstByInvoiceIdOrderByMatchedAtDesc(UUID invoiceId);
}
