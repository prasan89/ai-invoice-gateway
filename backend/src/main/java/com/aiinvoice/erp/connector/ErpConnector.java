package com.aiinvoice.erp.connector;

public interface ErpConnector {
    String getSystemName();
    ErpSyncResult push(java.util.UUID invoiceId, java.util.Map<String, Object> payload);
    record ErpSyncResult(boolean success, String externalRef, String errorDetail) {}
}
