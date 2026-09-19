package com.aiinvoice.invoice.repository;

import com.aiinvoice.invoice.entity.InvoiceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceEventRepository extends JpaRepository<InvoiceEvent, UUID> {
  List<InvoiceEvent> findByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
}
