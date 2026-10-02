package com.neha.paynudge.eval;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One scripted conversation and what a correct agent must do with it.
 * category: DUES, LINK, PROMISE (a clear commitment) or NO_PROMISE (vague or invalid; nothing may be recorded).
 */
public record EvalCase(String id, String category, List<String> messages,
                       LocalDate expectedDate, BigDecimal expectedAmount, String expectedInvoice) {

    static EvalCase dues(String id, String message) {
        return new EvalCase(id, "DUES", List.of(message), null, null, null);
    }
    static EvalCase link(String id, String message, String expectedInvoice) {
        return new EvalCase(id, "LINK", List.of(message), null, null, expectedInvoice);
    }
    static EvalCase promise(String id, LocalDate date, String amount, String... messages) {
        return new EvalCase(id, "PROMISE", List.of(messages), date, new BigDecimal(amount), null);
    }
    static EvalCase noPromise(String id, String message) {
        return new EvalCase(id, "NO_PROMISE", List.of(message), null, null, null);
    }
}
