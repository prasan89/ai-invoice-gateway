package com.aiinvoice.enterprise.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "sso_configurations")
@Getter @Setter @NoArgsConstructor
public class SsoConfiguration {
    @Id private UUID id;
    @Column(name = "organization_id", nullable = false, unique = true) private UUID organizationId;
    @Column(nullable = false) private String provider;
    @Column(name = "entity_id") private String entityId;
    @Column(name = "metadata_url") private String metadataUrl;
    @Column(name = "client_id") private String clientId;
    @Column(name = "client_secret") private String clientSecret;
    private String issuer;
    @Column(nullable = false) private boolean enabled = false;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
