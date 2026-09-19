package com.aiinvoice.invoice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvoiceStorageService {
  private final Path root;

  public InvoiceStorageService(@Value("\${invoice.storage-dir:./data/invoices}") String storageDir) {
    this.root = Paths.get(storageDir).toAbsolutePath().normalize();
  }

  public StoredDocument store(UUID invoiceId, MultipartFile file) {
    try {
      Files.createDirectories(root);
      String extension = extension(file.getOriginalFilename(), file.getContentType());
      Path target = root.resolve(invoiceId + extension).normalize();
      if (!target.getParent().equals(root)) {
        throw new IllegalArgumentException("Invalid invoice file name");
      }
      file.transferTo(target);
      return new StoredDocument(
          file.getOriginalFilename() == null ? "invoice" : file.getOriginalFilename(),
          file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
          target.toString());
    } catch (IOException e) {
      throw new IllegalStateException("Could not store invoice document", e);
    }
  }

  public Resource load(String storagePath) {
    try {
      Path path = Paths.get(storagePath).toAbsolutePath().normalize();
      if (!path.startsWith(root) || !Files.exists(path)) {
        throw new IllegalArgumentException("Invoice document not found");
      }
      return new UrlResource(path.toUri());
    } catch (IOException e) {
      throw new IllegalStateException("Could not load invoice document", e);
    }
  }

  private String extension(String filename, String contentType) {
    String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
    if (name.endsWith(".pdf") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
      return name.substring(name.lastIndexOf('.'));
    }
    if ("application/pdf".equalsIgnoreCase(contentType)) return ".pdf";
    if ("image/png".equalsIgnoreCase(contentType)) return ".png";
    if ("image/jpeg".equalsIgnoreCase(contentType)) return ".jpg";
    throw new IllegalArgumentException("Only PDF, PNG and JPG invoices are supported");
  }

  public record StoredDocument(String fileName, String contentType, String storagePath) {}
}
