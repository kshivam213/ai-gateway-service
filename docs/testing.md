# Testing Guide

## Prerequisites

- Service running on `http://localhost:8095`
- PostgreSQL running with schema `aigateway` migrated (Liquibase runs on startup)
- Valid `OPENAI_API_KEY` set in environment or `application.yml`

---

## 1. Register a Tool

```bash
curl --location 'http://localhost:8095/tools/register' \
--header 'Content-Type: application/json' \
--data '{
  "name": "getPaymentBundles",
  "description": "Fetch payment bundles for a company. Supports optional creation-date range filtering and an optional referenceId.",
  "url": "http://localhost:8091/tools/getPaymentBundles",
  "method": "POST",
  "headers": {
    "Authorization": "Bearer local-dev-secret-token"
  },
  "parameters": {
    "type": "object",
    "properties": {
      "companyId": {
        "type": "string",
        "description": "Identifier of the company whose payment bundles should be retrieved."
      },
      "createdDateRangeStart": {
        "type": "string",
        "format": "date-time",
        "description": "Inclusive lower bound on bundle creation time, ISO-8601 instant (e.g. 2025-04-01T00:00:00Z). Optional."
      },
      "createdDateRangeEnd": {
        "type": "string",
        "format": "date-time",
        "description": "Inclusive upper bound on bundle creation time, ISO-8601 instant (e.g. 2025-04-30T23:59:59Z). Must be greater than or equal to createdDateRangeStart. Optional."
      },
      "referenceId": {
        "type": "string",
        "description": "Optional external reference identifier to filter by."
      }
    },
    "required": ["companyId"],
    "additionalProperties": false
  }
}'
```

**List all tools**

```bash
curl http://localhost:8095/tools
```

---

## 2. Create an Agent

```bash
curl --location 'http://localhost:8095/agents' \
--header 'Content-Type: application/json' \
--data '{
  "agentId": "payment-bundle-agent",
  "name": "Payment Bundle Agent",
  "systemPrompt": "You are a payment operations assistant helping users understand payment bundle data.\n\nIMPORTANT RULES:\n\n1. NEVER return raw JSON or full unformatted data.\n2. Always present results in a clean, human-readable format.\n3. If the result contains many records:\n   - First provide a SUMMARY\n   - Then highlight ONLY important items (max 5)\n4. Focus on insights:\n   - failed payments\n   - processing delays\n   - unusual patterns\n\n5. Use formatting:\n   - bullet points\n   - short sections\n   - clear labels\n\n6. For follow-up questions:\n   - use previous context\n   - do NOT ask user to repeat information\n\n7. When calling tools:\n   - extract required fields correctly\n   - do NOT hallucinate values\n\n---\n\nRESPONSE STYLE:\n\nInstead of listing everything, respond like:\n\nSummary:\n- Total bundles: X\n- Failed: X\n- Processing: X\n- Paid: X\n\nImportant Findings:\n- List only key items (failures first)\n\nEnd with:\n\"Would you like details for any specific bundle?\"",
  "model": "gpt-4o-mini",
  "tools": ["getPaymentBundles"]
}'
```

**List all agents**

```bash
curl http://localhost:8095/agents
```

---

## 3. Run the Agent

### Turn 1 — initial query

```bash
curl --location 'http://localhost:8095/agents/run' \
--header 'Content-Type: application/json' \
--data '{
  "agentId": "payment-bundle-agent",
  "input": "Can you please provide bundle status having company id as 508473?",
  "context": {},
  "metadata": {
    "sessionId": "demo-123"
  }
}'
```

### Turn 2 — follow-up in the same session

Same `sessionId` — the agent remembers the previous exchange.

```bash
curl --location 'http://localhost:8095/agents/run' \
--header 'Content-Type: application/json' \
--data '{
  "agentId": "payment-bundle-agent",
  "input": "Which of those bundles failed?",
  "context": {},
  "metadata": {
    "sessionId": "demo-123"
  }
}'
```

### New session — fresh conversation

Change `sessionId` to start with no prior context.

```bash
curl --location 'http://localhost:8095/agents/run' \
--header 'Content-Type: application/json' \
--data '{
  "agentId": "payment-bundle-agent",
  "input": "Show me bundles for company 508473",
  "context": {},
  "metadata": {
    "sessionId": "demo-456"
  }
}'
```

> **Note:** If `sessionId` is omitted, all callers share a single `"default"` session. Always pass an explicit `sessionId` in multi-user scenarios.

---

## Session vs Agent Run

| Concept | What it is | Lives in |
|---|---|---|
| Session | A conversation — groups multiple turns | `session_messages` |
| Agent Run | A single request/response pair | `agent_runs` |

One `sessionId` maps to many `agent_runs`. The LLM sees the full session history on every turn.

---

## Verify Persistence

After running the agent, check the DB directly:

```sql
-- Tools registered
SELECT name, url, method FROM aigateway.tools;

-- Agents registered
SELECT agent_id, name, model FROM aigateway.agents;

-- Agent tool assignments
SELECT agent_id, tool_name FROM aigateway.agent_tools;

-- Every run for a session
SELECT request_id, input, status, total_tokens, latency_ms, created_at
FROM aigateway.agent_runs
WHERE session_id = 'demo-123'
ORDER BY created_at;

-- Conversation history for a session
SELECT sequence_num, role, content
FROM aigateway.session_messages
WHERE session_id = 'demo-123'
ORDER BY sequence_num;
```
