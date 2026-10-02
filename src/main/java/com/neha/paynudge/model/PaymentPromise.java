package com.neha.paynudge.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A commitment the customer made in a chat: "I will pay X by date Y". */
@Entity
public class PaymentPromise {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private Long invoiceId;
    private BigDecimal amount;
    private LocalDate promisedDate;
    @Column(unique = true, nullable = false)
    private String idempotencyKey;
    /** ACTIVE, or SUPERSEDED once the customer gives a newer promise for the same invoice. */
    private String status = "ACTIVE";
    private Instant createdAt = Instant.now();

    protected PaymentPromise() {}
    public PaymentPromise(Long customerId, Long invoiceId, BigDecimal amount, LocalDate promisedDate, String idempotencyKey) {
        this.customerId = customerId; this.invoiceId = invoiceId; this.amount = amount;
        this.promisedDate = promisedDate; this.idempotencyKey = idempotencyKey;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Long getInvoiceId() { return invoiceId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getPromisedDate() { return promisedDate; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStatus() { return status == null ? "ACTIVE" : status; }
    public boolean isActive() { return !"SUPERSEDED".equals(status); }
    public void setStatus(String status) { this.status = status; }
}
