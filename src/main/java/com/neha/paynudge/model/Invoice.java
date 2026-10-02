package com.neha.paynudge.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String status; // UNPAID or PAID

    protected Invoice() {}
    public Invoice(Long customerId, String description, BigDecimal amount, LocalDate dueDate) {
        this.customerId = customerId; this.description = description;
        this.amount = amount; this.dueDate = dueDate; this.status = "UNPAID";
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }
}
