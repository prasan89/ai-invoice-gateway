package com.aiinvoice.po.controller;

import com.aiinvoice.po.dto.CreateGrnRequest;
import com.aiinvoice.po.dto.GoodsReceiptDto;
import com.aiinvoice.po.service.GoodsReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceiptController {

    private final GoodsReceiptService service;

    @PostMapping
    public ResponseEntity<GoodsReceiptDto> create(@RequestBody CreateGrnRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping
    public ResponseEntity<List<GoodsReceiptDto>> list() {
        return ResponseEntity.ok(service.listByOrg());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoodsReceiptDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
