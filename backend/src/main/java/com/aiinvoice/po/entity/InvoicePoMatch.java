package com.aiinvoice.po.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoice_po_matches")
@Getter @Setter @NoArgsConstructor
public class InvoicePoMatch {

    @Id private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "po_id", nullable = false)
    private UUID poId;

    @Column(name = "grn_id")
    private UUID grnId;

    @Column(name = "match_type", nullable = false, length = 20)
    private String matchType;   // TWO_WAY, THREE_WAY

    @Column(name = "match_status", nullable = false, length = 30)
    private String matchStatus; // MATCHED, PARTIAL, OVER_BILLED, UNDER_BILLED, UNMATCHED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "discrepancies", columnDefinition = "jsonb")
    private String discrepancies;

    @Column(name = "matched_at", nullable = false)
    private Instant matchedAt;

    @Column(name = "matched_by")
    private String matchedBy;
}
