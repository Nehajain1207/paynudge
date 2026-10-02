package com.neha.paynudge.repo;

import com.neha.paynudge.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop20ByCustomerIdOrderByIdDesc(Long customerId);
    List<ChatMessage> findByCustomerIdOrderByIdAsc(Long customerId);
}
