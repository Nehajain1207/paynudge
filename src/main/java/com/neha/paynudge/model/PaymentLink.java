package com.neha.paynudge.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class PaymentLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long invoiceId;
    private BigDecimal amount;
    private String url;
    /** Same request retried any number of times maps to the same key, so only one row can exist. */
    @Column(unique = true, nullable = false)
    private String idempotencyKey;
    private Instant createdAt = Instant.now();

    protected PaymentLink() {}
    public PaymentLink(Long invoiceId, BigDecimal amount, String url, String idempotencyKey) {
        this.invoiceId = invoiceId; this.amount = amount; this.url = url; this.idempotencyKey = idempotencyKey;
    }

    public Long getId() { return id; }
    public Long getInvoiceId() { return invoiceId; }
    public BigDecimal getAmount() { return amount; }
    public String getUrl() { return url; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
}
