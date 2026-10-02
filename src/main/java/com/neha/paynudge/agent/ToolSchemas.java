package com.neha.paynudge.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tool descriptions sent to the model so it knows what it may call and with which arguments. */
final class ToolSchemas {
    private ToolSchemas() {}

    static final List<Map<String, Object>> ALL = List.of(
            tool("get_dues", "Get this customer's unpaid invoices and total amount due.",
                    Map.of(), List.of()),
            tool("create_payment_link", "Create a payment link for an unpaid invoice. If invoice_id is omitted, the oldest unpaid invoice is used.",
                    Map.of("invoice_id", prop("integer", "Invoice id from get_dues")), List.of()),
            tool("record_promise", "Record the customer's commitment to pay by a date.",
                    Map.of("invoice_id", prop("integer", "Invoice id from get_dues"),
                           "amount", prop("number", "Amount promised in rupees; defaults to the full invoice amount"),
                           "promised_date", prop("string", "Date the customer promised to pay, as YYYY-MM-DD")),
                    List.of("promised_date")));

    private static Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private static Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }
}
