package com.aiinvoice.bulk.controller;

import com.aiinvoice.bulk.dto.BulkJobDto;
import com.aiinvoice.bulk.service.BulkJobService;
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
    private static final UUID DEMO_ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @PostMapping("/invoices/bulk")
    public ResponseEntity<BulkJobDto> uploadBulk(
            @RequestParam("files") List<MultipartFile> files) throws IOException {
        if (files.isEmpty() || files.size() > 100) {
            return ResponseEntity.badRequest().build();
        }
        BulkJobDto dto = bulkJobService.createJob(DEMO_ORG, files);
        return ResponseEntity.accepted().body(dto);
    }

    @GetMapping("/bulk-jobs/{id}/progress")
    public BulkJobDto getProgress(@PathVariable UUID id) {
        return bulkJobService.getProgress(id, DEMO_ORG);
    }
}
