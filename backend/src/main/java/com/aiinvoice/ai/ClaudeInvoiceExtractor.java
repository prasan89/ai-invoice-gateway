package com.aiinvoice.ai;

import com.aiinvoice.invoice.domain.InvoiceStatus;
import com.aiinvoice.invoice.dto.InvoiceDto;
import com.aiinvoice.invoice.dto.InvoiceLineDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "claude")
public class ClaudeInvoiceExtractor implements InvoiceExtractor {

  private static final String PROMPT = """
      You are an Indian GST invoice extraction engine.
      Extract every field from the supplied invoice document and return JSON only — no markdown, no explanation.
      Never invent a value. If a field is absent or unclear, return null.
      Preserve exact numeric values as they appear on the invoice; do not recalculate.
      Dates must be ISO-8601 yyyy-MM-dd.

      Header fields to extract:
        invoiceNumber, invoiceDate, currency (default INR),
        supplierName, supplierGstin, supplierAddress,
        customerName, customerGstin, customerAddress,
        placeOfSupply, reverseCharge (boolean),
        taxableSubtotal (sum of all line taxable values),
        cgstAmount (total CGST), sgstAmount (total SGST),
        igstAmount (total IGST), cessAmount (total Cess),
        taxAmount (total tax = cgst+sgst+igst+cess),
        totalAmount (grand total including tax),
        amountInWords.

      Line item fields to extract for each line:
        description, hsnSac, quantity, unitPrice, discount,
        taxableValue (= quantity * unitPrice - discount),
        cgstRate, cgstAmount, sgstRate, sgstAmount,
        igstRate, igstAmount, cessRate, cessAmount,
        taxRate (total GST rate = cgstRate + sgstRate or igstRate),
        taxAmount (line tax = cgst+sgst+igst+cess for that line),
        lineTotal (= taxableValue + line tax).

      For every extracted field provide a confidence score 0-100 based on clarity in the document.

      Return a single JSON object matching this exact structure:
      {
        "invoiceNumber": string|null,
        "invoiceDate": string|null,
        "currency": string|null,
        "supplierName": string|null,
        "supplierGstin": string|null,
        "supplierAddress": string|null,
        "customerName": string|null,
        "customerGstin": string|null,
        "customerAddress": string|null,
        "placeOfSupply": string|null,
        "reverseCharge": boolean|null,
        "taxableSubtotal": number|null,
        "cgstAmount": number|null,
        "sgstAmount": number|null,
        "igstAmount": number|null,
        "cessAmount": number|null,
        "taxAmount": number|null,
        "totalAmount": number|null,
        "amountInWords": string|null,
        "lines": [
          {
            "description": string|null,
            "hsnSac": string|null,
            "quantity": number|null,
            "unitPrice": number|null,
            "discount": number|null,
            "taxableValue": number|null,
            "taxRate": number|null,
            "taxAmount": number|null,
            "cgstRate": number|null,
            "cgstAmount": number|null,
            "sgstRate": number|null,
            "sgstAmount": number|null,
            "igstRate": number|null,
            "igstAmount": number|null,
            "cessRate": number|null,
            "cessAmount": number|null,
            "lineTotal": number|null,
            "confidence": number
          }
        ],
        "fieldConfidence": {
          "invoiceNumber": number,
          "invoiceDate": number,
          "currency": number,
          "supplierName": number,
          "supplierGstin": number,
          "customerName": number,
          "customerGstin": number,
          "taxableSubtotal": number,
          "cgstAmount": number,
          "sgstAmount": number,
          "igstAmount": number,
          "cessAmount": number,
          "taxAmount": number,
          "totalAmount": number,
          "lines": number
        }
      }
      """;

  private final ObjectMapper mapper;
  private final HttpClient client;
  private final String apiKey;
  private final String model;

  public ClaudeInvoiceExtractor(ObjectMapper mapper,
      @Value("${claude.api-key:}") String apiKey,
      @Value("${claude.model:claude-sonnet-4-6}") String model) {
    this.mapper = mapper;
    this.apiKey = apiKey;
    this.model = model;
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
  }

