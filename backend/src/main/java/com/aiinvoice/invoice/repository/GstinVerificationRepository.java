package com.aiinvoice.invoice.repository;

import com.aiinvoice.invoice.entity.GstinVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface GstinVerificationRepository extends JpaRepository<GstinVerification, String> {

    @Query("SELECT v FROM GstinVerification v WHERE v.gstin = :gstin AND v.verifiedAt > :after")
    Optional<GstinVerification> findFresh(String gstin, Instant after);
}
