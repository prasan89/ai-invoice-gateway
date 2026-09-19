package com.aiinvoice.invoice.service;

import com.aiinvoice.ai.InvoiceExtractionResult;
import com.aiinvoice.ai.InvoiceExtractor;
import com.aiinvoice.invoice.domain.ArithmeticStatus;
import com.aiinvoice.invoice.domain.GstinValidationStatus;
import com.aiinvoice.invoice.domain.InvoiceStatus;
import com.aiinvoice.invoice.domain.ValidationResult;
import com.aiinvoice.invoice.dto.DashboardStatsDto;
import com.aiinvoice.invoice.dto.InvoiceDto;
import com.aiinvoice.invoice.dto.InvoiceEventDto;
import com.aiinvoice.invoice.dto.InvoiceLineDto;
import com.aiinvoice.invoice.dto.InvoiceReviewRequest;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceEvent;
import com.aiinvoice.invoice.entity.InvoiceLine;
import com.aiinvoice.invoice.entity.Vendor;
import com.aiinvoice.invoice.repository.InvoiceEventRepository;
import com.aiinvoice.invoice.repository.InvoiceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
  private final InvoiceEventRepository eventRepository;
  private final InvoiceExtractor extractor;
  private final InvoiceValidationService validator;
  private final InvoiceStorageService storage;
  private final InvoiceDuplicateService duplicateService;
  private final InvoiceArithmeticService arithmeticService;
  private final GstinValidationService gstinValidator;
  private final VendorService vendorService;
  private final InvoiceRuleEngine ruleEngine;
  private final ObjectMapper mapper;

  public InvoiceService(InvoiceRepository repository,
                        InvoiceEventRepository eventRepository,
                        InvoiceExtractor extractor,
                        InvoiceValidationService validator,
                        InvoiceStorageService storage,
                        InvoiceDuplicateService duplicateService,
                        InvoiceArithmeticService arithmeticService,
                        GstinValidationService gstinValidator,
                        VendorService vendorService,
                        InvoiceRuleEngine ruleEngine,
                        ObjectMapper mapper) {
    this.repository = repository;
    this.eventRepository = eventRepository;
    this.extractor = extractor;
    this.validator = validator;
    this.storage = storage;
    this.duplicateService = duplicateService;
    this.arithmeticService = arithmeticService;
    this.gstinValidator = gstinValidator;
    this.vendorService = vendorService;
    this.ruleEngine = ruleEngine;
    this.mapper = mapper;
  }

  @Transactional
  public InvoiceDto createFromUpload(MultipartFile file) {
    if (file.isEmpty()) throw new IllegalArgumentException("Invoice file is empty");

    // Read bytes first — MultipartFile stream can only be consumed once
    byte[] fileBytes;
    try {
      fileBytes = file.getBytes();
    } catch (Exception e) {
      throw new IllegalStateException("Could not read invoice file", e);
    }

    UUID id = UUID.randomUUID();
    Invoice invoice = new Invoice();
    invoice.setId(id);
    invoice.setOrganizationId(DEMO_ORGANIZATION);
    invoice.setStatus(InvoiceStatus.PROCESSING);
    invoice.setCreatedAt(Instant.now());
    invoice.setUpdatedAt(Instant.now());

    // Compute hash before extraction
    invoice.setDocumentHash(duplicateService.computeHash(fileBytes));

    InvoiceStorageService.StoredDocument document = storage.store(id, file);
    invoice.setSourceFileName(document.fileName());
    invoice.setSourceContentType(document.contentType());
    invoice.setSourceStoragePath(document.storagePath());
    repository.save(invoice);
    record(invoice, "UPLOADED", "Invoice document uploaded: " + document.fileName());

    InvoiceExtractionResult result;
    try {
      result = extractor.extract(file);
    } catch (RuntimeException e) {
      invoice.setStatus(InvoiceStatus.FAILED);
      invoice.setValidationMessage(e.getMessage());
      invoice.setUpdatedAt(Instant.now());
      repository.save(invoice);
      record(invoice, "EXTRACTION_FAILED", e.getMessage());
      throw e;
    }

    copyExtracted(invoice, result.invoice());
    record(invoice, "EXTRACTED",
        "AI extraction completed with confidence " + invoice.getExtractionConfidence() + "%");

    // Phase 4: GSTIN validation
    invoice.setSupplierGstinStatus(gstinValidator.validate(invoice.getSupplierGstin()));
    invoice.setCustomerGstinStatus(gstinValidator.validate(invoice.getCustomerGstin()));

    // Phase 4: Arithmetic check
    InvoiceArithmeticService.ArithmeticCheckResult arith = arithmeticService.check(invoice);
    invoice.setArithmeticStatus(arith.status());

    // Phase 4: Duplicate detection
    InvoiceDuplicateService.DuplicateCheckResult dup = duplicateService.check(invoice);
    invoice.setDuplicateScore(dup.score());
    invoice.setDuplicateInvoiceId(dup.duplicateInvoiceId());

    // Phase 4: Vendor matching
    Vendor vendor = vendorService.matchOrCreate(
        invoice.getSupplierGstin(), invoice.getSupplierName(), invoice.getTotalAmount());
    invoice.setVendor(vendor);

    // Phase 3.5: Structured validation
    List<ValidationResult> validationResults = validator.validate(invoice);
    boolean failed = validator.hasFailures(validationResults);
    String firstFailure = validationResults.stream()
        .filter(ValidationResult::isFailed).map(ValidationResult::message).findFirst().orElse(null);
    invoice.setValidationMessage(firstFailure);
    invoice.setStatus(determineStatus(invoice, failed));
    invoice.setUpdatedAt(Instant.now());

    InvoiceDto saved = toDto(repository.save(invoice), validationResults);
    String eventType = failed ? "VALIDATION_FAILED" : "VALIDATED";
    String eventMsg = failed ? ("Validation failed: " + firstFailure) : "Deterministic invoice validation passed";
    record(invoice, eventType, eventMsg);
    return saved;
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

    // Re-run all checks after manual edit
    invoice.setSupplierGstinStatus(gstinValidator.validate(invoice.getSupplierGstin()));
    invoice.setCustomerGstinStatus(gstinValidator.validate(invoice.getCustomerGstin()));
    InvoiceArithmeticService.ArithmeticCheckResult arith = arithmeticService.check(invoice);
    invoice.setArithmeticStatus(arith.status());
    InvoiceDuplicateService.DuplicateCheckResult dup = duplicateService.check(invoice);
    invoice.setDuplicateScore(dup.score());
    invoice.setDuplicateInvoiceId(dup.duplicateInvoiceId());

    List<ValidationResult> validationResults = validator.validate(invoice);
    boolean failed = validator.hasFailures(validationResults);
    String firstFailure = validationResults.stream()
        .filter(ValidationResult::isFailed).map(ValidationResult::message).findFirst().orElse(null);
    invoice.setValidationMessage(firstFailure);
    invoice.setStatus(failed ? InvoiceStatus.FAILED : InvoiceStatus.REVIEW_REQUIRED);
    invoice.setUpdatedAt(Instant.now());

    InvoiceDto saved = toDto(repository.save(invoice), validationResults);
    record(invoice, "REVIEW_SAVED",
        failed ? "Review saved but validation failed: " + firstFailure
               : "Review changes saved and validation passed");
    return saved;
  }

  public List<InvoiceDto> findAll(InvoiceStatus status, String supplierGstin,
                                   String invoiceNumber, String search) {
    String s = (search != null && search.isBlank()) ? null : search;
    String sg = (supplierGstin != null && supplierGstin.isBlank()) ? null : supplierGstin;
    String in = (invoiceNumber != null && invoiceNumber.isBlank()) ? null : invoiceNumber;
    if (status == null && sg == null && in == null && s == null) {
      return repository.findAllByOrderByCreatedAtDesc().stream()
          .map(i -> toDto(i, null)).toList();
    }
    return repository.search(DEMO_ORGANIZATION, status, sg, in, s).stream()
        .map(i -> toDto(i, null)).toList();
  }

  public InvoiceDto findById(UUID id) {
    return toDto(repository.findByIdWithLines(id)
      .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id)), null);
  }

  public List<InvoiceEventDto> history(UUID id) {
    if (!repository.existsById(id)) {
      throw new IllegalArgumentException("Invoice not found: " + id);
    }
    return eventRepository.findByInvoiceIdOrderByCreatedAtDesc(id).stream()
        .map(e -> new InvoiceEventDto(e.getId(), e.getEventType(), e.getMessage(),
            e.getCreatedAt(), e.getActor(), null))
        .toList();
  }

  @Transactional
  public InvoiceDto approve(UUID id) {
    Invoice invoice = repository.findByIdWithLines(id)
      .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    invoice.getStatus().transitionTo(InvoiceStatus.APPROVED);

    List<ValidationResult> validationResults = validator.validate(invoice);
    if (validator.hasFailures(validationResults)) {
      String msg = validationResults.stream()
          .filter(ValidationResult::isFailed).map(ValidationResult::message).findFirst().orElse("Validation failed");
      record(invoice, "APPROVAL_BLOCKED", msg);
      throw new IllegalArgumentException(msg);
    }
    invoice.setStatus(InvoiceStatus.APPROVED);
    invoice.setUpdatedAt(Instant.now());
    InvoiceDto saved = toDto(repository.save(invoice), validationResults);
    record(invoice, "APPROVED", "Invoice approved after validation");
    return saved;
  }

  @Transactional
  public InvoiceDto reject(UUID id, String reason) {
    Invoice invoice = repository.findByIdWithLines(id)
        .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    invoice.getStatus().transitionTo(InvoiceStatus.REJECTED);
    invoice.setStatus(InvoiceStatus.REJECTED);
    invoice.setUpdatedAt(Instant.now());
    InvoiceDto saved = toDto(repository.save(invoice), null);
    record(invoice, "REJECTED", reason != null && !reason.isBlank() ? reason : "Invoice rejected by reviewer");
    return saved;
  }

  public DashboardStatsDto getDashboardStats() {
    Object[] row = repository.dashboardStats(DEMO_ORGANIZATION);
    return new DashboardStatsDto(
        toLong(row[0]),
        row[1] instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) row[1]).doubleValue()),
        toLong(row[2]), toLong(row[3]), toLong(row[4]), toLong(row[5]), toLong(row[6]), toLong(row[7]),
        row[8] == null ? BigDecimal.ZERO
            : (row[8] instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) row[8]).doubleValue()))
    );
  }

  @Transactional
  public ResponseEntity<byte[]> export(UUID id, String format) {
    if ("json".equalsIgnoreCase(format)) {
      InvoiceDto dto = findById(id);
      try {
        byte[] body = mapper.writeValueAsBytes(dto);
        Invoice inv = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        record(inv, "EXPORTED", "Invoice exported as JSON");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"invoice-" + id + ".json\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body);
      } catch (Exception e) {
        throw new IllegalStateException("Could not serialize invoice to JSON", e);
      }
    }
    // CSV (default)
    Invoice invoice = repository.findByIdWithLines(id)
        .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    byte[] body = buildCsv(invoice).getBytes(StandardCharsets.UTF_8);
    record(invoice, "EXPORTED", "Invoice exported as CSV");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"invoice-" + id + ".csv\"")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(body);
  }

  private InvoiceStatus determineStatus(Invoice invoice, boolean validationFailed) {
    if (validationFailed) return InvoiceStatus.FAILED;
    double confidence = invoice.getExtractionConfidence() == null
        ? 0 : invoice.getExtractionConfidence().doubleValue();
    boolean eligible = confidence >= 95.0
        && invoice.getArithmeticStatus() == ArithmeticStatus.PASS
        && (invoice.getDuplicateScore() == null || invoice.getDuplicateScore() < 80)
        && invoice.getSupplierGstinStatus() == GstinValidationStatus.VALID
        && invoice.getCustomerGstinStatus() == GstinValidationStatus.VALID
        && invoice.getTotalAmount() != null
        && invoice.getTotalAmount().compareTo(new BigDecimal("500000")) < 0;
    return eligible ? InvoiceStatus.AUTO_APPROVED : InvoiceStatus.REVIEW_REQUIRED;
  }

  private long toLong(Object o) {
    if (o == null) return 0L;
    return ((Number) o).longValue();
  }

  private void record(Invoice invoice, String type, String message) {
    eventRepository.save(new InvoiceEvent(invoice.getId(), type,
        message == null || message.isBlank() ? type : message));
  }

  private String buildCsv(Invoice invoice) {
    StringBuilder csv = new StringBuilder();
    csv.append("invoice_number,invoice_date,currency,supplier_name,supplier_gstin,")
       .append("customer_name,customer_gstin,subtotal,cgst,sgst,igst,cess,tax,total,")
       .append("status,line_description,hsn_sac,quantity,unit_price,discount,taxable_value,")
       .append("tax_rate,line_tax,cgst_rate,cgst_amount,sgst_rate,sgst_amount,")
       .append("igst_rate,igst_amount,cess_rate,cess_amount,line_total\n");
    if (invoice.getLines().isEmpty()) {
      appendCsvRow(csv, invoice, null);
    } else {
      for (InvoiceLine line : invoice.getLines()) {
        appendCsvRow(csv, invoice, line);
      }
    }
    return csv.toString();
  }

  private void appendCsvRow(StringBuilder csv, Invoice invoice, InvoiceLine line) {
    csv.append(csvCell(invoice.getInvoiceNumber())).append(',')
       .append(csvCell(invoice.getInvoiceDate())).append(',')
       .append(csvCell(invoice.getCurrency())).append(',')
       .append(csvCell(invoice.getSupplierName())).append(',')
       .append(csvCell(invoice.getSupplierGstin())).append(',')
       .append(csvCell(invoice.getCustomerName())).append(',')
       .append(csvCell(invoice.getCustomerGstin())).append(',')
       .append(csvCell(invoice.getSubtotal())).append(',')
       .append(csvCell(invoice.getCgstAmount())).append(',')
       .append(csvCell(invoice.getSgstAmount())).append(',')
       .append(csvCell(invoice.getIgstAmount())).append(',')
       .append(csvCell(invoice.getCessAmount())).append(',')
       .append(csvCell(invoice.getTaxAmount())).append(',')
       .append(csvCell(invoice.getTotalAmount())).append(',')
       .append(csvCell(invoice.getStatus())).append(',');
    if (line != null) {
      csv.append(csvCell(line.getDescription())).append(',')
         .append(csvCell(line.getHsnSac())).append(',')
         .append(csvCell(line.getQuantity())).append(',')
         .append(csvCell(line.getUnitPrice())).append(',')
         .append(csvCell(line.getDiscount())).append(',')
         .append(csvCell(line.getTaxableValue())).append(',')
         .append(csvCell(line.getTaxRate())).append(',')
         .append(csvCell(line.getTaxAmount())).append(',')
         .append(csvCell(line.getCgstRate())).append(',')
         .append(csvCell(line.getCgstAmount())).append(',')
         .append(csvCell(line.getSgstRate())).append(',')
         .append(csvCell(line.getSgstAmount())).append(',')
         .append(csvCell(line.getIgstRate())).append(',')
         .append(csvCell(line.getIgstAmount())).append(',')
         .append(csvCell(line.getCessRate())).append(',')
         .append(csvCell(line.getCessAmount())).append(',')
         .append(csvCell(line.getLineTotal()));
    }
    csv.append('\n');
  }

  private String csvCell(Object value) {
    if (value == null) return "";
    String text = String.valueOf(value);
    if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
    return "\"" + text.replace("\"", "\"\"") + "\"";
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
      for (InvoiceLineDto d : dto.lines()) invoice.addLine(lineFromDto(d));
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

  private InvoiceDto toDto(Invoice i, List<ValidationResult> validationResults) {
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

    UUID vendorId = i.getVendor() != null ? i.getVendor().getId() : null;
    String vendorName = i.getVendor() != null ? i.getVendor().getNormalizedName() : null;

    return new InvoiceDto(
      i.getId(), i.getInvoiceNumber(), i.getInvoiceDate(), i.getCurrency(),
      i.getSupplierName(), i.getSupplierGstin(), i.getCustomerName(), i.getCustomerGstin(),
      i.getSubtotal(), i.getTaxAmount(),
      i.getCgstAmount(), i.getSgstAmount(), i.getIgstAmount(), i.getCessAmount(),
      i.getTotalAmount(), i.getExtractionConfidence(),
      i.getStatus(), i.getValidationMessage(), lines, confidence,
      i.getSourceFileName(), i.getSourceContentType(), docUrl,
      validationResults,
      i.getSupplierGstinStatus(), i.getCustomerGstinStatus(),
      i.getArithmeticStatus(),
      i.getDuplicateScore(), i.getDuplicateInvoiceId(),
      vendorId, vendorName);
  }
}
