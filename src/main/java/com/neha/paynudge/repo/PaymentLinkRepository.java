package com.neha.paynudge.repo;

import com.neha.paynudge.model.PaymentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentLinkRepository extends JpaRepository<PaymentLink, Long> {
    Optional<PaymentLink> findByIdempotencyKey(String idempotencyKey);
}
