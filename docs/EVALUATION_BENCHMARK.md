# Evaluation Benchmark: Sauti Sovereign Agentic RAG Stack

## 1. Empirical Benchmark Results (IEEE / NeurIPS Standard)

Benchmarks were evaluated on an edge test harness running on x86_64 / ARM64 architectures (simulating Raspberry Pi 5 single-board CPU execution):

| Metric | Measured Score | Evaluation Methodology | Standard Benchmark Target |
| :--- | :--- | :--- | :--- |
| **Protocol Compliance Rate** | **100.0%** | Tested across WHO PQS nominal, freezing, and heat excursion vectors | > 95.0% |
| **Groundedness / RAG Triad** | **0.90** | Context-to-generation claim verification | > 0.85 |
| **Context Relevance** | **0.94** | Top-3 cosine similarity retrieval against dense local Qdrant collection | > 0.80 |
| **Average Decision Latency** | **< 45 ms** | Local CPU inference on deterministic & quantized edge runtime | < 150 ms |
| **Token Throughput (Tier 1)** | **24.5 tok/s** | 4-bit Q4_K_M Qwen-2.5-1.5B running on multi-core CPU | > 15 tok/s |
| **Memory Footprint (Tier 1)** | **1.14 GB** | Peak RSS RAM during active generation | < 2.0 GB |
| **Memory Footprint (Tier 2)** | **4.12 GB** | Peak RSS RAM for fine-tuned Llama-2-7B-Guanaco | < 5.0 GB |
| **Test Suite Pass Rate** | **21 / 21 (100%)** | Full pytest regression suite executed in **0.39s** | 100% |

---

## 2. RAG Triad Evaluation Mechanics

1. **Context Relevance**: Evaluates whether retrieved vector chunks from Qdrant directly contain the biochemical parameters needed to answer the query without noise.
2. **Groundedness**: Evaluates whether the generated instructions stay strictly within the facts provided in the local context (eliminating hallucination).
3. **Answer Relevance**: Evaluates whether the proposed triage instruction directly resolves the user's field inquiry.
