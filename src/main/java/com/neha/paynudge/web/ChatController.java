package com.neha.paynudge.web;

import com.neha.paynudge.agent.AgentService;
import com.neha.paynudge.model.ChatMessage;
import com.neha.paynudge.model.Customer;
import com.neha.paynudge.model.PaymentPromise;
import com.neha.paynudge.repo.ChatMessageRepository;
import com.neha.paynudge.repo.CustomerRepository;
import com.neha.paynudge.repo.PaymentPromiseRepository;
import com.neha.paynudge.tools.CollectionTools;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final AgentService agent;
    private final CollectionTools tools;
    private final CustomerRepository customers;
    private final ChatMessageRepository chatMessages;
    private final PaymentPromiseRepository promises;

    public ChatController(AgentService agent, CollectionTools tools, CustomerRepository customers,
                          ChatMessageRepository chatMessages, PaymentPromiseRepository promises) {
        this.agent = agent; this.tools = tools; this.customers = customers;
        this.chatMessages = chatMessages; this.promises = promises;
    }

    public record ChatRequest(@NotBlank @Size(max = 1000) String message) {}

    @GetMapping("/customers")
    public List<Customer> customers() {
        return customers.findAll().stream().filter(c -> !c.getName().startsWith("[eval]")).toList();
    }

    @GetMapping("/customers/{id}/dues")
    public Map<String, Object> dues(@PathVariable Long id) { return tools.getDues(id); }

    @GetMapping("/customers/{id}/promises")
    public List<PaymentPromise> promises(@PathVariable Long id) { return promises.findByCustomerIdOrderByIdDesc(id); }

    @GetMapping("/customers/{id}/messages")
    public List<ChatMessage> messages(@PathVariable Long id) { return chatMessages.findByCustomerIdOrderByIdAsc(id); }

    @PostMapping("/customers/{id}/chat")
    public AgentService.ChatResult chat(@PathVariable Long id, @Valid @RequestBody ChatRequest request) {
        return agent.chat(id, request.message());
    }
}
