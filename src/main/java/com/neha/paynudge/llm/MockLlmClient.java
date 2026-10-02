package com.neha.paynudge.llm;

import com.neha.paynudge.Json;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A rule-based stand-in for the model, used when no API key is set and in tests.
 * It lets the whole agent loop (tool call -> tool result -> reply) run offline and for free.
 */
public class MockLlmClient implements LlmClient {
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    @Override
    public Map<String, Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> last = messages.get(messages.size() - 1);
        String text = String.valueOf(last.get("content"));

        if (tools == null || tools.isEmpty()) {   // a summary request: reply with JSON only
            boolean promised = DATE.matcher(text).find();
            return text(promised
                    ? "{\"intent\":\"PROMISED_TO_PAY\",\"sentiment\":\"POSITIVE\",\"next_action\":\"Check for the payment on the promised date.\"}"
                    : "{\"intent\":\"ASKED_FOR_DUES\",\"sentiment\":\"NEUTRAL\",\"next_action\":\"Follow up in a few days.\"}");
        }
        if ("tool".equals(last.get("role")))
            return text("Here is what I found: " + text);

        String lower = text.toLowerCase();
        Matcher date = DATE.matcher(text);
        if (date.find())
            return toolCall("record_promise", Map.of("promised_date", date.group()));
        if (lower.contains("link") || lower.contains("pay now"))
            return toolCall("create_payment_link", Map.of());
        if (lower.contains("due") || lower.contains("owe") || lower.contains("how much"))
            return toolCall("get_dues", Map.of());
        return text("Hello! I can tell you what is due, send a payment link, or note the date you plan to pay.");
    }

    private static Map<String, Object> text(String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", "assistant");
        m.put("content", content);
        return m;
    }

    private static Map<String, Object> toolCall(String name, Map<String, Object> args) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("arguments", Json.write(args));
        Map<String, Object> call = new LinkedHashMap<>();
        call.put("id", "call_" + UUID.randomUUID().toString().substring(0, 8));
        call.put("type", "function");
        call.put("function", function);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", "assistant");
        m.put("content", null);
        m.put("tool_calls", List.of(call));
        return m;
    }
}
