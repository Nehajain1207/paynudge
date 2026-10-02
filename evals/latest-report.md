# PayNudge eval report

| Metric | Result |
|---|---|
| Model | openai/gpt-oss-120b |
| Run at | 2026-10-02 12:48 |
| Cases passed | 38 of 38 (100.0%) |
| Promise extraction (date and amount both exact) | 100.0% of 16 |
| Invented promises on vague or invalid messages | 0 of 10 |
| Dues and payment-link cases correct | 100.0% of 12 |
| Reply time per turn, p50 / p95 | 7470 ms / 16405 ms |
| Cases that hit an API error (not scored) | 0 |

## Cases

| Case | Input | Result | Detail |
|---|---|---|---|
| dues-1 | How much do I owe? | pass | ok |
| dues-2 | What's my pending amount? | pass | ok |
| dues-3 | Mera kitna baaki hai? | pass | ok |
| dues-4 | Which of my bills are unpaid? | pass | ok |
| dues-5 | Total outstanding please | pass | ok |
| dues-6 | Kitna paisa dena hai mujhe? | pass | ok |
| link-1 | Send me the payment link | pass | ok |
| link-2 | I want to pay now | pass | ok |
| link-3 | Payment ka link bhejo | pass | ok |
| link-4 | Send a link for the brushes bill | pass | ok |
| link-5 | Give me a link to pay the paint supplies invoice | pass | ok |
| link-6 | Pay karna hai, link do | pass | ok |
| promise-1 | I will pay the full amount tomorrow. | pass | ok |
| promise-2 | I'll clear it the day after tomorrow | pass | ok |
| promise-3 | I can pay 5000 in 3 days | pass | ok |
| promise-4 | I will pay the whole bill on 8 October | pass | ok |
| promise-5 | Kal pura payment kar dunga | pass | ok |
| promise-6 | Parso 4000 de dunga | pass | ok |
| promise-7 | I'll pay half of it in a week | pass | ok |
| promise-8 | Give me two weeks, I will pay everything then | pass | ok |
| promise-9 | 10 din mein pura clear kar dunga | pass | ok |
| promise-10 | I'll pay 2500 this coming Tuesday | pass | ok |
| promise-11 | Full payment will be done on 11/10/2026 (dd/mm/yyyy) | pass | ok |
| promise-12 | ok I will pay 8000 on 2026-10-07 | pass | ok |
| promise-13 | I can't pay anything today / Fine, I'll pay 3000 tomorrow | pass | ok |
| promise-14 | I'll pay 1000 tomorrow and the rest some time later | pass | ok |
| promise-15 | Sure, tomorrow morning I'll transfer 12000 | pass | ok |
| promise-16 | I'll settle the full bill in 5 days | pass | ok |
| vague-1 | I'll pay soon | pass | ok |
| vague-2 | Thoda time do, abhi paisa nahi hai | pass | ok |
| vague-3 | Business is slow, let me see what I can do | pass | ok |
| vague-4 | Maybe next month sometime | pass | ok |
| vague-5 | Call me later | pass | ok |
| vague-6 | I already paid this last week | pass | ok |
| vague-7 | Who are you and why are you messaging me? | pass | ok |
| vague-8 | I'll try my best | pass | ok |
| invalid-1 | I will pay on 1 January 2020 | pass | ok |
| invalid-2 | I paid it yesterday | pass | ok |
