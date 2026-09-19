package com.aiinvoice.email.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_poll_logs")
@Getter @Setter @NoArgsConstructor
public class EmailPollLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "polled_at", nullable = false)
    private Instant polledAt;

    @Column(name = "mailbox_user")
    private String mailboxUser;

    @Column(name = "messages_found")
    private int messagesFound;

    @Column(name = "invoices_created")
    private int invoicesCreated;

    @Column(name = "errors")
    private int errors;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "SUCCESS";
}
