import subprocess
import json

prompt = """<|im_start|>system
You are Sauti AI, an intelligent coding and technical assistant. Understand Swahili user requests and provide working code solutions.
<|im_end|>
<|im_start|>user
Andika function ya Python ya kuhesabu jumla ya nambari katika orodha (list) na kurudisha wastani.
<|im_end|>
<|im_start|>assistant
Hapa kuna function ya Python:
```python
"""

res = subprocess.run([
    '/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/benchmarks/sauti_bench',
    '/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/models_store/qwen2.5-0.5b-chat-q4_0.gguf',
    prompt
], capture_output=True, text=True)

print("PREFILL RESULT:")
print(res.stdout)
