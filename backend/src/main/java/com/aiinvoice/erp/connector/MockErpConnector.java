package com.aiinvoice.erp.connector;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "erp.system", havingValue = "mock", matchIfMissing = true)
public class MockErpConnector implements ErpConnector {
    @Override public String getSystemName() { return "MOCK"; }
    @Override public ErpSyncResult push(UUID invoiceId, Map<String, Object> payload) {
        return new ErpSyncResult(true, "MOCK-" + invoiceId.toString().substring(0, 8), null);
    }
}
