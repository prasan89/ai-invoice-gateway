package com.aiinvoice.webhook.event;

import java.util.UUID;

public record InvoiceApprovedEvent(UUID invoiceId, UUID organizationId, String supplierName, String totalAmount) {
}
