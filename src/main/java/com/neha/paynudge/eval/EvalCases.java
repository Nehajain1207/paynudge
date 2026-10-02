package com.neha.paynudge.eval;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * The eval set. Dates are relative to today so the suite never goes stale.
 * DUES and LINK cases use a customer with two unpaid invoices (paint 12000, brushes 6000);
 * PROMISE and NO_PROMISE cases use a customer with one unpaid invoice of 12000.
 */
final class EvalCases {
    private EvalCases() {}

    static final String PAINT = "Paint supplies";
    static final String BRUSHES = "Brushes and rollers";

    static List<EvalCase> all() {
        LocalDate t = LocalDate.now();
        String weekday = t.plusDays(4).getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String dayMonth = t.plusDays(6).format(DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH));
        String slashDate = t.plusDays(9).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        return List.of(
            EvalCase.dues("dues-1", "How much do I owe?"),
            EvalCase.dues("dues-2", "What's my pending amount?"),
            EvalCase.dues("dues-3", "Mera kitna baaki hai?"),
            EvalCase.dues("dues-4", "Which of my bills are unpaid?"),
            EvalCase.dues("dues-5", "Total outstanding please"),
            EvalCase.dues("dues-6", "Kitna paisa dena hai mujhe?"),

            EvalCase.link("link-1", "Send me the payment link", null),
            EvalCase.link("link-2", "I want to pay now", null),
            EvalCase.link("link-3", "Payment ka link bhejo", null),
            EvalCase.link("link-4", "Send a link for the brushes bill", BRUSHES),
            EvalCase.link("link-5", "Give me a link to pay the paint supplies invoice", PAINT),
            EvalCase.link("link-6", "Pay karna hai, link do", null),

            EvalCase.promise("promise-1", t.plusDays(1), "12000", "I will pay the full amount tomorrow."),
            EvalCase.promise("promise-2", t.plusDays(2), "12000", "I'll clear it the day after tomorrow"),
            EvalCase.promise("promise-3", t.plusDays(3), "5000", "I can pay 5000 in 3 days"),
            EvalCase.promise("promise-4", t.plusDays(6), "12000", "I will pay the whole bill on " + dayMonth),
            EvalCase.promise("promise-5", t.plusDays(1), "12000", "Kal pura payment kar dunga"),
            EvalCase.promise("promise-6", t.plusDays(2), "4000", "Parso 4000 de dunga"),
            EvalCase.promise("promise-7", t.plusDays(7), "6000", "I'll pay half of it in a week"),
            EvalCase.promise("promise-8", t.plusDays(14), "12000", "Give me two weeks, I will pay everything then"),
            EvalCase.promise("promise-9", t.plusDays(10), "12000", "10 din mein pura clear kar dunga"),
            EvalCase.promise("promise-10", t.plusDays(4), "2500", "I'll pay 2500 this coming " + weekday),
            EvalCase.promise("promise-11", t.plusDays(9), "12000", "Full payment will be done on " + slashDate + " (dd/mm/yyyy)"),
            EvalCase.promise("promise-12", t.plusDays(5), "8000", "ok I will pay 8000 on " + t.plusDays(5)),
            EvalCase.promise("promise-13", t.plusDays(1), "3000", "I can't pay anything today", "Fine, I'll pay 3000 tomorrow"),
            EvalCase.promise("promise-14", t.plusDays(1), "1000", "I'll pay 1000 tomorrow and the rest some time later"),
            EvalCase.promise("promise-15", t.plusDays(1), "12000", "Sure, tomorrow morning I'll transfer 12000"),
            EvalCase.promise("promise-16", t.plusDays(5), "12000", "I'll settle the full bill in 5 days"),

            EvalCase.noPromise("vague-1", "I'll pay soon"),
            EvalCase.noPromise("vague-2", "Thoda time do, abhi paisa nahi hai"),
            EvalCase.noPromise("vague-3", "Business is slow, let me see what I can do"),
            EvalCase.noPromise("vague-4", "Maybe next month sometime"),
            EvalCase.noPromise("vague-5", "Call me later"),
            EvalCase.noPromise("vague-6", "I already paid this last week"),
            EvalCase.noPromise("vague-7", "Who are you and why are you messaging me?"),
            EvalCase.noPromise("vague-8", "I'll try my best"),
            EvalCase.noPromise("invalid-1", "I will pay on 1 January 2020"),
            EvalCase.noPromise("invalid-2", "I paid it yesterday")
        );
    }
}
