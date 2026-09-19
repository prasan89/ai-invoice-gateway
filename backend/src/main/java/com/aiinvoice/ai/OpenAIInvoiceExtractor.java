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
      """;

  private final ObjectMapper mapper;
  private final HttpClient client;
  private final String apiKey;
  private final String model;

  public OpenAIInvoiceExtractor(ObjectMapper mapper,
      @Value("${openai.api-key:}") String apiKey,
      @Value("${openai.model:gpt-4o}") String model) {
    this.mapper = mapper;
    this.apiKey = apiKey;
    this.model = model;
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
  }

  @Override
  public InvoiceExtractionResult extract(MultipartFile document) {
    if (apiKey == null || apiKey.isBlank())
      throw new IllegalStateException(
          "OPENAI_API_KEY is not configured. Set AI_PROVIDER=demo to use the demo extractor.");

    try {
      String mime = document.getContentType() == null ? "application/pdf" : document.getContentType();
      String dataUrl = "data:" + mime + ";base64,"
          + Base64.getEncoder().encodeToString(document.getBytes());

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

      if (response.statusCode() / 100 != 2)
        throw new IllegalStateException(
            "AI extraction failed: HTTP " + response.statusCode() + " " + response.body());

      JsonNode root = mapper.readTree(response.body());
      if (!"completed".equals(root.path("status").asText()))
        throw new IllegalStateException(
            "AI extraction did not complete: " + root.path("status").asText());

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
      if (output.isBlank())
        throw new IllegalStateException("AI returned no structured invoice data");

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
    List<String> confidenceFields = List.of(
        "invoiceNumber", "invoiceDate", "currency",
        "supplierName", "supplierGstin", "customerName", "customerGstin",
        "taxableSubtotal", "cgstAmount", "sgstAmount", "igstAmount", "cessAmount",
        "taxAmount", "totalAmount", "lines");

    Map<String, Object> confidence = new LinkedHashMap<>();
    for (String f : confidenceFields)
      confidence.put(f, Map.of("type", "number", "minimum", 0, "maximum", 100));

    Map<String, Object> lineProps = new LinkedHashMap<>();
    for (String f : List.of("description", "hsnSac"))
      lineProps.put(f, Map.of("type", List.of("string", "null")));
    for (String f : List.of("quantity", "unitPrice", "discount", "taxableValue",
        "taxRate", "taxAmount", "lineTotal",
        "cgstRate", "cgstAmount", "sgstRate", "sgstAmount",
        "igstRate", "igstAmount", "cessRate", "cessAmount", "confidence"))
      lineProps.put(f, Map.of("type", List.of("number", "null")));

    Map<String, Object> line = new LinkedHashMap<>();
    line.put("type", "object");
    line.put("additionalProperties", false);
    line.put("properties", lineProps);
    line.put("required", new ArrayList<>(lineProps.keySet()));

    Map<String, Object> props = new LinkedHashMap<>();
    for (String f : List.of("invoiceNumber", "invoiceDate", "currency",
        "supplierName", "supplierGstin", "supplierAddress",
        "customerName", "customerGstin", "customerAddress",
        "placeOfSupply", "amountInWords"))
      props.put(f, Map.of("type", List.of("string", "null")));
    props.put("reverseCharge", Map.of("type", List.of("boolean", "null")));
    for (String f : List.of("taxableSubtotal", "cgstAmount", "sgstAmount",
        "igstAmount", "cessAmount", "taxAmount", "totalAmount"))
      props.put(f, Map.of("type", List.of("number", "null")));
    props.put("lines", Map.of("type", "array", "items", line));
    props.put("fieldConfidence", Map.of(
        "type", "object", "additionalProperties", false,
        "properties", confidence, "required", confidenceFields));

    return Map.of(
        "type", "json_schema", "name", "indian_gst_invoice", "strict", true,
        "schema", Map.of("type", "object", "additionalProperties", false,
            "properties", props, "required", new ArrayList<>(props.keySet())));
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
    n.path("fieldConfidence").fields().forEachRemaining(e ->
        conf.put(e.getKey(), BigDecimal.valueOf(e.getValue().asDouble()).setScale(2, java.math.RoundingMode.HALF_UP)));

    double overall = conf.values().stream()
        .mapToDouble(BigDecimal::doubleValue).average().orElse(0);

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
    return node.path(name).isNull() ? null : node.path(name).decimalValue();
  }

  private LocalDate date(JsonNode node, String name) {
    String v = text(node, name);
    return v == null || v.isBlank() ? null : LocalDate.parse(v);
  }
}
