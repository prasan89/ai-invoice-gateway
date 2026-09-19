package com.aiinvoice.po.entity;

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
@Table(name = "purchase_orders")
@Getter @Setter @NoArgsConstructor
public class PurchaseOrder {

    @Id private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "po_number", nullable = false)
    private String poNumber;

    @Column(name = "supplier_gstin", length = 20)
    private String supplierGstin;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "po_date")
    private LocalDate poDate;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "OPEN";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PoLine> lines = new ArrayList<>();

    public void addLine(PoLine line) {
        line.setPurchaseOrder(this);
        lines.add(line);
    }
}
