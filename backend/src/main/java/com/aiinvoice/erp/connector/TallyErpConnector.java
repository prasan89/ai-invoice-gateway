package com.aiinvoice.erp.connector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Tally ERP connector via Tally XML Gateway (port 9000).
 * Tally accepts XML posts at http://<host>:9000 — this sends a TallyPrime-compatible
 * CREATEVCH (voucher creation) XML payload for the invoice.
 */
@Component
@Slf4j
public class TallyErpConnector implements ErpConnector {

    @Override
    public String getSystemName() { return "TALLY"; }

    @Override
    public ErpSyncResult push(UUID invoiceId, Map<String, Object> payload) {
        String host = (String) payload.getOrDefault("tally_host", "localhost");
        String port = (String) payload.getOrDefault("tally_port", "9000");
        String xml = buildTallyXml(payload);
        try {
            java.net.URL url = new java.net.URL("http://" + host + ":" + port);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            if (code == 200) {
                return new ErpSyncResult(true, "TALLY_" + invoiceId, null);
            }
            return new ErpSyncResult(false, null, "Tally returned HTTP " + code);
        } catch (Exception e) {
            log.warn("Tally ERP sync failed for invoice {}: {}", invoiceId, e.getMessage());
            return new ErpSyncResult(false, null, e.getMessage());
        }
    }

    private String buildTallyXml(Map<String, Object> p) {
        return """
            <ENVELOPE>
              <HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER>
              <BODY><IMPORTDATA><REQUESTDESC>
                <REPORTNAME>Vouchers</REPORTNAME>
                <STATICVARIABLES><SVCURRENTCOMPANY>%s</SVCURRENTCOMPANY></STATICVARIABLES>
              </REQUESTDESC>
              <REQUESTDATA><TALLYMESSAGE xmlns:UDF="TallyUDF">
                <VOUCHER VCHTYPE="Purchase" ACTION="Create">
                  <DATE>%s</DATE>
                  <PARTYLEDGERNAME>%s</PARTYLEDGERNAME>
                  <AMOUNT>%s</AMOUNT>
                  <NARRATION>Invoice %s from %s</NARRATION>
                </VOUCHER>
              </TALLYMESSAGE></REQUESTDATA></IMPORTDATA></BODY>
            </ENVELOPE>""".formatted(
                p.getOrDefault("company", "Default Company"),
                p.getOrDefault("invoiceDate", ""),
                p.getOrDefault("supplierName", ""),
                p.getOrDefault("totalAmount", "0"),
                p.getOrDefault("invoiceNumber", ""),
                p.getOrDefault("supplierName", ""));
    }
}
