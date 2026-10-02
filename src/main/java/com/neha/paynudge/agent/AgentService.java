package com.neha.paynudge.agent;

import com.neha.paynudge.Json;
import com.neha.paynudge.llm.LlmClient;
import com.neha.paynudge.model.ChatMessage;
import com.neha.paynudge.model.Customer;
import com.neha.paynudge.model.PaymentPromise;
import com.neha.paynudge.repo.ChatMessageRepository;
import com.neha.paynudge.repo.CustomerRepository;
import com.neha.paynudge.repo.PaymentPromiseRepository;
import com.neha.paynudge.tools.CollectionTools;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * The agent loop: send the conversation to the model, run any tools it asks for,
 * feed the results back, and repeat until the model answers in plain text.
 */
@Service
public class AgentService {
    private static final int MAX_STEPS = 5;

    private final LlmClient llm;
    private final CollectionTools tools;
    private final CustomerRepository customers;
    private final ChatMessageRepository chatMessages;
    private final PaymentPromiseRepository promises;

    public AgentService(LlmClient llm, CollectionTools tools, CustomerRepository customers,
                        ChatMessageRepository chatMessages, PaymentPromiseRepository promises) {
        this.llm = llm; this.tools = tools; this.customers = customers;
        this.chatMessages = chatMessages; this.promises = promises;
    }

    public record ToolUse(String name, Map<String, Object> arguments, Map<String, Object> result) {}
    public record ChatResult(String reply, List<ToolUse> toolsUsed) {}

    @SuppressWarnings("unchecked")
    public ChatResult chat(Long customerId, String userText) {
        Customer customer = customers.findById(customerId).orElseThrow();

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt(customer)));
        List<ChatMessage> history = new ArrayList<>(chatMessages.findTop20ByCustomerIdOrderByIdDesc(customerId));
        Collections.reverse(history);
        for (ChatMessage m : history) messages.add(message(m.getRole(), m.getContent()));
        messages.add(message("user", userText));

        List<ToolUse> used = new ArrayList<>();
        String reply = "Sorry, I could not complete that. Please try again.";
        for (int step = 0; step < MAX_STEPS; step++) {
            Map<String, Object> assistant = llm.chat(messages, ToolSchemas.ALL);
            messages.add(assistant);
            List<Map<String, Object>> calls = (List<Map<String, Object>>) assistant.get("tool_calls");
            if (calls == null || calls.isEmpty()) {
                Object content = assistant.get("content");
                if (content != null && !content.toString().isBlank()) reply = content.toString();
                break;
            }
            for (Map<String, Object> call : calls) {
                Map<String, Object> function = (Map<String, Object>) call.get("function");
                String name = String.valueOf(function.get("name"));
                Object rawArgs = function.get("arguments");
                Map<String, Object> args = (rawArgs == null || rawArgs.toString().isBlank())
                        ? new HashMap<>() : Json.readMap(rawArgs.toString());
                Map<String, Object> result = runTool(customerId, name, args);
                used.add(new ToolUse(name, args, result));

                Map<String, Object> toolMessage = new LinkedHashMap<>();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", call.get("id"));
                toolMessage.put("content", Json.write(result));
                messages.add(toolMessage);
            }
        }

        chatMessages.save(new ChatMessage(customerId, "user", userText));
        chatMessages.save(new ChatMessage(customerId, "assistant", reply));
        return new ChatResult(reply, used);
    }

    private Map<String, Object> runTool(Long customerId, String name, Map<String, Object> args) {
        try {
            return switch (name) {
                case "get_dues" -> tools.getDues(customerId);
                case "create_payment_link" -> tools.createPaymentLink(customerId, toLong(args.get("invoice_id")));
                case "record_promise" -> tools.recordPromise(customerId, toLong(args.get("invoice_id")),
                        toDecimal(args.get("amount")), args.get("promised_date") == null ? null : args.get("promised_date").toString());
                default -> Map.of("error", "Unknown tool: " + name);
            };
        } catch (RuntimeException e) {
            return Map.of("error", "Tool failed: " + e.getMessage());
        }
    }

    /** Memory: earlier promises are loaded from the database into every new conversation. */
    private String systemPrompt(Customer customer) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are PayNudge, a polite payment-reminder assistant for a small business. ")
          .append("You are chatting with the customer ").append(customer.getName()).append(". ")
          .append("Today is ").append(LocalDate.now().getDayOfWeek()).append(", ").append(LocalDate.now()).append(". ")
          .append("Work out relative dates (tomorrow, kal, parso, in 3 days, a weekday name) from today's date. ")
          .append("Rules: Always call get_dues before stating any amount. ")
          .append("When the customer wants to pay, call create_payment_link and share the url. ")
          .append("When the customer commits to a date, convert it to YYYY-MM-DD and call record_promise. ")
          .append("Never invent amounts, dates, links or promises; only state what a tool returned. ")
          .append("If the customer has not given a clear date, ask for one instead of guessing. ")
          .append("Reply in the customer's language (English or Hinglish), in 1 to 3 short sentences. ");
        List<PaymentPromise> earlier = promises.findByCustomerIdOrderByIdDesc(customer.getId());
        if (earlier.isEmpty()) {
            sb.append("The customer has made no earlier payment promises.");
        } else {
            sb.append("Earlier promises by this customer (most recent first): ");
            for (PaymentPromise p : earlier.subList(0, Math.min(5, earlier.size())))
                sb.append("[Rs ").append(p.getAmount()).append(" by ").append(p.getPromisedDate())
                  .append(" for invoice ").append(p.getInvoiceId()).append("] ");
            sb.append("If a promised date has passed, remind the customer of it politely.");
        }
        return sb.toString();
    }

    private static Map<String, Object> message(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private static Long toLong(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        return new BigDecimal(value.toString()).longValue();
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        return new BigDecimal(value.toString());
    }
}
