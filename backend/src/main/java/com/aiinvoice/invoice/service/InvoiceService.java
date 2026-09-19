package com.aiinvoice.invoice.service;

import com.aiinvoice.ai.InvoiceExtractionResult;
import com.aiinvoice.ai.InvoiceExtractor;
import com.aiinvoice.invoice.domain.InvoiceStatus;
import com.aiinvoice.invoice.dto.InvoiceDto;
import com.aiinvoice.invoice.dto.InvoiceLineDto;
import com.aiinvoice.invoice.entity.Invoice;
import com.aiinvoice.invoice.entity.InvoiceLine;
import com.aiinvoice.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {
  private final InvoiceRepository repository;
  private final InvoiceExtractor extractor;
  private final InvoiceValidationService validator;

  public InvoiceService(InvoiceRepository repository, InvoiceExtractor extractor,
                        InvoiceValidationService validator) {
    this.repository = repository;
    this.extractor = extractor;
    this.validator = validator;
  }

  public InvoiceDto createFromUpload(MultipartFile file) {
    if (file.isEmpty()) throw new IllegalArgumentException("Invoice file is empty");
    InvoiceExtractionResult result = extractor.extract(file);
    InvoiceDto dto = result.invoice();

    Invoice invoice = new Invoice();
    invoice.setId(UUID.randomUUID());
    invoice.setOrganizationId(UUID.nameUUIDFromBytes("demo-organization".getBytes()));
    invoice.setInvoiceNumber(dto.invoiceNumber());
    invoice.setInvoiceDate(dto.invoiceDate());
    invoice.setCurrency(dto.currency());
    invoice.setSupplierName(dto.supplierName());
    invoice.setSupplierGstin(dto.supplierGstin());
    invoice.setCustomerName(dto.customerName());
    invoice.setCustomerGstin(dto.customerGstin());
    invoice.setSubtotal(dto.subtotal());
    invoice.setTaxAmount(dto.taxAmount());
    invoice.setTotalAmount(dto.totalAmount());
    invoice.setExtractionConfidence(dto.extractionConfidence());
    invoice.setStatus(InvoiceStatus.REVIEW_REQUIRED);
    invoice.setCreatedAt(Instant.now());
    invoice.setUpdatedAt(Instant.now());

    if (dto.lines() != null) {
      for (InvoiceLineDto d : dto.lines()) {
        InvoiceLine l = new InvoiceLine();
        l.setId(UUID.randomUUID());
        l.setDescription(d.description());
        l.setQuantity(d.quantity());
        l.setUnitPrice(d.unitPrice());
        l.setDiscount(d.discount());
        l.setTaxRate(d.taxRate());
        l.setTaxAmount(d.taxAmount());
        l.setLineTotal(d.lineTotal());
        invoice.addLine(l);
      }
    }

    String validation = validator.validate(invoice);
    invoice.setValidationMessage(validation);
    if (validation != null) invoice.setStatus(InvoiceStatus.FAILED);
    return toDto(repository.save(invoice));
  }

  public List<InvoiceDto> findAll() {
    return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
  }

  public InvoiceDto findById(UUID id) {
    return toDto(repository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id)));
  }

  public InvoiceDto approve(UUID id) {
    Invoice invoice = repository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    String validation = validator.validate(invoice);
    if (validation != null) throw new IllegalArgumentException(validation);
    invoice.setStatus(InvoiceStatus.APPROVED);
    invoice.setUpdatedAt(Instant.now());
    return toDto(repository.save(invoice));
  }

  private InvoiceDto toDto(Invoice i) {
    return new InvoiceDto(i.getId(), i.getInvoiceNumber(), i.getInvoiceDate(), i.getCurrency(),
      i.getSupplierName(), i.getSupplierGstin(), i.getCustomerName(), i.getCustomerGstin(),
      i.getSubtotal(), i.getTaxAmount(), i.getTotalAmount(), i.getExtractionConfidence(),
      i.getStatus(), i.getValidationMessage(),
      i.getLines().stream().map(l -> new InvoiceLineDto(l.getId(), l.getDescription(),
        l.getQuantity(), l.getUnitPrice(), l.getDiscount(), l.getTaxRate(),
        l.getTaxAmount(), l.getLineTotal())).toList());
  }
}
