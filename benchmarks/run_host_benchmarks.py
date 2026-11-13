import json
import subprocess
import time
import os
import re

MODELS = {
    "qwen2.5-0.5b": "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/models_store/qwen2.5-0.5b-chat-q4_0.gguf",
    "smollm2-135m": "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/models_store/smollm2-135m-sw-q4.gguf"
}

SCENARIOS_FILE = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/benchmarks/scenarios.json"
OUTPUT_FILE = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/benchmarks/host_benchmark_results.json"
LLAMA_CLI = "/tmp/build_llama/bin/llama-cli"

with open(SCENARIOS_FILE, "r") as f:
    scenarios = json.load(f)

results = {}

for model_name, model_path in MODELS.items():
    print(f"\n==========================================")
    print(f"BENCHMARKING MODEL ON HOST: {model_name}")
    print(f"==========================================")
    results[model_name] = []
    
    for sc in scenarios:
        sc_id = sc["id"]
        lang = sc["language"]
        domain = sc["domain"]
        user_prompt = sc["prompt"]
        
        system_msg = (
            "Wewe ni Sauti AI, msaidizi mwerevu wa teknolojia na uandishi wa programu. Mtumiaji anakuuliza kwa Kiswahili. Toa code au majibu sahihi na kamili moja kwa moja."
            if lang == "Swahili"
            else "You are Sauti AI, an intelligent coding and technical assistant. Reply directly with complete code or solution."
        )
        
        formatted_prompt = (
            f"<|im_start|>system\n{system_msg}<|im_end|>\n"
            f"<|im_start|>user\n{user_prompt}<|im_end|>\n"
            f"<|im_start|>assistant\n"
        )
        
        cmd = [
            LLAMA_CLI,
            "-m", model_path,
            "-p", formatted_prompt,
            "-n", "256",
            "-c", "1024",
            "--temp", "0.7",
            "--top-p", "0.90",
            "--top-k", "40",
            "--min-p", "0.05",
            "--repeat-penalty", "1.25",
            "--presence-penalty", "0.30",
            "--frequency-penalty", "0.50",
            "--threads", "4",
            "--single-turn"
        ]
        
        start_t = time.time()
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        dur = time.time() - start_t
        
        raw_output = res.stdout
        
        # Extract response from after "<|im_start|>assistant\n"
        assistant_marker = "<|im_start|>assistant\n"
        if assistant_marker in raw_output:
            response_text = raw_output.split(assistant_marker, 1)[1]
        else:
            response_text = raw_output
            
        # Clean off trailing stats banner and exit prompt
        if "[ Prompt:" in response_text:
            response_text = response_text.split("[ Prompt:")[0].strip()
        if "Exiting..." in response_text:
            response_text = response_text.split("Exiting...")[0].strip()
        
        print(f"[{model_name}] {sc_id} ({domain} - {lang}) took {dur:.2f}s:")
        print(response_text[:160].replace('\n', ' ') + "...\n")
        
        results[model_name].append({
            "scenario_id": sc_id,
            "domain": domain,
            "language": lang,
            "prompt": user_prompt,
            "response": response_text,
            "duration_sec": round(dur, 2)
        })

with open(OUTPUT_FILE, "w") as f:
    json.dump(results, f, indent=2)

print(f"\nHost benchmarks completed successfully. Saved to: {OUTPUT_FILE}")
