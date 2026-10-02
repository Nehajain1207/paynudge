package com.neha.paynudge.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A conversation turned into structured data that a business owner or a report can use. */
@Entity
public class ChatSummary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private String intent;      // see SummaryService.INTENTS
    private String sentiment;   // POSITIVE, NEUTRAL or NEGATIVE
    private LocalDate promisedDate;
    private BigDecimal promisedAmount;
    @Column(length = 500)
    private String nextAction;
    private int messageCount;
    private Instant createdAt = Instant.now();

    protected ChatSummary() {}
    public ChatSummary(Long customerId, String intent, String sentiment, LocalDate promisedDate,
                       BigDecimal promisedAmount, String nextAction, int messageCount) {
        this.customerId = customerId; this.intent = intent; this.sentiment = sentiment;
        this.promisedDate = promisedDate; this.promisedAmount = promisedAmount;
        this.nextAction = nextAction; this.messageCount = messageCount;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getIntent() { return intent; }
    public String getSentiment() { return sentiment; }
    public LocalDate getPromisedDate() { return promisedDate; }
    public BigDecimal getPromisedAmount() { return promisedAmount; }
    public String getNextAction() { return nextAction; }
    public int getMessageCount() { return messageCount; }
    public Instant getCreatedAt() { return createdAt; }
}
