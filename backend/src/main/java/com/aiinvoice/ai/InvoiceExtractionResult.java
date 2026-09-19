package com.aiinvoice.ai;

import com.aiinvoice.invoice.dto.InvoiceDto;

public record InvoiceExtractionResult(InvoiceDto invoice, double confidence) {}
