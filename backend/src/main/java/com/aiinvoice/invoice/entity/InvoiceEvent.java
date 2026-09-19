package com.aiinvoice.invoice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  @Column(length = 100)
  private String actor;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String metadata;

  public InvoiceEvent(UUID invoiceId, String eventType, String message) {
    this(invoiceId, eventType, message, "SYSTEM");
  }

  public InvoiceEvent(UUID invoiceId, String eventType, String message, String actor) {
    this.id = UUID.randomUUID();
    this.invoiceId = invoiceId;
    this.eventType = eventType;
    this.message = message;
    this.actor = actor;
    this.createdAt = Instant.now();
  }
}
