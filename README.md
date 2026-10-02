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
- **Memory across chats.** Promises are stored and added to the prompt of every new conversation.
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

Open http://localhost:8080/evals.html and click **Run eval suite**. It runs 38 scripted conversations
(English and Hinglish) against the configured model. Each case gets a fresh customer, and is scored on what the
agent **did** (tools called, rows written), not on wording:

- **Promise extraction**: 16 clear commitments ("Parso 4000 de dunga", "half of it in a week"); the stored date and amount must both be exact.
- **Invented promises**: 10 vague or invalid messages ("I'll pay soon", a date in the past); nothing may be recorded.
- **Dues and payment links**: 12 cases; the right tool, the right invoice, and the reply must contain the real amount or link.
- **Reply time** per turn (p50 / p95).

### Results (2 Oct 2026, `openai/gpt-oss-120b` on Groq, single run)

| Metric | Result |
|---|---|
| Cases passed | 38 of 38 |
| Promise extraction (date and amount both exact) | 16 of 16 |
| Invented promises on vague or invalid messages | 0 of 10 |
| Dues and payment-link cases correct | 12 of 12 |
| Reply time, one-tool turn (no rate-limit waits) | about 1.1 to 1.4 s |
| Reply time, two-tool turn (no rate-limit waits) | about 1.9 s |

The run used a free API tier, so most later turns include rate-limit waits (overall p50 7.5 s, p95 16.4 s);
the unthrottled figures above come from the first ten cases. This is a small set and one run, so treat it as a
regression check, not a benchmark. Full per-case output: [`evals/latest-report.md`](evals/latest-report.md).

## API

| Method | Path | What it does |
|---|---|---|
| GET | `/api/customers` | List customers |
| GET | `/api/customers/{id}/dues` | Unpaid invoices and total |
| GET | `/api/customers/{id}/promises` | Recorded promises |
| GET | `/api/customers/{id}/messages` | Chat history |
| POST | `/api/customers/{id}/chat` | Send a message to the agent |
| POST | `/api/evals/run` | Start the eval suite |
| GET | `/api/evals/status` | Eval progress and results |

## Stack

Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL (Docker Compose), H2 for tests, any OpenAI-compatible model API (Gemini, Groq), JUnit 5.

## Roadmap

- [ ] Post-chat summary: intent, promised date, amount, sentiment as structured data
- [x] Eval suite of scripted conversations (extraction accuracy, invented promises)
- [x] PostgreSQL with Docker Compose
- [x] Automatic retries when the model API is busy
- [ ] Redis caching
- [ ] React front end
