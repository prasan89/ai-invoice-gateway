package com.aiinvoice.invoice.entity;

import com.aiinvoice.invoice.domain.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor
public class Invoice {
  @Id private UUID id;
  @Column(name="organization_id", nullable=false) private UUID organizationId;
  @Column(name="invoice_number", nullable=false) private String invoiceNumber;
  private LocalDate invoiceDate;
  private String currency;
  private String supplierName;
  private String supplierGstin;
  private String customerName;
  private String customerGstin;
  private BigDecimal subtotal;
  private BigDecimal taxAmount;
  private BigDecimal totalAmount;
  private BigDecimal extractionConfidence;
  @Enumerated(EnumType.STRING) private InvoiceStatus status;
  private String validationMessage;
  private Instant createdAt;
  private Instant updatedAt;

  @OneToMany(mappedBy="invoice", cascade=CascadeType.ALL, orphanRemoval=true)
  private List<InvoiceLine> lines = new ArrayList<>();

  public void addLine(InvoiceLine line) {
    line.setInvoice(this);
    lines.add(line);
  }
}
