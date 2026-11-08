# Technical Architecture: Sauti Sovereign Local-First AI & RAG Stack

## 1. Overview
Sauti is an extractive-free, high-sovereignty LLMOps platform engineered for East African infrastructure.
It eliminates dependence on centralized, high-cost cloud APIs by deploying a local-first Retrieval-Augmented Generation (RAG) architecture paired with a Dual-Tier model hierarchy running on consumer edge devices or domestic VPS instances.

---

## 2. Dual-Tier Model Strategy

```
                               ┌──────────────────────────────────────────────┐
                               │           Sauti Sovereign Gateway            │
                               │          (FastAPI Lifespan Router)           │
                               └──────────────────────┬───────────────────────┘
                                                      │
                       ┌──────────────────────────────┴──────────────────────────────┐
                       ▼                                                             ▼
        ┌─────────────────────────────┐                               ┌─────────────────────────────┐
        │      Tier 1: Micro-Edge     │                               │   Tier 2: Regional Hub      │
        ├─────────────────────────────┤                               ├─────────────────────────────┤
        │ • Model: Qwen 2.5 1.5B/3B   │                               │ • Model: Llama-2-7B-Guanaco │
        │ • Precision: Q4_K_M (GGUF)  │                               │ • Precision: Q4_K_M (GGUF)  │
        │ • Footprint: ~1.1 - 2.1 GB  │                               │ • Footprint: ~4.1 GB RAM    │
        │ • Hardware: Raspberry Pi 5  │                               │ • Hardware: Domestic VPS    │
        │ • Vocab: 151,000 (Multi)    │                               │ • Vocab: 32,000 tokens      │
        └─────────────────────────────┘                               └─────────────────────────────┘
```

### Why Dual-Tier Wins:
1. **Tier 1 (Micro-Edge / Clinic Node)**: Runs on a $35 Raspberry Pi with 4GB RAM. Achieves 20+ tokens/sec on CPU without needing a dedicated GPU. Native 151k vocabulary gives it state-of-the-art multilingual and African dialect transfer.
2. **Tier 2 (Regional Sovereign Hub)**: Runs your fine-tuned `Llama-2-7b-Chat-Guanaco-GGUF` model (4.1 GB RAM footprint) on domestic virtual private servers or regional district servers, providing rich medical and institutional reasoning behind local firewalls.

---

## 3. High-Sovereignty RAG Core

- **Vector Database**: On-premises **Qdrant** (port 6333) with persistent volume storage (`data/qdrant_storage`). Includes an automatic in-memory fallback for extreme edge nodes without Docker.
- **Local Embeddings**: Multilingual normalized 384-dimensional dense vectors (compatible with MiniLM / BGE-M3).
- **Symmetric Context Windowing**: Truncates retrieved chunks conservatively to protect tokenizer context boundaries without losing critical regional nuance.

---

## 4. Vocab & Tensor Dimension Safety
The `src/engine/vocab_guard.py` module pre-validates model GGUF metadata before loading to prevent `check_tensor_dims` dimension crashes and token padding conflicts.
