package com.aiinvoice.apikey.service;

import com.aiinvoice.apikey.dto.ApiKeyDto;
import com.aiinvoice.apikey.dto.CreateApiKeyRequest;
import com.aiinvoice.apikey.entity.ApiKey;
import com.aiinvoice.apikey.repository.ApiKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository keyRepo;
    private final ObjectMapper objectMapper;

    public record CreatedKey(ApiKeyDto dto, String rawKey) {}

    @Transactional
    public CreatedKey create(UUID organizationId, CreateApiKeyRequest req) {
        String raw = "ak_" + UUID.randomUUID().toString().replace("-", "");
        String hash = sha256(raw);
        String scopesJson = writeJson(req.scopes());

        ApiKey key = new ApiKey();
        key.setId(UUID.randomUUID());
        key.setOrganizationId(organizationId);
        key.setName(req.name());
        key.setKeyHash(hash);
        key.setScopes(scopesJson);
        key.setCreatedAt(Instant.now());
        keyRepo.save(key);

        return new CreatedKey(toDto(key), raw);
    }

    public List<ApiKeyDto> listByOrg(UUID organizationId) {
        return keyRepo.findByOrganizationIdAndRevokedAtIsNull(organizationId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public void revoke(UUID id) {
        keyRepo.findById(id).ifPresent(k -> {
            k.setRevokedAt(Instant.now());
            keyRepo.save(k);
        });
    }

    public Optional<ApiKey> validateRawKey(String raw) {
        String hash = sha256(raw);
        Optional<ApiKey> key = keyRepo.findActiveByKeyHash(hash);
        key.ifPresent(k -> {
            k.setLastUsedAt(Instant.now());
            keyRepo.save(k);
        });
        return key;
    }

    private ApiKeyDto toDto(ApiKey k) {
        List<String> scopes;
        try { scopes = objectMapper.readValue(k.getScopes(), new com.fasterxml.jackson.core.type.TypeReference<>() {}); }
        catch (Exception e) { scopes = List.of(); }
        return new ApiKeyDto(k.getId(), k.getName(), scopes, k.getCreatedAt(), k.getLastUsedAt());
    }

    private String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private String writeJson(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception e) { return "[]"; }
    }
}
