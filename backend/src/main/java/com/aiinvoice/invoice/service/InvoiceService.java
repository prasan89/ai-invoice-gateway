package com.aiinvoice.invoice.service;

import com.aiinvoice.ai.InvoiceExtractionResult;
import com.aiinvoice.ai.InvoiceExtractor;
import com.aiinvoice.invoice.domain.InvoiceStatus;
import com.aiinvoice.invoice.dto.InvoiceDto;
import com.aiinvoice.invoice.dto.InvoiceLineDto;
import com.aiinvoice.invoice.dto.InvoiceReviewRequest;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import com.aiinvoice.invoice.repository.InvoiceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InvoiceService {
  private static final UUID DEMO_ORGANIZATION =
      UUID.nameUUIDFromBytes("demo-organization".getBytes());

  private final InvoiceRepository repository;
  private final InvoiceExtractor extractor;
  private final InvoiceValidationService validator;
  private final InvoiceStorageService storage;
  private final ObjectMapper mapper;

  public InvoiceService(InvoiceRepository repository, InvoiceExtractor extractor,
                        InvoiceValidationService validator, InvoiceStorageService storage,
                        ObjectMapper mapper) {
    this.repository = repository;
    this.extractor = extractor;
    this.validator = validator;
    this.storage = storage;
    this.mapper = mapper;
  }

  @Transactional
  public InvoiceDto createFromUpload(MultipartFile file) {
    if (file.isEmpty()) throw new IllegalArgumentException("Invoice file is empty");

    UUID id = UUID.randomUUID();
    Invoice invoice = new Invoice();
    invoice.setId(id);
    invoice.setOrganizationId(DEMO_ORGANIZATION);
    invoice.setStatus(InvoiceStatus.PROCESSING);
    invoice.setCreatedAt(Instant.now());
    invoice.setUpdatedAt(Instant.now());

    InvoiceStorageService.StoredDocument document = storage.store(id, file);
    invoice.setSourceFileName(document.fileName());
    invoice.setSourceContentType(document.contentType());
    invoice.setSourceStoragePath(document.storagePath());
    repository.save(invoice);

    InvoiceExtractionResult result;
    try {
      result = extractor.extract(file);
    } catch (RuntimeException e) {
      invoice.setStatus(InvoiceStatus.FAILED);
      invoice.setValidationMessage(e.getMessage());
      invoice.setUpdatedAt(Instant.now());
      repository.save(invoice);
      throw e;
    }

    copyExtracted(invoice, result.invoice());
    String validation = validator.validate(invoice);
    invoice.setValidationMessage(validation);
    invoice.setStatus(validation == null ? InvoiceStatus.REVIEW_REQUIRED : InvoiceStatus.FAILED);
    invoice.setUpdatedAt(Instant.now());
    return toDto(repository.save(invoice));
  }

  @Transactional
  public InvoiceDto updateReview(UUID id, InvoiceReviewRequest req) {
    Invoice invoice = repository.findByIdWithLines(id)
        .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));

    invoice.setInvoiceNumber(req.invoiceNumber());
    invoice.setInvoiceDate(req.invoiceDate());
    invoice.setCurrency(req.currency());
    invoice.setSupplierName(req.supplierName());
    invoice.setSupplierGstin(req.supplierGstin());
    invoice.setCustomerName(req.customerName());
    invoice.setCustomerGstin(req.customerGstin());
    invoice.setSubtotal(req.subtotal());
    invoice.setTaxAmount(req.taxAmount());
    invoice.setCgstAmount(req.cgstAmount());
    invoice.setSgstAmount(req.sgstAmount());
    invoice.setIgstAmount(req.igstAmount());
    invoice.setCessAmount(req.cessAmount());
    invoice.setTotalAmount(req.totalAmount());

    invoice.getLines().clear();
    if (req.lines() != null) {
      for (InvoiceLineDto d : req.lines()) {
        invoice.addLine(lineFromDto(d));
      }
    }

    String validation = validator.validate(invoice);
    invoice.setValidationMessage(validation);
    invoice.setStatus(validation == null ? InvoiceStatus.REVIEW_REQUIRED : InvoiceStatus.FAILED);
    invoice.setUpdatedAt(Instant.now());
    return toDto(repository.save(invoice));
  }

  public List<InvoiceDto> findAll() {
    return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
  }

  public InvoiceDto findById(UUID id) {
    return toDto(repository.findByIdWithLines(id)
      .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id)));
  }

  @Transactional
  public InvoiceDto approve(UUID id) {
    Invoice invoice = repository.findByIdWithLines(id)
      .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    String validation = validator.validate(invoice);
    if (validation != null) throw new IllegalArgumentException(validation);
    invoice.setStatus(InvoiceStatus.APPROVED);
    invoice.setUpdatedAt(Instant.now());
    return toDto(repository.save(invoice));
  }

  private void copyExtracted(Invoice invoice, InvoiceDto dto) {
    invoice.setInvoiceNumber(dto.invoiceNumber());
    invoice.setInvoiceDate(dto.invoiceDate());
    invoice.setCurrency(dto.currency());
    invoice.setSupplierName(dto.supplierName());
    invoice.setSupplierGstin(dto.supplierGstin());
    invoice.setCustomerName(dto.customerName());
    invoice.setCustomerGstin(dto.customerGstin());
    invoice.setSubtotal(dto.subtotal());
    invoice.setTaxAmount(dto.taxAmount());
    invoice.setCgstAmount(dto.cgstAmount());
    invoice.setSgstAmount(dto.sgstAmount());
    invoice.setIgstAmount(dto.igstAmount());
    invoice.setCessAmount(dto.cessAmount());
    invoice.setTotalAmount(dto.totalAmount());
    invoice.setExtractionConfidence(dto.extractionConfidence());

    try {
      invoice.setFieldConfidence(mapper.writeValueAsString(
          dto.fieldConfidence() == null ? Map.of() : dto.fieldConfidence()));
    } catch (Exception e) {
      throw new IllegalStateException("Could not store extraction confidence", e);
    }

    invoice.getLines().clear();
    if (dto.lines() != null) {
      for (InvoiceLineDto d : dto.lines()) {
        invoice.addLine(lineFromDto(d));
      }
    }
  }

  private InvoiceLine lineFromDto(InvoiceLineDto d) {
    InvoiceLine l = new InvoiceLine();
    l.setId(UUID.randomUUID());
    l.setDescription(d.description());
    l.setHsnSac(d.hsnSac());
    l.setQuantity(d.quantity());
    l.setUnitPrice(d.unitPrice());
    l.setDiscount(d.discount());
    l.setTaxableValue(d.taxableValue());
    l.setTaxRate(d.taxRate());
    l.setTaxAmount(d.taxAmount());
    l.setCgstRate(d.cgstRate());
    l.setCgstAmount(d.cgstAmount());
    l.setSgstRate(d.sgstRate());
    l.setSgstAmount(d.sgstAmount());
    l.setIgstRate(d.igstRate());
    l.setIgstAmount(d.igstAmount());
    l.setCessRate(d.cessRate());
    l.setCessAmount(d.cessAmount());
    l.setLineTotal(d.lineTotal());
    return l;
  }

  private InvoiceDto toDto(Invoice i) {
    Map<String, BigDecimal> confidence = new LinkedHashMap<>();
    if (i.getFieldConfidence() != null && !i.getFieldConfidence().isBlank()) {
      try {
        confidence.putAll(mapper.readValue(i.getFieldConfidence(),
            new TypeReference<Map<String, BigDecimal>>() {}));
      } catch (Exception ignored) {}
    }

    List<InvoiceLineDto> lines = i.getLines().stream().map(l -> new InvoiceLineDto(
        l.getId(), l.getDescription(), l.getHsnSac(),
        l.getQuantity(), l.getUnitPrice(), l.getDiscount(), l.getTaxableValue(),
        l.getTaxRate(), l.getTaxAmount(),
        l.getCgstRate(), l.getCgstAmount(),
        l.getSgstRate(), l.getSgstAmount(),
        l.getIgstRate(), l.getIgstAmount(),
        l.getCessRate(), l.getCessAmount(),
        l.getLineTotal()
    )).toList();

    String docUrl = i.getSourceStoragePath() != null
        ? "/api/v1/invoices/" + i.getId() + "/document" : null;

    return new InvoiceDto(
      i.getId(), i.getInvoiceNumber(), i.getInvoiceDate(), i.getCurrency(),
      i.getSupplierName(), i.getSupplierGstin(), i.getCustomerName(), i.getCustomerGstin(),
      i.getSubtotal(), i.getTaxAmount(),
      i.getCgstAmount(), i.getSgstAmount(), i.getIgstAmount(), i.getCessAmount(),
      i.getTotalAmount(), i.getExtractionConfidence(),
      i.getStatus(), i.getValidationMessage(), lines, confidence,
      i.getSourceFileName(), i.getSourceContentType(), docUrl);
  }
}
