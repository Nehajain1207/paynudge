# PayNudge – AI Payment Collection Agent

A chat agent that follows up on overdue invoices for a small business.
The customer chats in plain language; the agent looks up what is due, shares a payment link,
and records the date the customer promises to pay. It remembers that promise in the next chat.

## How it works

```
customer message
      │
      ▼
 AgentService ──► LLM (Gemini, OpenAI-compatible API)
      ▲                │ "call get_dues" / "call record_promise"
      │                ▼
      └────────── CollectionTools ──► database
```

1. The message, recent chat history and the customer's earlier promises go to the model.
2. The model either replies or asks for a **tool**: `get_dues`, `create_payment_link`, `record_promise`.
3. The server runs the tool, returns the result to the model, and repeats (up to 5 steps).

Design choices:

- **The model never touches the database.** It only picks a tool and arguments. The server checks
  that the invoice belongs to the customer, that dates are real and not in the past, and that amounts are valid.
- **The customer id comes from the server**, never from the model, so one customer can't see another's dues.
- **Idempotent tools.** Each payment link and promise has a unique idempotency key, so retries and
  concurrent duplicate requests create exactly one row (covered by a 100-thread test).
- **One live promise per invoice.** If a customer changes their mind, the new promise supersedes the old one;
  the eval suite's first hard run surfaced this (the latest promise was right, but the stale one stayed active).
- **Memory across chats.** Promises are stored and added to the prompt of every new conversation.
- **Post-chat summary.** One click turns a chat into intent, sentiment and a next action. The model only judges the
  conversation; the promised date and amount come from the validated promise row, so a summary cannot contain an invented promise.
- **Offline mock model.** With no API key the app runs on a rule-based stand-in, so tests are free and repeatable.

## Run it

Needs Java 17+.

```bash
# optional: use a real model (free Gemini key from aistudio.google.com/apikey)
set GEMINI_API_KEY=your-key        # Windows
export GEMINI_API_KEY=your-key     # macOS / Linux

./mvnw spring-boot:run
```

Open http://localhost:8080 and try: *"How much do I owe?"*, *"Send me a link to pay"*, *"I'll pay 5000 next Friday"*.

```bash
./mvnw test
```

### With PostgreSQL (data survives restarts)

```bash
docker compose up -d
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=postgres"
```

## Evals

Open http://localhost:8080/evals.html and click **Run eval suite**. It runs 61 scripted conversations: a base set of 38 and a hard set of 23
(English and Hinglish) against the configured model. Each case gets a fresh customer, and is scored on what the
agent **did** (tools called, rows written), not on wording:

- **Promise extraction**: 16 clear commitments ("Parso 4000 de dunga", "half of it in a week"); the stored date and amount must both be exact.
- **Invented promises**: 10 vague or invalid messages ("I'll pay soon", a date in the past); nothing may be recorded.
- **Dues and payment links**: 12 cases; the right tool, the right invoice, and the reply must contain the real amount or link.
- **Hard set**: typos, amounts in words ("paanch hazaar"), a customer changing their mind, a prompt-injection attempt,
  and requests for another customer's data (which must not leak).
- **Reply time** per turn (p50 / p95).

### Results (2 Oct 2026, `openai/gpt-oss-120b` on Groq)

| Set | Run | Passed | Notes |
|---|---|---|---|
| Base (38 cases) | 1 | 38 of 38 | 16 of 16 promises exact, 0 invented promises in 10 vague or invalid cases |
| Hard (23 cases) | 1 | 23 of 23 | Scoring was too lenient: a changed promise left the stale one active |
| Hard (23 cases) | 2 | 22 of 23 | After the supersede fix and stricter check. "1.5k parso" was recorded as tomorrow |
| Hard (23 cases) | 3 | 23 of 23 | After adding a Hinglish date glossary to the prompt and setting temperature to 0 |

What the suite found:

- **Stale promises.** When a customer changed their mind, the old promise stayed active. Now a newer promise for the same invoice supersedes it.
- **A flaky Hinglish date.** "parso" (day after tomorrow) passed in one run and failed in the next with no related code change. Fixed with a short glossary in the prompt and temperature 0.
- **No leaks, no invented promises.** Across all runs: 0 invented promises in 17 vague or adversarial cases (including a prompt-injection attempt), and 2 of 2 requests for another customer's data refused.

Honest limits: this is a small suite on one model, the base set has not been re-run since the prompt changed, and reply times
(p50 about 6 to 7 s) are dominated by free-tier rate-limit waits; unthrottled turns took about 1.1 to 1.9 s.
Per-case output of the latest run: [`evals/latest-report.md`](evals/latest-report.md).

## API

| Method | Path | What it does |
|---|---|---|
| GET | `/api/customers` | List customers |
| GET | `/api/customers/{id}/dues` | Unpaid invoices and total |
| GET | `/api/customers/{id}/promises` | Recorded promises |
| GET | `/api/customers/{id}/messages` | Chat history |
| POST | `/api/customers/{id}/chat` | Send a message to the agent |
| POST | `/api/customers/{id}/summary` | Summarise the chat into structured data |
| GET | `/api/customers/{id}/summaries` | Past summaries |
| POST | `/api/evals/run` | Start the eval suite |
| GET | `/api/evals/status` | Eval progress and results |

## Stack

Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL (Docker Compose), H2 for tests, any OpenAI-compatible model API (Gemini, Groq), JUnit 5.

## Roadmap

- [x] Post-chat summary: intent, sentiment, next action, with promise facts taken from the database
- [x] Eval suite of scripted conversations (extraction accuracy, invented promises)
- [x] PostgreSQL with Docker Compose
- [x] Automatic retries when the model API is busy
- [ ] Redis caching
- [ ] React front end
