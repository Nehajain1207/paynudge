package com.neha.paynudge.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private String role; // "user" or "assistant"
    @Column(length = 4000)
    private String content;
    private Instant createdAt = Instant.now();

    protected ChatMessage() {}
    public ChatMessage(Long customerId, String role, String content) {
        this.customerId = customerId; this.role = role; this.content = content;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
