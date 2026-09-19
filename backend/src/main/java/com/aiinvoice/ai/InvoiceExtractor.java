package com.aiinvoice.ai;

import org.springframework.web.multipart.MultipartFile;

public interface InvoiceExtractor {
  InvoiceExtractionResult extract(MultipartFile document);
}
