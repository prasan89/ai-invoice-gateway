package com.aiinvoice.invoice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoice_events")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceEvent {
  @Id
  private UUID id;

  @Column(name = "invoice_id", nullable = false)
  private UUID invoiceId;

  @Column(name = "event_type", nullable = false, length = 50)
  private String eventType;

  @Column(nullable = false, length = 2000)
  private String message;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public InvoiceEvent(UUID invoiceId, String eventType, String message) {
    this.id = UUID.randomUUID();
    this.invoiceId = invoiceId;
    this.eventType = eventType;
    this.message = message;
    this.createdAt = Instant.now();
  }
}
