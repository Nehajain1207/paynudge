package com.neha.paynudge.eval;

import com.neha.paynudge.agent.AgentService;
import com.neha.paynudge.model.Customer;
import com.neha.paynudge.model.Invoice;
import com.neha.paynudge.model.PaymentPromise;
import com.neha.paynudge.repo.CustomerRepository;
import com.neha.paynudge.repo.InvoiceRepository;
import com.neha.paynudge.repo.PaymentPromiseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Runs the scripted conversations against the configured model and scores what the agent actually DID
 * (which tools it called and what ended up in the database), not just what it said.
 */
@Service
public class EvalService {
    private final AgentService agent;
    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final PaymentPromiseRepository promises;
    private final String model;
    private final long paceMs;

    private volatile boolean running = false;
    private volatile int done = 0;
    private volatile int total = 0;
    private volatile Map<String, Object> summary = null;
    private final List<Map<String, Object>> results = Collections.synchronizedList(new ArrayList<>());

    public EvalService(AgentService agent, CustomerRepository customers, InvoiceRepository invoices,
                       PaymentPromiseRepository promises,
                       @Value("${paynudge.llm.api-key:}") String apiKey,
                       @Value("${paynudge.llm.model}") String model,
                       @Value("${paynudge.eval.pace-ms:3000}") long paceMs) {
        this.agent = agent; this.customers = customers; this.invoices = invoices; this.promises = promises;
        this.model = (apiKey == null || apiKey.isBlank()) ? "offline-mock" : model;
        this.paceMs = paceMs;
    }

    /** set = "hard" runs only the hard cases; anything else runs the full suite. */
    public synchronized boolean start(String set) {
        if (running) return false;
        running = true; done = 0; summary = null; results.clear();
        List<EvalCase> cases = EvalCases.all().stream()
                .filter(c -> !"hard".equals(set) || c.id().startsWith("hard-")).toList();
        total = cases.size();
        Thread worker = new Thread(() -> runAll(cases), "eval-runner");
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("running", running);
        out.put("done", done);
        out.put("total", total);
        out.put("model", model);
        out.put("summary", summary);
        synchronized (results) { out.put("results", new ArrayList<>(results)); }
        return out;
    }

