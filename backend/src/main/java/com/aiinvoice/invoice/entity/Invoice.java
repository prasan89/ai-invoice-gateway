package com.aiinvoice.invoice.entity;

import com.aiinvoice.invoice.domain.ArithmeticStatus;
import com.aiinvoice.invoice.domain.GstinValidationStatus;
import com.aiinvoice.invoice.domain.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
  @Column(name="cgst_amount") private BigDecimal cgstAmount;
  @Column(name="sgst_amount") private BigDecimal sgstAmount;
  @Column(name="igst_amount") private BigDecimal igstAmount;
  @Column(name="cess_amount") private BigDecimal cessAmount;
  private BigDecimal totalAmount;
  private BigDecimal extractionConfidence;
  @Enumerated(EnumType.STRING) private InvoiceStatus status;
  private String validationMessage;
  private Instant createdAt;
  private Instant updatedAt;

  @Column(name="source_file_name") private String sourceFileName;
  @Column(name="source_content_type") private String sourceContentType;
  @Column(name="source_storage_path") private String sourceStoragePath;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name="field_confidence", columnDefinition="jsonb")
  private String fieldConfidence;

  @Column(name="document_hash") private String documentHash;

  @Enumerated(EnumType.STRING)
  @Column(name="arithmetic_status") private ArithmeticStatus arithmeticStatus;

  @Column(name="duplicate_score") private Integer duplicateScore;

  @Column(name="duplicate_invoice_id") private UUID duplicateInvoiceId;

  @Column(name="duplicate_label") private String duplicateLabel;

  // Phase 5.2
  @Column(name="duplicate_reason") private String duplicateReason;

  // Phase 5.1 — cached GST portal result
  @Column(name="supplier_legal_name")    private String supplierLegalName;
  @Column(name="supplier_portal_status") private String supplierPortalStatus;
  @Column(name="supplier_trade_name")    private String supplierTradeName;

  // Phase 6
  @Column(name="po_match_status") private String poMatchStatus;
  @Column(name="matched_po_id")   private UUID matchedPoId;

  @Enumerated(EnumType.STRING)
  @Column(name="supplier_gstin_status") private GstinValidationStatus supplierGstinStatus;

  @Enumerated(EnumType.STRING)
  @Column(name="customer_gstin_status") private GstinValidationStatus customerGstinStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="vendor_id") private Vendor vendor;

  @OneToMany(mappedBy="invoice", cascade=CascadeType.ALL, orphanRemoval=true)
  private List<InvoiceLine> lines = new ArrayList<>();

  public void addLine(InvoiceLine line) {
    line.setInvoice(this);
    lines.add(line);
  }
}
