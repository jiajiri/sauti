import json
import subprocess
import time

BENCH_BIN = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/benchmarks/sauti_bench"
QWEN_MODEL = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/models_store/qwen2.5-0.5b-chat-q4_0.gguf"
SMOLLM_MODEL = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/models_store/smollm2-135m-sw-q4.gguf"
SCENARIOS_FILE = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/benchmarks/scenarios.json"

with open(SCENARIOS_FILE, "r") as f:
    scenarios = json.load(f)

print("PARITY EVALUATION READY")
