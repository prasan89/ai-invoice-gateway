package com.aiinvoice.invoice.controller;

import com.aiinvoice.invoice.dto.InvoiceDto;
import com.aiinvoice.invoice.dto.InvoiceEventDto;
import com.aiinvoice.invoice.dto.InvoiceReviewRequest;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.repository.InvoiceRepository;
import com.aiinvoice.invoice.service.InvoiceService;
import com.aiinvoice.invoice.service.InvoiceStorageService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@CrossOrigin(origins = "http://localhost:3000")
public class InvoiceController {
  private final InvoiceService service;
  private final InvoiceRepository repository;
  private final InvoiceStorageService storage;

  public InvoiceController(InvoiceService service, InvoiceRepository repository,
                           InvoiceStorageService storage) {
    this.service = service;
    this.repository = repository;
    this.storage = storage;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public InvoiceDto upload(@RequestPart("file") MultipartFile file) {
    return service.createFromUpload(file);
  }

  @GetMapping public List<InvoiceDto> list() { return service.findAll(); }

  @GetMapping("/{id}") public InvoiceDto get(@PathVariable UUID id) { return service.findById(id); }

  @GetMapping("/{id}/history")
  public List<InvoiceEventDto> history(@PathVariable UUID id) {
    return service.history(id);
  }

  @GetMapping("/{id}/export")
  public ResponseEntity<byte[]> export(@PathVariable UUID id) {
    byte[] body = service.exportCsv(id).getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"invoice-" + id + ".csv\"")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(body);
  }

  @PutMapping("/{id}/review")
  public InvoiceDto review(@PathVariable UUID id, @Valid @RequestBody InvoiceReviewRequest request) {
    return service.updateReview(id, request);
  }

  @PostMapping("/{id}/approve")
  public InvoiceDto approve(@PathVariable UUID id) { return service.approve(id); }

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