  @Override
  public InvoiceExtractionResult extract(MultipartFile document) {
    if (apiKey == null || apiKey.isBlank())
      throw new IllegalStateException(
          "CLAUDE_API_KEY is not configured. Set AI_PROVIDER=demo to use the demo extractor.");

    try {
      String mime = document.getContentType() == null ? "application/pdf" : document.getContentType();
      String base64 = Base64.getEncoder().encodeToString(document.getBytes());

      // Claude messages API — image or document content block
      Map<String, Object> contentBlock = new LinkedHashMap<>();
      if (mime.startsWith("image/")) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", "base64");
        source.put("media_type", mime);
        source.put("data", base64);
        contentBlock.put("type", "image");
        contentBlock.put("source", source);
      } else {
        // PDF via document block (claude-3.5+)
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", "base64");
        source.put("media_type", mime);
        source.put("data", base64);
        contentBlock.put("type", "document");
        contentBlock.put("source", source);
      }

      Map<String, Object> textBlock = Map.of("type", "text", "text", PROMPT);
      Map<String, Object> message = Map.of(
          "role", "user",
          "content", List.of(contentBlock, textBlock));

      Map<String, Object> request = new LinkedHashMap<>();
      request.put("model", model);
      request.put("max_tokens", 4096);
      request.put("messages", List.of(message));

      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create("https://api.anthropic.com/v1/messages"))
          .timeout(Duration.ofSeconds(120))
          .header("x-api-key", apiKey)
          .header("anthropic-version", "2023-06-01")
          .header("anthropic-beta", "pdfs-2024-09-25")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request)))
          .build();

      HttpResponse<String> response =
          client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() / 100 != 2)
        throw new IllegalStateException(
            "Claude extraction failed: HTTP " + response.statusCode() + " " + response.body());

      JsonNode root = mapper.readTree(response.body());
      String output = root.path("content").get(0).path("text").asText("");

      // Strip markdown code fences if Claude wraps the JSON
      output = output.strip();
      if (output.startsWith("```")) {
        output = output.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
      }

      if (output.isBlank())
        throw new IllegalStateException("Claude returned no structured invoice data");

      return toResult(mapper.readTree(output));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Claude extraction was interrupted", e);
    } catch (Exception e) {
      if (e instanceof IllegalStateException ise) throw ise;
      throw new IllegalStateException("Claude extraction failed: " + e.getMessage(), e);
    }
  }

  private InvoiceExtractionResult toResult(JsonNode n) {
    List<InvoiceLineDto> lines = new ArrayList<>();
    for (JsonNode l : n.path("lines")) {
      lines.add(new InvoiceLineDto(
          null,
          text(l, "description"), text(l, "hsnSac"),
          decimal(l, "quantity"), decimal(l, "unitPrice"), decimal(l, "discount"),
          decimal(l, "taxableValue"),
          decimal(l, "taxRate"), decimal(l, "taxAmount"),
          decimal(l, "cgstRate"), decimal(l, "cgstAmount"),
          decimal(l, "sgstRate"), decimal(l, "sgstAmount"),
          decimal(l, "igstRate"), decimal(l, "igstAmount"),
          decimal(l, "cessRate"), decimal(l, "cessAmount"),
          decimal(l, "lineTotal")));
    }

    Map<String, BigDecimal> conf = new LinkedHashMap<>();
    n.path("fieldConfidence").fields().forEachRemaining(e -> {
      double v = e.getValue().asDouble();
      if (v > 0 && v <= 1.0) v = v * 100; // normalize 0-1 scale to 0-100
      conf.put(e.getKey(), BigDecimal.valueOf(v).setScale(2, java.math.RoundingMode.HALF_UP));
    });

    double overall = conf.values().stream()
        .mapToDouble(BigDecimal::doubleValue).average().orElse(75.0);

    InvoiceDto invoice = new InvoiceDto(
        null, text(n, "invoiceNumber"), date(n, "invoiceDate"), text(n, "currency"),
        text(n, "supplierName"), text(n, "supplierGstin"),
        text(n, "customerName"), text(n, "customerGstin"),
        decimal(n, "taxableSubtotal"), decimal(n, "taxAmount"),
        decimal(n, "cgstAmount"), decimal(n, "sgstAmount"),
        decimal(n, "igstAmount"), decimal(n, "cessAmount"),
        decimal(n, "totalAmount"),
        BigDecimal.valueOf(overall).setScale(2, java.math.RoundingMode.HALF_UP),
        InvoiceStatus.EXTRACTED, null, lines, conf, null, null, null,
        null, null, null, null, null, null, null, null, null, false);

    return new InvoiceExtractionResult(invoice, overall);
  }

  private String text(JsonNode node, String name) {
    return node.path(name).isNull() ? null : node.path(name).asText(null);
  }

  private BigDecimal decimal(JsonNode node, String name) {
    if (node.path(name).isNull() || node.path(name).isMissingNode()) return null;
    return node.path(name).decimalValue().setScale(2, java.math.RoundingMode.HALF_UP);
  }

  private LocalDate date(JsonNode node, String name) {
    String v = text(node, name);
    return v == null || v.isBlank() ? null : LocalDate.parse(v);
  }
}
