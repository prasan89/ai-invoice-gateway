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
  private BigDecimal quantity;
  private BigDecimal unitPrice;
  private BigDecimal discount;
  private BigDecimal taxRate;
  private BigDecimal taxAmount;
  private BigDecimal lineTotal;
}
