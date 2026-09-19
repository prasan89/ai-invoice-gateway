package com.aiinvoice.invoice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "gstin_verifications")
@Getter @Setter @NoArgsConstructor
public class GstinVerification {

    @Id
    @Column(length = 20)
    private String gstin;

    @Column(name = "legal_name", length = 300)
    private String legalName;

    @Column(name = "trade_name", length = 300)
    private String tradeName;

    @Column(name = "registration_status", length = 30)
    private String registrationStatus;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "business_nature", length = 200)
    private String businessNature;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;
}
