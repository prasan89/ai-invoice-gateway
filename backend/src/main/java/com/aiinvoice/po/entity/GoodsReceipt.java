package com.aiinvoice.po.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goods_receipts")
@Getter @Setter @NoArgsConstructor
public class GoodsReceipt {

    @Id private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "po_id")
    private UUID poId;

    @Column(name = "grn_number", nullable = false)
    private String grnNumber;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "RECEIVED";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GrnLine> lines = new ArrayList<>();

    public void addLine(GrnLine line) {
        line.setGoodsReceipt(this);
        lines.add(line);
    }
}
