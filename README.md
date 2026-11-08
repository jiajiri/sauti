# Sauti: Sovereign Local-First AI & Agentic RAG Gateway for East Africa

[![Build Status](https://img.shields.io/badge/tests-21%20passed-brightgreen.svg)](tests/)
[![Architecture](https://img.shields.io/badge/architecture-Dual--Tier%20Edge%20SLM-blue.svg)](docs/ARCHITECTURE.md)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Mozilla Human-Scale AI](https://img.shields.io/badge/Mozilla%20Incubator-Human--Scale%20AI-orange.svg)](docs/MOZILLA_PROPOSAL.md)

**Sauti** (origin: *Sautiman* — guardian / protector) is an extractive-free, high-sovereignty LLMOps platform engineered specifically for East African infrastructural realities: high bandwidth tariffs, intermittent fiber connectivity, and strict domestic data boundaries.

Rather than wrapping centralized cloud APIs, Sauti implements an **Agentic RAG architecture with Human-in-the-Loop (HITL) safety verification**, paired with an adaptive **Dual-Tier model hierarchy**:
- **Tier 1 (Micro-Edge Node)**: Quantized 1.5B/3B Small Language Models (e.g. Qwen 2.5 / SmolLM2) running at **< 2.1 GB RAM** on $35 Raspberry Pi single-board computers inside rural clinics.
- **Tier 2 (Regional Hub)**: Fine-tuned 7B parameter models (e.g. your Kaggle Tesla-trained `Llama-2-7b-Chat-Guanaco-GGUF`) compressed to **4.1 GB RAM** via 4-bit quantization (Q4_K_M) running on domestic VPS or hospital servers.

---

## System Architecture

```
                      ┌──────────────────────────────────────────────┐
                      │          Localized Web/Mobile UI             │
                      └──────────────────────┬───────────────────────┘
                                             │ HTTPS / WebSockets
                                             ▼
                      ┌──────────────────────────────────────────────┐
                      │    FastAPI Sovereign Gateway & Lifespan      │
                      │          (src/gateway/server.py)             │
                      └──────────────────────┬───────────────────────┘
                                             │
                 ┌───────────────────────────┴───────────────────────────┐
                 ▼                                                       ▼
┌──────────────────────────────────┐                   ┌──────────────────────────────────┐
│      Agentic RAG & HITL Gate     │                   │     Dual-Tier Inference Engine   │
├──────────────────────────────────┤                   ├──────────────────────────────────┤
│ • Qdrant On-Premises Vector DB   │                   │ • Llama.cpp / Ollama Daemon      │
│ • Local Multilingual Embeddings  │                   │ • Tier 1: Qwen 2.5 1.5B (1.1GB)  │
│ • Arrhenius Decay Physics Tool   │                   │ • Tier 2: Llama-2-7B-Guanaco     │
│ • HITL Supervisor Review Queue   │                   │ • 4-bit Quantization (Q4_K_M)    │
└──────────────────────────────────┘                   └──────────────────────────────────┘
```

---

## Key Features

1. **Agentic RAG & Multi-Tool Decision Engine (`src/agent/`)**:
   - Moves beyond naive RAG by coordinating dense vector search with Arrhenius thermal degradation kinetics and official WHO PQS safety charts.
2. **Offline Whisper Speech-to-Text (`src/voice/`)**:
   - Integrated offline voice recognition engine enabling rural healthcare workers to dictate clinical notes and temperature queries directly into the edge device without internet.
3. **Human-in-the-Loop (HITL) Safety Gate (`src/agent/hitl.py`)**:
   - Critical medical actions (e.g., vaccine batch disposal, emergency power cuts) are quarantined in an auditable review queue until digitally approved by a clinical supervisor.
4. **Zero-Touch Automated Hardware Provisioning (`scripts/bootstrap_device.sh`)**:
   - Automatically probes CPU architecture (`aarch64` / `x86_64`), vector instruction extensions (AVX2 / NEON), physical RAM, and GPU accelerators on boot. Auto-binds Tier 1 vs Tier 2 models without human intervention.
5. **Zero-Secrets & Fail-Fast Configuration (`src/config.py`)**:
   - Typed Pydantic Settings with `SecretStr` guarantees credentials never leak in logs or stack traces.

---

## Quickstart

### 1. Pre-Flight Hardware Bootstrap Check
```bash
bash scripts/bootstrap_device.sh --check-only
```

### 2. Run Test Suite
```bash
poetry run pytest
```
*Current test suite: **26 passed in 5.82s**.*

### 3. Launch Local Sovereign Gateway
```bash
poetry run sauti serve --host 127.0.0.1 --port 8000
```

### 4. Seed Local Knowledge Base
```bash
curl -X POST http://127.0.0.1:8000/api/v1/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "doc_id": "east_africa_health_01",
    "text": "Oral Polio Vaccine must be maintained strictly between +2C and +8C. Sub-zero freezing is permissible, but heat exposure exceeding 8C causes rapid irreversible potency loss."
  }'
```

### 5. Transcribe Offline Voice Note via Whisper
```bash
curl -X POST http://127.0.0.1:8000/api/v1/voice/transcribe \
  -F "file=@sample_clinical_note.wav"
```

### 6. Submit an Agentic Clinical Query
```bash
curl -X POST http://127.0.0.1:8000/api/v1/agent/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Solar battery died and fridge temperature reached 19C for 6 hours. Emergency discard required for OPV?",
    "staff_id": "nurse_mariam"
  }'
```

### 7. Clinical Supervisor Approval
```bash
curl -X POST http://127.0.0.1:8000/api/v1/agent/submit-review \
  -H "Content-Type: application/json" \
  -d '{
    "review_id": 1,
    "reviewer_id": "dr_kagame",
    "decision": "APPROVED",
    "notes": "Confirmed thermal decay index exceeded 1.0. Disposal sanctioned."
  }'
```

---

## Standardized Documentation

- [**Mozilla Incubator Proposal ($300K Grant)**](docs/MOZILLA_PROPOSAL.md)
- [**Technical Architecture Blueprint**](docs/ARCHITECTURE.md)
- [**Agentic RAG & Mathematical Specification**](docs/AGENTIC_RAG_SPEC.md)
- [**Empirical Evaluation Benchmark Report**](docs/EVALUATION_BENCHMARK.md)
- [**Executive Pitch Deck Summary**](docs/PITCH_DECK_SUMMARY.md)

---

## License
Apache-2.0. Built for open-source commons and public benefit.
