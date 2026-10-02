package com.neha.paynudge.repo;

import com.neha.paynudge.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCustomerIdAndStatusOrderByDueDateAsc(Long customerId, String status);
}
