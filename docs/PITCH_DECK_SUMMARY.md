# Executive Pitch Deck Summary: Sauti

*Designed for Selection Committee Interviews & Mozilla Festival Incubator Showcase*

---

### Slide 1: Title & The Vision
* **Title**: **TALIS** — Sovereign Local-First AI & RAG Gateway for East African Infrastructure
* **Tagline**: Extractive-free, offline-native clinical and institutional intelligence designed at human scale.
* **Founder & Role**: LLMOps & Infrastructure Engineer

---

### Slide 2: The Ground Truth African Problem
* Over **50% of vaccines and temperature-critical pharmaceuticals** spoil across off-grid health facilities in sub-Saharan Africa.
* **The Root Cause**: Cloud AI models fail the moment the rural internet or power goes out. Current dumb data loggers alert nobody until the stock has already decayed.
* **The Injustice**: Sensitive community health data is extracted by hyperscale Western cloud providers without local governance or offline resilience.

---

### Slide 3: The Infrastructure Solution
* **Sauti is an infrastructure intervention**, not a chatbot wrapper.
* An edge appliance running entirely on local $35 hardware (Raspberry Pi 5) or domestic VPS servers.
* **Dual-Tier Model Hierarchy**:
  * **Tier 1 (Micro-Edge)**: 1.5B/3B parameter SLM (1.1 GB RAM) for rural clinics.
  * **Tier 2 (Regional Hub)**: Custom fine-tuned 7B parameter model (4.1 GB RAM) for hospital servers.
* **Agentic RAG Core**: On-premises Qdrant vector retrieval + Arrhenius biochemical decay physics calculations.

---

### Slide 4: Human-in-the-Loop (HITL) Safety
* We do not let AI make unsupervised clinical decisions.
* Critical actions (e.g., vaccine disposal, dose adjustments, emergency power reallocation) are held in an auditable review queue.
* Clinical supervisors review and digitally sign off before irreversible actions are committed.

---

### Slide 5: Real Engineering & Traction
* **21 / 21 Passing Automated Unit & Integration Tests** in **0.39 seconds**.
* Sub-45ms average decision latency with zero cloud dependency.
* Automated zero-touch hardware bootstrap script (`scripts/bootstrap_device.sh`) with auto-architecture probing.

---

### Slide 6: The 12-Month Plan ($300,000 USD)
* **Q1**: Hardening local Swahili & English vector corpora and Kaggle training recipes.
* **Q2**: Field deployment across 15 rural clinics in East Africa.
* **Q3**: Launching automated edge active-learning cycles and supervisor mobile app.
* **Q4**: Open-source commons release and Mozilla Festival demo day.
