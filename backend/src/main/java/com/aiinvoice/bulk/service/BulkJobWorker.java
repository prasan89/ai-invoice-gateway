package com.aiinvoice.bulk.service;

import com.aiinvoice.bulk.entity.BulkJob;
import com.aiinvoice.bulk.entity.BulkJobItem;
import com.aiinvoice.bulk.repository.BulkJobItemRepository;
import com.aiinvoice.bulk.repository.BulkJobRepository;
import com.aiinvoice.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BulkJobWorker {

    private final BulkJobItemRepository itemRepo;
    private final BulkJobRepository jobRepo;
    private final InvoiceService invoiceService;

    @Scheduled(fixedDelayString = "${bulk.poll-interval-ms:5000}")
    @Transactional
    public void processItems() {
        List<BulkJobItem> items = itemRepo.claimPendingItems(10);
        if (items.isEmpty()) return;

        for (BulkJobItem item : items) {
            item.setStatus("PROCESSING");
            itemRepo.save(item);

            try {
                byte[] bytes = Files.readAllBytes(Paths.get(item.getFilePath()));
                StagedMultipartFile file = new StagedMultipartFile(bytes, item.getFileName());
                var result = invoiceService.createFromUpload(file);
                item.setStatus("COMPLETED");
                item.setInvoiceId(result.id());
            } catch (Exception e) {
                item.setStatus("FAILED");
                item.setErrorMessage(e.getMessage());
                log.error("Bulk item {} failed: {}", item.getId(), e.getMessage());
            }

            item.setProcessedAt(Instant.now());
            itemRepo.save(item);

            updateJobCounters(item);
        }
    }

    private void updateJobCounters(BulkJobItem item) {
        jobRepo.findById(item.getJobId()).ifPresent(job -> {
            int done = itemRepo.countByJobIdAndStatus(item.getJobId(), "COMPLETED");
            int failed = itemRepo.countByJobIdAndStatus(item.getJobId(), "FAILED");
            job.setProcessedCount(done);
            job.setFailedCount(failed);
            job.setUpdatedAt(Instant.now());
            if (done + failed >= job.getTotalCount()) {
                job.setStatus(failed == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS");
            } else {
                job.setStatus("PROCESSING");
            }
            jobRepo.save(job);
        });
    }

    private record StagedMultipartFile(byte[] content, String name)
            implements org.springframework.web.multipart.MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return "application/pdf"; }
        @Override public boolean isEmpty() { return content == null || content.length == 0; }
        @Override public long getSize() { return content == null ? 0 : content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }
}
