package com.neha.paynudge;

import com.neha.paynudge.agent.AgentService;
import com.neha.paynudge.repo.CustomerRepository;
import com.neha.paynudge.repo.PaymentLinkRepository;
import com.neha.paynudge.repo.PaymentPromiseRepository;
import com.neha.paynudge.tools.CollectionTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the whole app against the offline mock model (no API key needed). */
@SpringBootTest(properties = "paynudge.llm.api-key=")
class PayNudgeFlowTests {
    @Autowired AgentService agent;
    @Autowired CollectionTools tools;
    @Autowired CustomerRepository customers;
    @Autowired PaymentLinkRepository links;
    @Autowired PaymentPromiseRepository promises;

    Long customerId;

    @BeforeEach
    void pickCustomer() { customerId = customers.findAll().get(0).getId(); }

    @Test
    void askingForDuesCallsTheDuesTool() {
        AgentService.ChatResult result = agent.chat(customerId, "How much do I owe?");
        assertEquals("get_dues", result.toolsUsed().get(0).name());
        assertTrue(result.reply().contains("total_due"));
    }

    @Test
    void sameInvoiceAlwaysGetsTheSamePaymentLink() {
        Map<String, Object> first = tools.createPaymentLink(customerId, null);
        Map<String, Object> second = tools.createPaymentLink(customerId, null);
        assertEquals(first.get("payment_link_id"), second.get("payment_link_id"));
        assertEquals(first.get("url"), second.get("url"));
    }

    @Test
    void hundredConcurrentRetriesCreateExactlyOneLink() throws Exception {
        Long otherCustomer = customers.findAll().get(1).getId();
        long before = links.count();
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < 100; i++)
            pool.submit(() -> { start.await(); return tools.createPaymentLink(otherCustomer, null); });
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals(before + 1, links.count());
    }

    @Test
    void promiseIsStoredOnceAndRememberedNextChat() {
        String date = LocalDate.now().plusDays(3).toString();
        long before = promises.findByCustomerIdOrderByIdDesc(customerId).size();
        agent.chat(customerId, "I will pay on " + date);
        agent.chat(customerId, "As I said, I will pay on " + date);
        assertEquals(before + 1, promises.findByCustomerIdOrderByIdDesc(customerId).size());
    }

    @Test
    void pastDatesAreRejected() {
        Map<String, Object> result = tools.recordPromise(customerId, null, null, LocalDate.now().minusDays(1).toString());
        assertTrue(result.containsKey("error"));
    }
}
