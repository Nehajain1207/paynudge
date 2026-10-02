package com.neha.paynudge.repo;

import com.neha.paynudge.model.ChatSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatSummaryRepository extends JpaRepository<ChatSummary, Long> {
    List<ChatSummary> findByCustomerIdOrderByIdDesc(Long customerId);
}
