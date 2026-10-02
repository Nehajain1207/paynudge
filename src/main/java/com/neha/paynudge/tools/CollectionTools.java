package com.neha.paynudge.tools;

import com.neha.paynudge.model.*;
import com.neha.paynudge.repo.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * The actions the AI agent is allowed to take. The model only chooses WHICH tool to call and with
 * what arguments; this class does the real work and enforces the rules (ownership, dates, idempotency).
 * The customer id always comes from the server, never from the model.
 */
@Service
public class CollectionTools {
    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final PaymentLinkRepository links;
    private final PaymentPromiseRepository promises;

    public CollectionTools(CustomerRepository customers, InvoiceRepository invoices,
                           PaymentLinkRepository links, PaymentPromiseRepository promises) {
        this.customers = customers; this.invoices = invoices; this.links = links; this.promises = promises;
    }

    public Map<String, Object> getDues(Long customerId) {
        Customer customer = customers.findById(customerId).orElseThrow();
        List<Invoice> unpaid = invoices.findByCustomerIdAndStatusOrderByDueDateAsc(customerId, "UNPAID");
        BigDecimal total = BigDecimal.ZERO;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Invoice inv : unpaid) {
            total = total.add(inv.getAmount());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("invoice_id", inv.getId());
            row.put("description", inv.getDescription());
            row.put("amount", inv.getAmount());
            row.put("due_date", inv.getDueDate().toString());
            row.put("days_overdue", Math.max(0, ChronoUnit.DAYS.between(inv.getDueDate(), LocalDate.now())));
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("customer", customer.getName());
        out.put("total_due", total);
        out.put("invoices", rows);
        return out;
    }

    /** Idempotent: asking for a link for the same invoice any number of times returns the same link. */
    public Map<String, Object> createPaymentLink(Long customerId, Long invoiceId) {
        Invoice inv = resolveInvoice(customerId, invoiceId);
        if (inv == null) return error("No unpaid invoice found for this customer.");
        String key = "link-" + inv.getId() + "-" + inv.getAmount().toPlainString();
        PaymentLink link = links.findByIdempotencyKey(key).orElse(null);
        if (link == null) {
            String url = "https://pay.paynudge.example/i/" + inv.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
            try {
                link = links.saveAndFlush(new PaymentLink(inv.getId(), inv.getAmount(), url, key));
            } catch (DataIntegrityViolationException raceLost) {
                // Another request created it a moment ago; the unique key guarantees a single row.
                link = links.findByIdempotencyKey(key).orElseThrow();
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("payment_link_id", link.getId());
        out.put("invoice_id", inv.getId());
        out.put("amount", link.getAmount());
        out.put("url", link.getUrl());
        return out;
    }

    /** Idempotent: the same promise (invoice, amount, date) is stored once, however often it is repeated. */
    public Map<String, Object> recordPromise(Long customerId, Long invoiceId, BigDecimal amount, String promisedDate) {
        Invoice inv = resolveInvoice(customerId, invoiceId);
        if (inv == null) return error("No unpaid invoice found for this customer.");
        LocalDate date;
        try { date = LocalDate.parse(promisedDate); }
        catch (DateTimeParseException | NullPointerException e) { return error("promised_date must be a real date in YYYY-MM-DD format."); }
        if (date.isBefore(LocalDate.now())) return error("promised_date cannot be in the past.");
        if (amount == null) amount = inv.getAmount();
        if (amount.signum() <= 0 || amount.compareTo(inv.getAmount()) > 0)
            return error("amount must be more than 0 and not more than the invoice amount " + inv.getAmount() + ".");

        String key = "promise-" + customerId + "-" + inv.getId() + "-" + amount.toPlainString() + "-" + date;
        PaymentPromise promise = promises.findByIdempotencyKey(key).orElse(null);
        if (promise == null) {
            try {
                promise = promises.saveAndFlush(new PaymentPromise(customerId, inv.getId(), amount, date, key));
            } catch (DataIntegrityViolationException raceLost) {
                promise = promises.findByIdempotencyKey(key).orElseThrow();
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("promise_id", promise.getId());
        out.put("invoice_id", inv.getId());
        out.put("amount", promise.getAmount());
        out.put("promised_date", promise.getPromisedDate().toString());
        return out;
    }

    /** Uses the given invoice if it belongs to this customer and is unpaid; otherwise the oldest unpaid one. */
    private Invoice resolveInvoice(Long customerId, Long invoiceId) {
        List<Invoice> unpaid = invoices.findByCustomerIdAndStatusOrderByDueDateAsc(customerId, "UNPAID");
        if (invoiceId != null)
            return unpaid.stream().filter(i -> i.getId().equals(invoiceId)).findFirst().orElse(null);
        return unpaid.isEmpty() ? null : unpaid.get(0);
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("error", message);
        return out;
    }
}
