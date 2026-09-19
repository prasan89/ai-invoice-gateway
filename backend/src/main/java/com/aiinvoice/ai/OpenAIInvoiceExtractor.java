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
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAIInvoiceExtractor implements InvoiceExtractor {
  private static final String PROMPT = """
      Extract the Indian GST invoice in the supplied document and return JSON only.
      Never invent a value. If a field is absent, return null.
      Preserve the invoice's currency and numeric values for accounting.
      Dates must be ISO-8601 yyyy-MM-dd when present.
      Do not calculate values that are not supported by the document.
      For every extracted field, provide confidence from 0 to 100 based on how clearly it
      is present in the document. Line items also need a confidence.
      Extract invoice number, invoice date, currency, supplier/customer names and GSTINs,
      taxable subtotal, total tax, grand total, and every line item including description,
      quantity, unit price, discount, tax rate, tax amount and line total.
      """;

  private final ObjectMapper mapper;
  private final HttpClient client;
  private final String apiKey;
  private final String model;

  public OpenAIInvoiceExtractor(ObjectMapper mapper,
      @Value("${openai.api-key:}") String apiKey,
      @Value("${openai.model:gpt-5.6-luna}") String model) {
    this.mapper = mapper;
    this.apiKey = apiKey;
    this.model = model;
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
  }

  @Override
  public InvoiceExtractionResult extract(MultipartFile document) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
          "OPENAI_API_KEY is not configured. Set AI_PROVIDER=demo to use the demo extractor.");
    }

    try {
      String mime = document.getContentType() == null ? "application/pdf" : document.getContentType();
      String dataUrl = "data:" + mime + ";base64," +
          Base64.getEncoder().encodeToString(document.getBytes());

      Map<String, Object> inputPart = new LinkedHashMap<>();
      if (mime.startsWith("image/")) {
        inputPart.put("type", "input_image");
        inputPart.put("image_url", dataUrl);
        inputPart.put("detail", "high");
      } else {
        inputPart.put("type", "input_file");
        inputPart.put("filename",
            document.getOriginalFilename() == null ? "invoice.pdf" : document.getOriginalFilename());
        inputPart.put("file_data", dataUrl);
        inputPart.put("detail", "high");
      }

      Map<String, Object> text = Map.of("type", "input_text", "text", PROMPT);
      Map<String, Object> message = Map.of("role", "user", "content", List.of(text, inputPart));

      Map<String, Object> request = new LinkedHashMap<>();
      request.put("model", model);
      request.put("store", false);
      request.put("input", List.of(message));
      request.put("text", Map.of("format", schemaFormat()));

      HttpRequest httpRequest = HttpRequest.newBuilder()
          .uri(URI.create("https://api.openai.com/v1/responses"))
          .timeout(Duration.ofSeconds(120))
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request)))
          .build();

      HttpResponse<String> response =
          client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() / 100 != 2) {
        throw new IllegalStateException(
            "AI extraction failed: HTTP " + response.statusCode() + " " + response.body());
      }

      JsonNode root = mapper.readTree(response.body());
      if (!"completed".equals(root.path("status").asText())) {
        throw new IllegalStateException(
            "AI extraction did not complete: " + root.path("status").asText());
      }

      String output = root.path("output_text").asText("");
      if (output.isBlank()) {
        for (JsonNode item : root.path("output")) {
          for (JsonNode part : item.path("content")) {
            if ("output_text".equals(part.path("type").asText())) {
              output = part.path("text").asText("");
              break;
            }
          }
          if (!output.isBlank()) break;
        }
      }
      if (output.isBlank()) throw new IllegalStateException("AI returned no structured invoice data");

      return toResult(mapper.readTree(output));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("AI extraction was interrupted", e);
    } catch (Exception e) {
      if (e instanceof IllegalStateException ise) throw ise;
      throw new IllegalStateException("AI extraction failed: " + e.getMessage(), e);
    }
  }

  private Map<String, Object> schemaFormat() {
    Map<String, Object> confidence = new LinkedHashMap<>();
    List<String> fields = List.of(
        "invoiceNumber", "invoiceDate", "currency", "supplierName", "supplierGstin",
        "customerName", "customerGstin", "subtotal", "taxAmount", "totalAmount", "lines");
    for (String field : fields) {
      confidence.put(field, Map.of("type", "number", "minimum", 0, "maximum", 100));
    }

    Map<String, Object> line = new LinkedHashMap<>();
    line.put("type", "object");
    line.put("additionalProperties", false);
    line.put("properties", Map.of(
        "description", Map.of("type", List.of("string", "null")),
        "quantity", Map.of("type", List.of("number", "null")),
        "unitPrice", Map.of("type", List.of("number", "null")),
        "discount", Map.of("type", List.of("number", "null")),
        "taxRate", Map.of("type", List.of("number", "null")),
        "taxAmount", Map.of("type", List.of("number", "null")),
        "lineTotal", Map.of("type", List.of("number", "null")),
        "confidence", Map.of("type", "number", "minimum", 0, "maximum", 100)));
    line.put("required", List.of(
        "description", "quantity", "unitPrice", "discount", "taxRate", "taxAmount", "lineTotal", "confidence"));

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("invoiceNumber", Map.of("type", List.of("string", "null")));
    properties.put("invoiceDate", Map.of("type", List.of("string", "null")));
    properties.put("currency", Map.of("type", List.of("string", "null")));
    properties.put("supplierName", Map.of("type", List.of("string", "null")));
    properties.put("supplierGstin", Map.of("type", List.of("string", "null")));
    properties.put("customerName", Map.of("type", List.of("string", "null")));
    properties.put("customerGstin", Map.of("type", List.of("string", "null")));
    properties.put("subtotal", Map.of("type", List.of("number", "null")));
    properties.put("taxAmount", Map.of("type", List.of("number", "null")));
    properties.put("totalAmount", Map.of("type", List.of("number", "null")));
    properties.put("lines", Map.of("type", "array", "items", line));
    properties.put("fieldConfidence", Map.of(
        "type", "object", "additionalProperties", false,
        "properties", confidence, "required", fields));

    return Map.of(
        "type", "json_schema", "name", "indian_gst_invoice", "strict", true,
        "schema", Map.of("type", "object", "additionalProperties", false,
            "properties", properties, "required", new ArrayList<>(properties.keySet())));
  }

  private InvoiceExtractionResult toResult(JsonNode n) {
    List<InvoiceLineDto> lines = new ArrayList<>();
    for (JsonNode l : n.path("lines")) {
      lines.add(new InvoiceLineDto(null, text(l, "description"), decimal(l, "quantity"),
          decimal(l, "unitPrice"), decimal(l, "discount"), decimal(l, "taxRate"),
          decimal(l, "taxAmount"), decimal(l, "lineTotal")));
    }

    Map<String, BigDecimal> confidence = new LinkedHashMap<>();
    n.path("fieldConfidence").fields().forEachRemaining(e ->
        confidence.put(e.getKey(), BigDecimal.valueOf(e.getValue().asDouble()).setScale(2)));

    double overall = confidence.values().stream()
        .mapToDouble(BigDecimal::doubleValue).average().orElse(0);

    InvoiceDto invoice = new InvoiceDto(
        null, text(n, "invoiceNumber"), date(n, "invoiceDate"), text(n, "currency"),
        text(n, "supplierName"), text(n, "supplierGstin"),
        text(n, "customerName"), text(n, "customerGstin"),
        decimal(n, "subtotal"), decimal(n, "taxAmount"), decimal(n, "totalAmount"),
        BigDecimal.valueOf(overall).setScale(2), InvoiceStatus.EXTRACTED, null, lines,
        confidence, null, null, null);

    return new InvoiceExtractionResult(invoice, overall);
  }

  private String text(JsonNode node, String name) {
    return node.path(name).isNull() ? null : node.path(name).asText(null);
  }

  private BigDecimal decimal(JsonNode node, String name) {
    return node.path(name).isNull() ? null : node.path(name).decimalValue();
  }

  private LocalDate date(JsonNode node, String name) {
    String value = text(node, name);
    return value == null || value.isBlank() ? null : LocalDate.parse(value);
  }
}
