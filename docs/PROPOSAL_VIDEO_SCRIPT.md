# Mozilla Foundation Human-Scale AI Incubator — 3-Minute Video Proposal Script

**Project**: TALIS (Sovereign Local-First AI & Agentic RAG Stack for East Africa)  
**Target Duration**: 3 Minutes (180 Seconds)  
**Presenter**: LLMOps & Infrastructure Engineer  
**Setting**: Development workstation with terminal, edge single-board computer (Raspberry Pi), and real temperature sensor probe.

---

## Timeline & Scene-by-Scene Storyboard

### [0:00 - 0:30] Scene 1: The Problem & The Human Stakes
* **Visual**: Camera starts on the presenter holding a vaccine vial (or model) next to a small temperature probe. Background shows an East African district map highlighting off-grid clinic zones.
* **Speaker**:
  > "Every single day in sub-Saharan Africa, more than 50% of temperature-sensitive vaccines spoil before they can ever reach a patient. 
  > When a solar battery inverter trips in a rural clinic during a heatwave, or when a faulty thermostat overnight freezes hepatitis B and tetanus vials, nurses have no real-time warning. 
  > Current mainstream AI solutions all assume uninterrupted high-speed internet and expensive cloud APIs. But the moment the rural cellular tower loses power, those cloud systems die completely. 
  > We built **Sauti** to change that."

---

### [0:30 - 1:15] Scene 2: What is Sauti? The Local-First Architecture
* **Visual**: Screen switches to the terminal running `sauti serve` and a diagram showing the Dual-Tier Model Hierarchy and local Qdrant vector database.
* **Speaker**:
  > "**Sauti** is not a chatbot wrapper. It is an extractive-free, high-sovereignty LLMOps platform engineered specifically for African infrastructural constraints.
  > Instead of sending sensitive medical data over international borders to Western cloud monopolies, Sauti runs 100% locally on low-power edge hardware.
  > We implemented an adaptive Dual-Tier architecture:
  > - **Tier 1**: A 1.5-billion parameter model running at just 1.1 gigabytes of RAM on a thirty-five dollar Raspberry Pi inside rural clinics.
  > - **Tier 2**: A fine-tuned 7-billion parameter model running on domestic regional hospital servers.
  > Both operate with sub-45-millisecond latency on CPU, with zero internet required."

---

### [1:15 - 2:05] Scene 3: Live Engineering Demonstration (Agentic RAG, Whisper & HITL)
* **Visual**: Live screen capture. Presenter plugs in a microphone and speaks into the terminal.
* **Action**:
  1. Presenter dictates voice alert: *"Solar inverter failed; fridge temperature rose to nineteen degrees for five hours with oral polio vaccine."*
  2. Terminal shows local **Whisper** engine transcribe the voice note in under two seconds.
  3. The **Agentic RAG Orchestrator** executes Arrhenius biochemical decay physics and WHO PQS rules.
  4. The screen highlights: `STATUS: REQUIRES_HUMAN_APPROVAL` (Review ID #14).
* **Speaker**:
  > "Watch this live. Our integrated offline Whisper engine transcribes local clinician voice notes directly on device. 
  > Then, our Agentic RAG core evaluates the physical decay kinetics. 
  > But critically: we enforce a Human-in-the-Loop safety gate. Sauti never takes unsupervised clinical actions. When an irreversible step like vaccine disposal is proposed, it is placed in an auditable review queue until a clinical supervisor digitally authorizes it."

---

### [2:05 - 2:40] Scene 4: Real Data Provenance & Open Commons
* **Visual**: Display showing the `DATASET_PROVENANCE.json` file and verified source links.
* **Speaker**:
  > "Our training data is authentic, verified, and non-synthetic. 
  > We curated over 1.7 million real records from the open-source community:
  > - 1.69 million Swahili health and public texts from Mendeley and Hugging Face.
  > - 15,000 real pan-African clinical QA pairs from AfriMed-QA across 16 countries.
  > - Real facility cold-chain monitoring logs and official WHO PQS immunization catalogue equipment specifications.
  > Every dataset is open-access and governed under transparent, non-extractive licenses."

---

### [2:40 - 3:00] Scene 5: Why Mozilla & Closing Call to Action
* **Visual**: Presenter back on camera, holding the Raspberry Pi appliance with the local dashboard open on an offline tablet screen.
* **Speaker**:
  > "With the Mozilla Foundation's Human-Scale AI Incubator, we will deploy fifty turnkey Sauti edge nodes across fifteen rural health clinics in East Africa, giving frontline workers the sovereignty, safety, and intelligence they deserve.
  > We are not building AI for scale at all costs. We are building AI at human scale. 
  > Thank you."
