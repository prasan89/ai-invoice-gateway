package com.aiinvoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvoiceGatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(InvoiceGatewayApplication.class, args);
  }
}
