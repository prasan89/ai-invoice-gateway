package com.aiinvoice.po.service;

import com.aiinvoice.po.dto.CreatePoRequest;
import com.aiinvoice.po.dto.PoLineDto;
import com.aiinvoice.po.dto.PurchaseOrderDto;
import com.aiinvoice.po.entity.PoLine;
import com.aiinvoice.po.entity.PurchaseOrder;
import com.aiinvoice.po.repository.PurchaseOrderRepository;
import com.aiinvoice.auth.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;

    @Transactional
    public PurchaseOrderDto create(CreatePoRequest req) {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(UUID.randomUUID());
        po.setOrganizationId(TenantContext.getOrDefault());
        po.setPoNumber(req.poNumber());
        po.setSupplierGstin(req.supplierGstin());
        po.setSupplierName(req.supplierName());
        po.setPoDate(req.poDate());
        po.setCurrency(req.currency() != null ? req.currency() : "INR");
        po.setNotes(req.notes());
        po.setStatus("OPEN");
        po.setCreatedAt(Instant.now());
        po.setUpdatedAt(Instant.now());

        BigDecimal total = BigDecimal.ZERO;
        if (req.lines() != null) {
            for (CreatePoRequest.PoLineRequest lr : req.lines()) {
                PoLine line = new PoLine();
                line.setId(UUID.randomUUID());
                line.setLineNumber(lr.lineNumber());
                line.setDescription(lr.description());
                line.setHsnSac(lr.hsnSac());
                line.setQuantity(lr.quantity());
                line.setUnitPrice(lr.unitPrice());
                line.setTaxRate(lr.taxRate());
                BigDecimal lineTotal = lr.unitPrice().multiply(lr.quantity());
                if (lr.taxRate() != null) {
                    lineTotal = lineTotal.multiply(BigDecimal.ONE.add(
                        lr.taxRate().divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP)));
                }
                line.setLineTotal(lineTotal.setScale(2, java.math.RoundingMode.HALF_UP));
                total = total.add(line.getLineTotal());
                po.addLine(line);
            }
        }
        po.setTotalAmount(total.setScale(2, java.math.RoundingMode.HALF_UP));

        return toDto(poRepository.save(po));
    }

    public List<PurchaseOrderDto> listByOrg() {
        return poRepository.findAll().stream()
            .filter(p -> TenantContext.getOrDefault().equals(p.getOrganizationId()))
            .map(this::toDto)
            .toList();
    }

    public PurchaseOrderDto getById(UUID id) {
        return poRepository.findByIdWithLines(id)
            .map(this::toDto)
            .orElseThrow(() -> new NoSuchElementException("PO not found: " + id));
    }

    private PurchaseOrderDto toDto(PurchaseOrder po) {
        List<PoLineDto> lineDtos = po.getLines().stream()
            .map(l -> new PoLineDto(l.getId(), l.getLineNumber(), l.getDescription(),
                l.getHsnSac(), l.getQuantity(), l.getUnitPrice(), l.getTaxRate(), l.getLineTotal()))
            .toList();
        return new PurchaseOrderDto(po.getId(), po.getPoNumber(), po.getSupplierGstin(),
            po.getSupplierName(), po.getPoDate(), po.getCurrency(), po.getTotalAmount(),
            po.getStatus(), po.getNotes(), lineDtos);
    }
}
