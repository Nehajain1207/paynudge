package com.neha.paynudge.repo;

import com.neha.paynudge.model.PaymentPromise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentPromiseRepository extends JpaRepository<PaymentPromise, Long> {
    Optional<PaymentPromise> findByIdempotencyKey(String idempotencyKey);
    List<PaymentPromise> findByCustomerIdOrderByIdDesc(Long customerId);
}
