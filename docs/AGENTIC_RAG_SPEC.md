# Technical Specification: Sauti Agentic RAG & Human-in-the-Loop (HITL) Architecture

## 1. Mathematical Formulation & Retrieval Mechanics

Classic naive RAG maps an input query $q$ to a top-$k$ set of vector documents $D_k = \text{TopK}(\text{Sim}(E(q), E(d)))$. In resource-constrained East African clinical and institutional environments, this approach fails due to semantic drift, noisy field terms, and hallucinated actions.

Sauti implements **Agentic RAG with Self-Correction and Hybrid Tool Orchestration**:

$$\text{Action} = \mathcal{A}(q, \mathcal{T}_{\text{Qdrant}}, \mathcal{T}_{\text{Arrhenius}}, \mathcal{T}_{\text{WHO\_PQS}})$$

### The Multi-Tool Action Space:
1. **$\mathcal{T}_{\text{Qdrant}}$**: High-density dense semantic retrieval across local Swahili and English health repositories using normalized cosine similarity:
   $$\text{Sim}(u, v) = \frac{u \cdot v}{\|u\|_2 \|v\|_2}$$
2. **$\mathcal{T}_{\text{Arrhenius}}$**: Kinetic biochemical potency degradation equation modeling cumulative thermal exposure:
   $$k = A \exp\left(-\frac{E_a}{R T}\right) \implies \text{Index} = \frac{\Delta t \cdot \exp(0.08 \cdot \Delta T \cdot \gamma_{\text{biologic}})}{\tau_{\text{critical}}}$$
3. **$\mathcal{T}_{\text{WHO\_PQS}}$**: Deterministic validation against official WHO Performance, Quality, Safety (PQS) vaccine excursion guidelines.

---

## 2. Human-in-the-Loop (HITL) State Machine

To prevent catastrophic autonomous errors (e.g. prematurely discarding salvageable vaccines or administering heat-damaged stock), Sauti enforces a deterministic safety gate:

```
       [User / Field Sensor Query]
                   │
                   ▼
       [Intent & Risk Classifier]
                   │
       ┌───────────┴───────────┐
       ▼                       ▼
 [Low Risk: Info]      [Critical Action: Discard / Outage]
       │                       │
       │                       ▼
       │             [Execute Tool Suite]
       │             (Qdrant + Arrhenius + PQS)
       │                       │
       │                       ▼
       │             [HITL Review Queue]
       │             (Stores State & SHA-256 Signature)
       │                       │
       │                       ▼
       │             [Clinical Supervisor Audit]
       │             (Nurse / Doctor Sign-off via API)
       │                       │
       ▼                       ▼
 [Direct Output]       [Committed Action & Sync]
```

### Cryptographic Audit Integrity:
Every review record stored in `data/edge_storage.db` incorporates a verifiable audit digest:
$$\text{Digest} = \text{SHA256}(\text{StaffId} \parallel \text{Query} \parallel \text{ProposedAction} \parallel \text{RiskLevel} \parallel \text{ToolEvidence})$$
This ensures records cannot be tampered with while off-grid.
