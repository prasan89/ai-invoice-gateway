package com.aiinvoice.po.service;

import com.aiinvoice.po.dto.CreateGrnRequest;
import com.aiinvoice.po.dto.GoodsReceiptDto;
import com.aiinvoice.po.dto.GrnLineDto;
import com.aiinvoice.po.entity.GoodsReceipt;
import com.aiinvoice.po.entity.GrnLine;
import com.aiinvoice.po.repository.GoodsReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoodsReceiptService {

    private static final UUID DEMO_ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final GoodsReceiptRepository grnRepository;

    @Transactional
    public GoodsReceiptDto create(CreateGrnRequest req) {
        GoodsReceipt grn = new GoodsReceipt();
        grn.setId(UUID.randomUUID());
        grn.setOrganizationId(DEMO_ORG);
        grn.setPoId(req.poId());
        grn.setGrnNumber(req.grnNumber());
        grn.setReceiptDate(req.receiptDate());
        grn.setNotes(req.notes());
        grn.setStatus("RECEIVED");
        grn.setCreatedAt(Instant.now());
        grn.setUpdatedAt(Instant.now());

        if (req.lines() != null) {
            for (CreateGrnRequest.GrnLineRequest lr : req.lines()) {
                GrnLine line = new GrnLine();
                line.setId(UUID.randomUUID());
                line.setPoLineId(lr.poLineId());
                line.setLineNumber(lr.lineNumber());
                line.setDescription(lr.description());
                line.setHsnSac(lr.hsnSac());
                line.setQuantityReceived(lr.quantityReceived());
                line.setUnitPrice(lr.unitPrice());
                grn.addLine(line);
            }
        }

        return toDto(grnRepository.save(grn));
    }

    public List<GoodsReceiptDto> listByOrg() {
        return grnRepository.findAll().stream()
            .filter(g -> DEMO_ORG.equals(g.getOrganizationId()))
            .map(this::toDto)
            .toList();
    }

    public GoodsReceiptDto getById(UUID id) {
        return grnRepository.findByIdWithLines(id)
            .map(this::toDto)
            .orElseThrow(() -> new NoSuchElementException("GRN not found: " + id));
    }

    private GoodsReceiptDto toDto(GoodsReceipt grn) {
        List<GrnLineDto> lineDtos = grn.getLines().stream()
            .map(l -> new GrnLineDto(l.getId(), l.getPoLineId(), l.getLineNumber(),
                l.getDescription(), l.getHsnSac(), l.getQuantityReceived(), l.getUnitPrice()))
            .toList();
        return new GoodsReceiptDto(grn.getId(), grn.getPoId(), grn.getGrnNumber(),
            grn.getReceiptDate(), grn.getStatus(), grn.getNotes(), lineDtos);
    }
}