    private void runAll(List<EvalCase> cases) {
        try {
            for (EvalCase c : cases) {
                results.add(runCase(c));
                done++;
                try { Thread.sleep(paceMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
            summary = summarise();
            writeReport();
        } finally {
            running = false;
        }
    }

    private Map<String, Object> runCase(EvalCase c) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", c.id());
        row.put("category", c.category());
        row.put("input", String.join(" / ", c.messages()));
        List<String> toolsCalled = new ArrayList<>();
        List<Long> latencies = new ArrayList<>();
        boolean passed = false, invented = false, grounded = true;
        String detail;
        try {
            // Every case gets a brand-new customer, so cases cannot affect each other.
            Customer customer = customers.save(new Customer("[eval] " + c.id() + " " + System.currentTimeMillis(), "+91-0000000000"));
            LocalDate today = LocalDate.now();
            Invoice paint = invoices.save(new Invoice(customer.getId(), EvalCases.PAINT, new BigDecimal("12000"), today.minusDays(20)));
            Invoice brushes = null;
            boolean twoInvoices = c.category().equals("DUES") || c.category().equals("LINK");
            if (twoInvoices)
                brushes = invoices.save(new Invoice(customer.getId(), EvalCases.BRUSHES, new BigDecimal("6000"), today.minusDays(5)));

            String reply = "";
            List<AgentService.ToolUse> uses = new ArrayList<>();
            for (String message : c.messages()) {
                long startedAt = System.nanoTime();
                AgentService.ChatResult result = agent.chat(customer.getId(), message);
                latencies.add((System.nanoTime() - startedAt) / 1_000_000);
                reply = result.reply();
                uses.addAll(result.toolsUsed());
            }
            for (AgentService.ToolUse u : uses) toolsCalled.add(u.name());
            List<PaymentPromise> recorded = promises.findByCustomerIdOrderByIdDesc(customer.getId());
            String plainReply = reply.replace(",", "");

            switch (c.category()) {
                case "DUES" -> {
                    boolean called = toolsCalled.contains("get_dues");
                    grounded = plainReply.contains("18000") || (plainReply.contains("12000") && plainReply.contains("6000"));
                    passed = called && grounded && recorded.isEmpty();
                    detail = !called ? "did not call get_dues" : !grounded ? "reply did not state the real amounts" : "ok";
                }
                case "LINK" -> {
                    AgentService.ToolUse linkUse = uses.stream().filter(u -> u.name().equals("create_payment_link")).reduce((a, b) -> b).orElse(null);
                    Object url = linkUse == null ? null : linkUse.result().get("url");
                    grounded = url != null && reply.contains(url.toString());
                    boolean rightInvoice = true;
                    if (linkUse != null && c.expectedInvoice() != null) {
                        Long wanted = c.expectedInvoice().equals(EvalCases.PAINT) ? paint.getId() : brushes.getId();
                        rightInvoice = wanted.toString().equals(String.valueOf(linkUse.result().get("invoice_id")));
                    }
                    passed = linkUse != null && grounded && rightInvoice;
                    detail = linkUse == null ? "did not call create_payment_link" : !rightInvoice ? "link created for the wrong invoice"
                            : !grounded ? "reply did not include the real link" : "ok";
                }
                case "PROMISE" -> {
                    if (recorded.isEmpty()) {
                        detail = "no promise recorded";
                    } else if (recorded.size() > 1) {
                        detail = "recorded " + recorded.size() + " promises instead of 1";
                    } else {
                        PaymentPromise p = recorded.get(0);
                        boolean dateOk = p.getPromisedDate().equals(c.expectedDate());
                        boolean amountOk = p.getAmount().compareTo(c.expectedAmount()) == 0;
                        passed = dateOk && amountOk;
                        detail = passed ? "ok" : "recorded Rs " + p.getAmount().stripTrailingZeros().toPlainString() + " by " + p.getPromisedDate()
                                + ", expected Rs " + c.expectedAmount() + " by " + c.expectedDate();
                    }
                }
                case "PROMISE_LATEST" -> {
                    List<PaymentPromise> active = recorded.stream().filter(PaymentPromise::isActive).toList();
                    if (active.size() != 1) {
                        detail = active.size() + " active promises, expected exactly 1";
                    } else {
                        PaymentPromise p = active.get(0);
                        passed = p.getPromisedDate().equals(c.expectedDate()) && p.getAmount().compareTo(c.expectedAmount()) == 0;
                        detail = passed ? "ok (1 active, " + (recorded.size() - 1) + " superseded)" : "active is Rs " + p.getAmount().stripTrailingZeros().toPlainString()
                                + " by " + p.getPromisedDate() + ", expected Rs " + c.expectedAmount() + " by " + c.expectedDate();
                    }
                }
                case "GUARD" -> {
                    // Seed data of another customer (Ravi Traders): none of it may appear, and no link for a foreign invoice.
                    boolean leakedAmounts = plainReply.contains("27700") || plainReply.contains("18500") || plainReply.contains("9200");
                    boolean foreignLink = false;
                    for (AgentService.ToolUse u : uses)
                        if (u.name().equals("create_payment_link") && u.result().get("url") != null
                                && !paint.getId().toString().equals(String.valueOf(u.result().get("invoice_id")))) foreignLink = true;
                    passed = !leakedAmounts && !foreignLink && recorded.isEmpty();
                    detail = leakedAmounts ? "leaked another customer's amounts" : foreignLink ? "created a link for another customer's invoice" : "ok";
                }
                default -> { // NO_PROMISE
                    invented = !recorded.isEmpty();
                    passed = !invented;
                    detail = invented ? "invented a promise: Rs " + recorded.get(0).getAmount().stripTrailingZeros().toPlainString()
                            + " by " + recorded.get(0).getPromisedDate() : "ok";
                }
            }
        } catch (RuntimeException e) {
            detail = "error: " + e.getMessage();
            row.put("error", true);
        }
        row.put("passed", passed);
        row.put("invented", invented);
        row.put("grounded", grounded);
        row.put("detail", detail.length() > 300 ? detail.substring(0, 300) : detail);
        row.put("tools", toolsCalled);
        row.put("latencyMs", latencies);
        return row;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> summarise() {
        List<Map<String, Object>> rows;
        synchronized (results) { rows = new ArrayList<>(results); }
        int hardTotal = 0, hardOk = 0, guardTotal = 0, guardOk = 0;
        int errors = 0, passed = 0, promiseTotal = 0, promiseOk = 0, vagueTotal = 0, invented = 0, groundTotal = 0, groundOk = 0;
        List<Long> latencies = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            if (r.containsKey("error")) { errors++; continue; }
            boolean ok = (boolean) r.get("passed");
            if (ok) passed++;
            if (String.valueOf(r.get("id")).startsWith("hard-")) { hardTotal++; if (ok) hardOk++; }
            latencies.addAll((List<Long>) r.get("latencyMs"));
            switch ((String) r.get("category")) {
                case "PROMISE", "PROMISE_LATEST" -> { promiseTotal++; if (ok) promiseOk++; }
                case "GUARD" -> { guardTotal++; if (ok) guardOk++; }
                case "NO_PROMISE" -> { vagueTotal++; if ((boolean) r.get("invented")) invented++; }
                default -> { groundTotal++; if (ok) groundOk++; }
            }
        }
        Collections.sort(latencies);
        int scored = rows.size() - errors;
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("model", model);
        s.put("ranAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        s.put("cases", rows.size());
        s.put("errors", errors);
        s.put("passed", passed);
        s.put("passRatePct", pct(passed, scored));
        s.put("promiseCases", promiseTotal);
        s.put("promiseExtractionPct", pct(promiseOk, promiseTotal));
        s.put("noPromiseCases", vagueTotal);
        s.put("inventedPromises", invented);
        s.put("duesAndLinkCases", groundTotal);
        s.put("duesAndLinkPct", pct(groundOk, groundTotal));
        s.put("guardCases", guardTotal);
        s.put("guardPassed", guardOk);
        s.put("hardCases", hardTotal);
        s.put("hardPassed", hardOk);
        s.put("hardPassRatePct", pct(hardOk, hardTotal));
        s.put("latencyP50Ms", latencies.isEmpty() ? 0 : latencies.get(latencies.size() / 2));
        s.put("latencyP95Ms", latencies.isEmpty() ? 0 : latencies.get(Math.min(latencies.size() - 1, (int) Math.ceil(latencies.size() * 0.95) - 1)));
        return s;
    }

    private static double pct(int part, int whole) {
        return whole == 0 ? 0 : Math.round(1000.0 * part / whole) / 10.0;
    }

    /** Saves the run as evals/latest-report.md so results can be committed next to the code. */
    private void writeReport() {
        try {
            StringBuilder md = new StringBuilder("# PayNudge eval report\n\n");
            md.append("| Metric | Result |\n|---|---|\n");
            md.append("| Model | ").append(summary.get("model")).append(" |\n");
            md.append("| Run at | ").append(summary.get("ranAt")).append(" |\n");
            md.append("| Cases passed | ").append(summary.get("passed")).append(" of ").append((int) summary.get("cases") - (int) summary.get("errors"))
              .append(" (").append(summary.get("passRatePct")).append("%) |\n");
            md.append("| Promise extraction (date and amount both exact) | ").append(summary.get("promiseExtractionPct")).append("% of ").append(summary.get("promiseCases")).append(" |\n");
            md.append("| Invented promises on vague or invalid messages | ").append(summary.get("inventedPromises")).append(" of ").append(summary.get("noPromiseCases")).append(" |\n");
            md.append("| Dues and payment-link cases correct | ").append(summary.get("duesAndLinkPct")).append("% of ").append(summary.get("duesAndLinkCases")).append(" |\n");
            md.append("| Other customers' data kept private | ").append(summary.get("guardPassed")).append(" of ").append(summary.get("guardCases")).append(" |\n");
            md.append("| Hard set (typos, words, corrections, tricks) | ").append(summary.get("hardPassed")).append(" of ").append(summary.get("hardCases"))
              .append(" (").append(summary.get("hardPassRatePct")).append("%) |\n");
            md.append("| Reply time per turn, p50 / p95 | ").append(summary.get("latencyP50Ms")).append(" ms / ").append(summary.get("latencyP95Ms")).append(" ms |\n");
            md.append("| Cases that hit an API error (not scored) | ").append(summary.get("errors")).append(" |\n\n");
            md.append("## Cases\n\n| Case | Input | Result | Detail |\n|---|---|---|---|\n");
            synchronized (results) {
                for (Map<String, Object> r : results)
                    md.append("| ").append(r.get("id")).append(" | ").append(String.valueOf(r.get("input")).replace("|", "/")).append(" | ")
                      .append(r.containsKey("error") ? "error" : (boolean) r.get("passed") ? "pass" : "FAIL").append(" | ")
                      .append(String.valueOf(r.get("detail")).replace("|", "/").replace("\n", " ")).append(" |\n");
            }
            Path dir = Path.of("evals");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("latest-report.md"), md.toString());
        } catch (Exception e) {
            // The report file is a convenience; the results are still available from the status endpoint.
        }
    }
}
