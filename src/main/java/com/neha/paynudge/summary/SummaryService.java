package com.neha.paynudge.summary;

import com.neha.paynudge.Json;
import com.neha.paynudge.llm.LlmClient;
import com.neha.paynudge.model.ChatMessage;
import com.neha.paynudge.model.ChatSummary;
import com.neha.paynudge.model.PaymentPromise;
import com.neha.paynudge.repo.ChatMessageRepository;
import com.neha.paynudge.repo.ChatSummaryRepository;
import com.neha.paynudge.repo.PaymentPromiseRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Post-chat intelligence: turns a conversation into intent, sentiment and a next action.
 *
 * The model only JUDGES the conversation (intent, sentiment, next action). Hard facts, the promised date
 * and amount, are never taken from the model's summary: they come from the promise row that the
 * record_promise tool already validated and stored. So a summary cannot contain an invented promise.
 */
@Service
public class SummaryService {
    public static final List<String> INTENTS = List.of(
            "PROMISED_TO_PAY", "WANTS_TO_PAY_NOW", "ASKED_FOR_DUES", "NEEDS_MORE_TIME", "DISPUTES_BILL", "REFUSES_TO_PAY", "OTHER");
    public static final List<String> SENTIMENTS = List.of("POSITIVE", "NEUTRAL", "NEGATIVE");

    private final LlmClient llm;
    private final ChatMessageRepository chatMessages;
    private final PaymentPromiseRepository promises;
    private final ChatSummaryRepository summaries;

    public SummaryService(LlmClient llm, ChatMessageRepository chatMessages,
                          PaymentPromiseRepository promises, ChatSummaryRepository summaries) {
        this.llm = llm; this.chatMessages = chatMessages; this.promises = promises; this.summaries = summaries;
    }

    public ChatSummary summarise(Long customerId) {
        List<ChatMessage> history = chatMessages.findByCustomerIdOrderByIdAsc(customerId);
        if (history.isEmpty()) throw new IllegalStateException("This customer has no chat messages to summarise.");

        StringBuilder transcript = new StringBuilder();
        for (ChatMessage m : history.subList(Math.max(0, history.size() - 30), history.size()))
            transcript.append(m.getRole().equals("user") ? "Customer: " : "Agent: ").append(m.getContent()).append("\n");

        String instructions = "You summarise a payment-reminder chat between a business's agent and a customer. "
                + "Return only JSON, with no other text, in exactly this shape: "
                + "{\"intent\": one of " + INTENTS + ", \"sentiment\": one of " + SENTIMENTS + ", "
                + "\"next_action\": one short sentence telling the business owner what to do next}. "
                + "Judge the customer's intent from their latest messages.";
        List<Map<String, Object>> messages = List.of(message("system", instructions), message("user", transcript.toString()));

        Map<String, Object> parsed = parseJson(String.valueOf(llm.chat(messages, List.of()).get("content")));
        String intent = pick(parsed.get("intent"), INTENTS, "OTHER");
        String sentiment = pick(parsed.get("sentiment"), SENTIMENTS, "NEUTRAL");
        String nextAction = parsed.get("next_action") == null ? "Review the conversation." : parsed.get("next_action").toString();
        if (nextAction.length() > 500) nextAction = nextAction.substring(0, 500);

        // Facts come from the database, not from the model.
        List<PaymentPromise> recorded = promises.findByCustomerIdOrderByIdDesc(customerId);
        PaymentPromise latest = recorded.isEmpty() ? null : recorded.get(0);
        if (latest == null && intent.equals("PROMISED_TO_PAY")) intent = "NEEDS_MORE_TIME"; // no stored promise, so not a promise

        return summaries.save(new ChatSummary(customerId, intent, sentiment,
                latest == null ? null : latest.getPromisedDate(),
                latest == null ? null : latest.getAmount(), nextAction, history.size()));
    }

    /** Models sometimes wrap JSON in code fences or add a sentence; keep only the outermost {...}. */
    static Map<String, Object> parseJson(String text) {
        int start = text.indexOf('{'), end = text.lastIndexOf('}');
        if (start < 0 || end <= start) return new HashMap<>();
        try { return Json.readMap(text.substring(start, end + 1)); }
        catch (IllegalArgumentException e) { return new HashMap<>(); }
    }

    private static String pick(Object value, List<String> allowed, String fallback) {
        if (value == null) return fallback;
        String v = value.toString().trim().toUpperCase().replace(' ', '_');
        return allowed.contains(v) ? v : fallback;
    }

    private static Map<String, Object> message(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
