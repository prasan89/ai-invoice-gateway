package com.aiinvoice.po.controller;

import com.aiinvoice.po.dto.CreatePoRequest;
import com.aiinvoice.po.dto.PurchaseOrderDto;
import com.aiinvoice.po.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @PostMapping
    public ResponseEntity<PurchaseOrderDto> create(@RequestBody CreatePoRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderDto>> list() {
        return ResponseEntity.ok(service.listByOrg());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
