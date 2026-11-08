# Mozilla Foundation Human-Scale AI Incubator — Grant Proposal

## 1. Project Title & Summary
**Sauti: Sovereign Local-First AI & RAG Gateway for East African Infrastructure**

**Sauti** (origin: *Sautiman* — guardian / protector) is an extractive-free, high-sovereignty LLMOps platform engineered specifically for East African infrastructural realities: high bandwidth tariffs, intermittent fiber connectivity, and strict domestic data boundaries. Rather than wrapping centralized cloud APIs, Sauti implements a sovereign local-first Retrieval-Augmented Generation (RAG) architecture paired with a Dual-Tier model hierarchy:
- **Tier 1 (Micro-Edge Node)**: Quantized 1.5B/3B Small Language Models (e.g. Qwen 2.5 / SmolLM2) running at < 2.1 GB RAM footprint on cheap $35 Raspberry Pi hardware inside rural clinics.
- **Tier 2 (Regional Hub)**: Fine-tuned 7B parameter models (e.g. Llama-2-7b-Chat-Guanaco) compressed to 4.1 GB RAM via 4-bit quantization (Q4_K_M) running on domestic VPS or hospital servers.

Coupled with a local Qdrant vector store and multilingual embeddings, Sauti delivers offline clinical triage, administrative assistance, and local language reasoning without any patient or institutional data crossing international borders.

---

## 2. Answers to Mozilla Incubator Evaluator Questions

### Question 1: Describe your "Small" design choices.
**Winning Answer:**
"Our entire platform architecture is built to challenge the current paradigm of centralized, high-latency, and extractive cloud-based AI. We run a dual-tier model hierarchy optimized down to 4-bit precision (Q4_K_M) using an embedded llama.cpp runtime: an ultra-compact 1.5B parameter model (Qwen 2.5) compressed to 1.1 GB of RAM for $35 single-board edge devices in rural clinics, and a custom fine-tuned 7B parameter model (Llama-2-7b-chat-guanaco) compressed to 4.1 GB of RAM for regional hospital servers.
By intentionally choosing optimized GGUF formats over unquantized weights, we remove the requirement for expensive, high-end server GPUs, allowing the entire stack to operate on consumer-grade edge hardware or domestic virtual private servers (VPS) within East Africa. For retrieval, we pair this with a local instance of Qdrant utilizing lightweight, multilingual embeddings (BGE-M3/MiniLM). This combination ensures that the entire lifecycle—from tokenization to vector search and generation—happens completely within a deterministic resource boundary, making AI deployment economically viable for resource-constrained local infrastructure."

### Question 2: Describe your approach to Data Governance and Privacy.
**Winning Answer:**
"Our system enforces absolute data minimization by running completely offline or within local private networks. Because inference and vector retrieval happen entirely on local hardware, sensitive user data never crosses international borders or leaks into third-party cloud APIs for training.
We maintain data sovereignty by ensuring that local organizations retain complete control over their knowledge bases. No data is cached or stored outside the user's explicit infrastructure boundary. This layout complies with strict data localization frameworks while preventing the digital extraction of local language nuances and institutional data by centralized tech monopolies."

### Question 3: How does this project serve a public, charitable benefit?
**Winning Answer:**
"Mainstream AI models are fundamentally optimized for Western infrastructure, high-bandwidth access, and dominant languages. By building an open-source, resource-efficient local RAG engine, we are actively preventing systemic technological exclusion in East Africa.
Our framework allows local clinics, legal aid groups, and educational initiatives to securely deploy tailored AI systems using local documentation and language contexts—without recurring subscription fees or reliance on an active international fiber connection. We are democratizing the underlying LLMOps pipeline so that local developers can build, run, and scale their own secure tools, creating true technological equity."

---

## 3. 12-Month Budget Allocation ($300,000 USD Phase 1)

| Category | Allocation | Strategic Purpose |
| :--- | :--- | :--- |
| **Engineering Salaries** | **$140,000** | 1 Full-Time LLMOps/Infra Lead + 1 Full-Time Full-Stack/Product Developer for 12 months. |
| **Local Compute & Cluster Infra** | **$60,000** | Co-location hosting, regional VPS nodes, and purchasing standalone edge hardware configurations (50+ Raspberry Pi kits) for field deployments. |
| **Data Pipelines & Local Vetting** | **$40,000** | Curation, tokenization, and manual translation/validation of high-quality local datasets (Swahili health and legal contexts). |
| **Community Deployment & Training** | **$30,000** | Running 3 field pilots with local target organizations (clinics, legal clinics, schools) to prove ground-truth user traction. |
| **Operational Overhead & Legal** | **$30,000** | Data compliance audits, local legal frameworks, and foundational non-profit / cooperative corporate setup. |
| **TOTAL** | **$300,000** | Complete Year 1 Execution Runway |
