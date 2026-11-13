import json
import subprocess
import time
import re

SCENARIOS_FILE = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/benchmarks/scenarios.json"
OUTPUT_FILE = "/home/laphilemon/Documents/engine/workstation/Projects/Mozilla/benchmarks/android_eval_results.json"

with open(SCENARIOS_FILE, "r") as f:
    scenarios = json.load(f)

results = []

for sc in scenarios:
    sc_id = sc["id"]
    domain = sc["domain"]
    lang = sc["language"]
    prompt = sc["prompt"]
    
    print(f"\n---> Testing on Android: [{sc_id}] {domain} ({lang})")
    print(f"Prompt: {prompt}")
    
    # 1. Tap input box (bounds approx [400, 1766])
    subprocess.run(["adb", "shell", "input", "tap", "400", "1766"])
    time.sleep(0.5)
    
    # 2. Format text for adb input (escape spaces)
    escaped_prompt = prompt.replace(" ", "%s").replace("'", "\\'").replace('"', '\\"').replace('(', '\\(').replace(')', '\\)').replace('+', '%s+%s').replace('=', '%s=%s').replace('^', '')
    subprocess.run(["adb", "shell", "input", "text", escaped_prompt])
    time.sleep(0.5)
    
    # 3. Dismiss keyboard
    subprocess.run(["adb", "shell", "input", "keyevent", "111"])
    time.sleep(0.5)
    
    # 4. Tap Send button (approx [967, 1740])
    subprocess.run(["adb", "shell", "input", "tap", "967", "1740"])
    
    # 5. Wait for token generation
    time.sleep(12)
    
    # 6. Dump UI to grab response
    subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/win_eval.xml"])
    subprocess.run(["adb", "pull", "/sdcard/win_eval.xml", "/tmp/win_eval.xml"], stdout=subprocess.DEVNULL)
    
    import xml.etree.ElementTree as ET
    tree = ET.parse('/tmp/win_eval.xml')
    messages = []
    for node in tree.iter('node'):
        txt = node.attrib.get('text', '')
        if txt and txt not in ['Sauti', 'Offline Sauti Agent', 'Andika swali au omba code hapa...', 'Copy']:
            messages.append(txt)
            
    # The last or second to last is the assistant response
    resp = messages[-1] if messages else ""
    print(f"Extracted Android response:\n{resp[:160]}...\n")
    
    results.append({
        "scenario_id": sc_id,
        "domain": domain,
        "language": lang,
        "prompt": prompt,
        "android_response": resp
    })

with open(OUTPUT_FILE, "w") as f:
    json.dump(results, f, indent=2)

print(f"Android evaluation complete. Saved to: {OUTPUT_FILE}")
