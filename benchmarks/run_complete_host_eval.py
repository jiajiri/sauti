import json
import subprocess
import time
import os

BENCH_BIN = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/benchmarks/sauti_bench"
QWEN_MODEL = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/models_store/qwen2.5-0.5b-chat-q4_0.gguf"
SMOLLM_MODEL = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/models_store/smollm2-135m-sw-q4.gguf"
SCENARIOS_FILE = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/benchmarks/scenarios.json"
OUTPUT_FILE = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/benchmarks/sauti_native_host_eval.json"

with open(SCENARIOS_FILE, "r") as f:
    scenarios = json.load(f)

models = {
    "qwen2.5-0.5b": QWEN_MODEL,
    "smollm2-135m": SMOLLM_MODEL
}

all_results = {}

for model_name, model_path in models.items():
    print(f"=== Evaluating {model_name} on Host ===")
    all_results[model_name] = []
    
    for sc in scenarios:
        sc_id = sc["id"]
        lang = sc["language"]
        domain = sc["domain"]
        user_prompt = sc["prompt"]
        
        system_msg = (
            "You are Sauti AI, an intelligent coding and technical assistant. Understand user instructions in both Swahili and English, and provide direct, helpful, and accurate solutions."
        )
        
        prompt = (
            f"<|im_start|>system\n{system_msg}<|im_end|>\n"
            f"<|im_start|>user\n{user_prompt}<|im_end|>\n"
            f"<|im_start|>assistant\n"
        )
        
        t0 = time.time()
        res = subprocess.run([BENCH_BIN, model_path, prompt], capture_output=True, text=True)
        dur = round(time.time() - t0, 2)
        
        output = res.stdout.strip()
        print(f"[{sc_id}] ({domain}-{lang}) [{dur}s]:")
        print(output[:140].replace('\n', ' ') + "...\n")
        
        all_results[model_name].append({
            "scenario_id": sc_id,
            "domain": domain,
            "language": lang,
            "prompt": user_prompt,
            "response": output,
            "duration_sec": dur
        })

with open(OUTPUT_FILE, "w") as f:
    json.dump(all_results, f, indent=2)

print(f"Evaluation complete. Saved to: {OUTPUT_FILE}")
