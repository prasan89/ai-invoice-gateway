package com.aiinvoice.invoice.controller;

import com.aiinvoice.invoice.domain.InvoiceStatus;
import com.aiinvoice.invoice.dto.DashboardStatsDto;
import com.aiinvoice.invoice.dto.InvoiceDto;
import com.aiinvoice.invoice.dto.InvoiceEventDto;
import com.aiinvoice.invoice.dto.RejectRequest;
import com.aiinvoice.invoice.dto.InvoiceReviewRequest;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.repository.InvoiceRepository;
import com.aiinvoice.invoice.service.InvoiceService;
import com.aiinvoice.invoice.service.InvoiceStorageService;
import com.aiinvoice.po.service.PoMatchingService;
import com.aiinvoice.security.FileTypeValidator;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@CrossOrigin(origins = "http://localhost:3000")
public class InvoiceController {
  private final InvoiceService service;
  private final InvoiceRepository repository;
  private final InvoiceStorageService storage;
  private final PoMatchingService poMatchingService;
  private final FileTypeValidator fileTypeValidator;

  public InvoiceController(InvoiceService service, InvoiceRepository repository,
                           InvoiceStorageService storage, PoMatchingService poMatchingService,
                           FileTypeValidator fileTypeValidator) {
    this.service = service;
    this.repository = repository;
    this.storage = storage;
    this.poMatchingService = poMatchingService;
    this.fileTypeValidator = fileTypeValidator;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<InvoiceDto> upload(@RequestPart("file") MultipartFile file) {
    if (!fileTypeValidator.isAllowed(file)) {
      return ResponseEntity.status(415).build();
    }
    return ResponseEntity.ok(service.createFromUpload(file));
  }

  @GetMapping
  public List<InvoiceDto> list(
      @RequestParam(required = false) InvoiceStatus status,
      @RequestParam(required = false) String supplierGstin,
      @RequestParam(required = false) String invoiceNumber,
      @RequestParam(required = false) String search) {
    return service.findAll(status, supplierGstin, invoiceNumber, search);
  }

  @GetMapping("/stats")
  public DashboardStatsDto stats() {
    return service.getDashboardStats();
  }

  @GetMapping("/{id}")
  public InvoiceDto get(@PathVariable UUID id) { return service.findById(id); }

  @GetMapping("/{id}/history")
  public List<InvoiceEventDto> history(@PathVariable UUID id) {
    return service.history(id);
  }

  @GetMapping("/{id}/export")
  public ResponseEntity<byte[]> export(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "csv") String format) {
    return service.export(id, format);
  }

  @PutMapping("/{id}/review")
  public InvoiceDto review(@PathVariable UUID id, @Valid @RequestBody InvoiceReviewRequest request,
                           @RequestHeader(value = "X-User-Name", required = false) String actor) {
    return service.updateReview(id, request, actor);
  }

  @PostMapping("/{id}/approve")
  public InvoiceDto approve(@PathVariable UUID id,
                            @RequestHeader(value = "X-User-Name", required = false) String actor) {
    return service.approve(id, actor);
  }

  @PostMapping("/{id}/reject")
  public InvoiceDto reject(@PathVariable UUID id,
                            @RequestBody(required = false) RejectRequest req,
                            @RequestHeader(value = "X-User-Name", required = false) String actor) {
    return service.reject(id, req != null ? req.reason() : null, actor);
  }

  @PostMapping("/{id}/reprocess")
  public InvoiceDto reprocess(@PathVariable UUID id) {
    return service.reprocess(id);
  }

  @PostMapping("/{id}/match-po")
  public PoMatchingService.MatchResult matchPo(
      @PathVariable UUID id,
      @RequestParam UUID poId,
      @RequestParam(required = false) UUID grnId) {
    if (grnId != null) {
      return poMatchingService.matchThreeWay(id, poId, grnId);
    }
    return poMatchingService.matchTwoWay(id, poId);
  }

  @GetMapping("/{id}/document")
  public ResponseEntity<Resource> document(@PathVariable UUID id) {
    Invoice invoice = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    Resource resource = storage.load(invoice.getSourceStoragePath());

    MediaType mediaType;
    try {
      mediaType = MediaType.parseMediaType(invoice.getSourceContentType());
    } catch (Exception e) {
      mediaType = MediaType.APPLICATION_OCTET_STREAM;
    }

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"" + invoice.getSourceFileName() + "\"")
        .contentType(mediaType)
        .body(resource);
  }
}
