package com.aiinvoice.bulk.controller;

import com.aiinvoice.bulk.dto.BulkJobDto;
import com.aiinvoice.bulk.service.BulkJobService;
import com.aiinvoice.auth.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BulkJobController {

    private final BulkJobService bulkJobService;
    @PostMapping("/invoices/bulk")
    public ResponseEntity<BulkJobDto> uploadBulk(
            @RequestParam("files") List<MultipartFile> files) throws IOException {
        if (files.isEmpty() || files.size() > 100) {
            return ResponseEntity.badRequest().build();
        }
        BulkJobDto dto = bulkJobService.createJob(TenantContext.getOrDefault(), files);
        return ResponseEntity.accepted().body(dto);
    }

    @GetMapping("/bulk-jobs/{id}/progress")
    public BulkJobDto getProgress(@PathVariable UUID id) {
        return bulkJobService.getProgress(id, TenantContext.getOrDefault());
    }
}
