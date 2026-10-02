# PayNudge – AI Payment Collection Agent

**Status: in progress.** A chat agent that follows up on overdue invoices for a small business.
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

## API

| Method | Path | What it does |
|---|---|---|
| GET | `/api/customers` | List customers |
| GET | `/api/customers/{id}/dues` | Unpaid invoices and total |
| GET | `/api/customers/{id}/promises` | Recorded promises |
| GET | `/api/customers/{id}/messages` | Chat history |
| POST | `/api/customers/{id}/chat` | Send a message to the agent |

## Stack

Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL (Docker Compose), H2 for tests, Gemini via OpenAI-compatible API, JUnit 5.

## Roadmap

- [ ] Post-chat summary: intent, promised date, amount, sentiment as structured data
- [ ] Eval suite of scripted conversations (extraction accuracy, invented promises)
- [x] PostgreSQL with Docker Compose
- [x] Automatic retries when the model API is busy
- [ ] Redis caching
- [ ] React front end
