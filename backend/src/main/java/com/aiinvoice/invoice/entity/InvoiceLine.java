package com.aiinvoice.invoice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name="invoice_lines")
@Getter @Setter @NoArgsConstructor
public class InvoiceLine {
  @Id private UUID id;
  @ManyToOne(fetch=FetchType.LAZY)
  @JoinColumn(name="invoice_id", nullable=false)
  private Invoice invoice;

  private String description;
  @Column(name="hsn_sac") private String hsnSac;
  private BigDecimal quantity;
  @Column(name="unit_price") private BigDecimal unitPrice;
  private BigDecimal discount;
  @Column(name="taxable_value") private BigDecimal taxableValue;

  // Composite tax rate (legacy / fallback)
  @Column(name="tax_rate") private BigDecimal taxRate;
  @Column(name="tax_amount") private BigDecimal taxAmount;

  @Column(name="cgst_rate")   private BigDecimal cgstRate;
  @Column(name="cgst_amount") private BigDecimal cgstAmount;
  @Column(name="sgst_rate")   private BigDecimal sgstRate;
  @Column(name="sgst_amount") private BigDecimal sgstAmount;
  @Column(name="igst_rate")   private BigDecimal igstRate;
  @Column(name="igst_amount") private BigDecimal igstAmount;
  @Column(name="cess_rate")   private BigDecimal cessRate;
  @Column(name="cess_amount") private BigDecimal cessAmount;

  @Column(name="line_total") private BigDecimal lineTotal;
}
