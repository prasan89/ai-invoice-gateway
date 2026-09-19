package com.aiinvoice.webhook.event;

import java.util.UUID;

public record InvoiceRejectedEvent(UUID invoiceId, UUID organizationId, String reason) {
}
