package com.aiinvoice.bulk.service;

import com.aiinvoice.bulk.dto.BulkJobDto;
import com.aiinvoice.bulk.entity.BulkJob;
import com.aiinvoice.bulk.entity.BulkJobItem;
import com.aiinvoice.bulk.repository.BulkJobItemRepository;
import com.aiinvoice.bulk.repository.BulkJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BulkJobService {

    private final BulkJobRepository jobRepo;
    private final BulkJobItemRepository itemRepo;

    @Value("${bulk.staging-dir:/tmp/bulk-staging}")
    private String stagingDir;

    @Transactional
    public BulkJobDto createJob(UUID organizationId, List<MultipartFile> files) throws IOException {
        Path base = Paths.get(stagingDir);
        Files.createDirectories(base);

        BulkJob job = new BulkJob();
        job.setId(UUID.randomUUID());
        job.setOrganizationId(organizationId);
        job.setStatus("PENDING");
        job.setTotalCount(files.size());
        job.setProcessedCount(0);
        job.setFailedCount(0);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        jobRepo.save(job);

        for (int i = 0; i < files.size(); i++) {
            MultipartFile f = files.get(i);
            Path dest = base.resolve(job.getId() + "_" + i + "_" + f.getOriginalFilename());
            f.transferTo(dest);

            BulkJobItem item = new BulkJobItem();
            item.setId(UUID.randomUUID());
            item.setJobId(job.getId());
            item.setFileName(f.getOriginalFilename());
            item.setFilePath(dest.toAbsolutePath().toString());
            item.setStatus("PENDING");
            item.setCreatedAt(Instant.now());
            itemRepo.save(item);
        }

        return toDto(job);
    }

    public BulkJobDto getProgress(UUID jobId, UUID organizationId) {
        BulkJob job = jobRepo.findByIdAndOrganizationId(jobId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk job not found"));
        return toDto(job);
    }

    private BulkJobDto toDto(BulkJob job) {
        int pct = job.getTotalCount() == 0 ? 0 :
                (int) Math.round((job.getProcessedCount() + job.getFailedCount()) * 100.0 / job.getTotalCount());
        return new BulkJobDto(job.getId(), job.getStatus(), job.getTotalCount(),
                job.getProcessedCount(), job.getFailedCount(), pct, job.getCreatedAt());
    }
}
