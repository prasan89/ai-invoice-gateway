package com.aiinvoice.invoice.controller;

import com.aiinvoice.invoice.dto.InvoiceDto;
import com.aiinvoice.invoice.service.InvoiceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@CrossOrigin(origins = "http://localhost:3000")
public class InvoiceController {
  private final InvoiceService service;
  public InvoiceController(InvoiceService service) { this.service = service; }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public InvoiceDto upload(@RequestPart("file") MultipartFile file) {
    return service.createFromUpload(file);
  }

  @GetMapping public List<InvoiceDto> list() { return service.findAll(); }

  @GetMapping("/{id}") public InvoiceDto get(@PathVariable UUID id) { return service.findById(id); }

  @PostMapping("/{id}/approve")
  public InvoiceDto approve(@PathVariable UUID id) { return service.approve(id); }
}
