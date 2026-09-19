package com.aiinvoice.invoice.repository;

import com.aiinvoice.invoice.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    Optional<Vendor> findByGstin(String gstin);
}
