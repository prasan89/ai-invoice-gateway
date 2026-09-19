package com.aiinvoice.po.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "grn_lines")
@Getter @Setter @NoArgsConstructor
public class GrnLine {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @Column(name = "po_line_id")
    private UUID poLineId;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "hsn_sac", length = 20)
    private String hsnSac;

    @Column(name = "quantity_received", nullable = false)
    private BigDecimal quantityReceived;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;
}
