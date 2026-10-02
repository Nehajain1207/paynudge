package com.neha.paynudge;

import com.neha.paynudge.model.Customer;
import com.neha.paynudge.model.Invoice;
import com.neha.paynudge.repo.CustomerRepository;
import com.neha.paynudge.repo.InvoiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Adds a few demo customers and unpaid invoices on first start. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final CustomerRepository customers;
    private final InvoiceRepository invoices;

    public DataSeeder(CustomerRepository customers, InvoiceRepository invoices) {
        this.customers = customers; this.invoices = invoices;
    }

    @Override
    public void run(String... args) {
        if (customers.count() > 0) return;
        Customer ravi = customers.save(new Customer("Ravi Traders", "+91-9000000001"));
        Customer meena = customers.save(new Customer("Meena Textiles", "+91-9000000002"));
        Customer arjun = customers.save(new Customer("Arjun Electricals", "+91-9000000003"));
        LocalDate today = LocalDate.now();
        invoices.save(new Invoice(ravi.getId(), "Steel rods, 40 units", new BigDecimal("18500"), today.minusDays(21)));
        invoices.save(new Invoice(ravi.getId(), "Cement, 25 bags", new BigDecimal("9200"), today.minusDays(6)));
        invoices.save(new Invoice(meena.getId(), "Cotton fabric rolls", new BigDecimal("42000"), today.minusDays(35)));
        invoices.save(new Invoice(arjun.getId(), "Wiring and switches", new BigDecimal("6750"), today.plusDays(4)));
    }
}
