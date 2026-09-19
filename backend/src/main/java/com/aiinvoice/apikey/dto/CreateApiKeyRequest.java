package com.aiinvoice.apikey.dto;

import java.util.List;

public record CreateApiKeyRequest(String name, List<String> scopes) {
}
