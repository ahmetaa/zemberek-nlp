#!/usr/bin/env python3
"""Annotate heldout 100 sentences with Gemini 3.8 Flash for testing."""
import os, sys, json, time, requests, concurrent.futures

def load_env(filepath=".env"):
    if os.path.exists(filepath):
        with open(filepath, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    os.environ[k.strip()] = v.strip().strip("'").strip('"')

load_env()
load_env(".env.local")

api_key = os.environ.get("GEMINI_API_KEY")
if not api_key:
    print("Error: GEMINI_API_KEY is not set.")
    sys.exit(1)

input_path = sys.argv[1] if len(sys.argv) > 1 else "/home/dndara/data/turkish/distilled/test_heldout_100_candidates.jsonl"
output_path = sys.argv[2] if len(sys.argv) > 2 else "/home/dndara/data/turkish/distilled/test_heldout_100_annotated.jsonl"

system_inst = "You are an expert Turkish computational linguist specialized in morphological disambiguation. Return only valid JSON mapping token index to chosen candidate ID. Example: {\"0\": 3, \"1\": 0}"

def format_prompt(rec):
    lines = [f'Sentence: "{rec["text"]}"', "Select correct candidate ID for each ambiguous word:"]
    for t in rec["tokens"]:
        if t.get("is_ambiguous"):
            cands = t.get("candidates", [])
            top_score = max(c.get("score", 0.0) for c in cands) if cands else 0
            pruned = [c for c in cands if c.get("score", 0.0) >= top_score - 15.0]
            if not pruned: pruned = cands
            cands_str = "  ".join(f"[{c['id']}] {c['oflazer_style']}" for c in pruned)
            lines.append(f"- #{t['index']} \"{t['surface']}\": {cands_str}")
    lines.append('Return JSON: {"<index>": <chosen_id>}')
    return "\n".join(lines)

def process_one(line):
    rec = json.loads(line)
    prompt = format_prompt(rec)
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key={api_key}"
    payload = {
        "system_instruction": {"parts": [{"text": system_inst}]},
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"temperature": 0.0, "responseMimeType": "application/json", "thinkingConfig": {"thinkingBudget": 0}}
    }
    for attempt in range(4):
        try:
            resp = requests.post(url, json=payload, timeout=30)
            if resp.status_code == 200:
                text = resp.json()["candidates"][0]["content"]["parts"][0]["text"].strip()
                if text.startswith("```json"): text = text[7:]
                if text.startswith("```"): text = text[3:]
                if text.endswith("```"): text = text[:-3]
                sel_map = json.loads(text.strip())
                for t in rec["tokens"]:
                    if t.get("is_ambiguous"):
                        idx_str = str(t["index"])
                        valid_ids = [c["id"] for c in t.get("candidates", [])]
                        chosen = sel_map.get(idx_str, sel_map.get(int(idx_str))) if isinstance(sel_map, dict) else None
                        if chosen is not None and int(chosen) in valid_ids:
                            t["selected_candidate_id"] = int(chosen)
                            t["selection_reasoning"] = "Heldout Gold Annotation"
                        else:
                            t["selected_candidate_id"] = valid_ids[0] if valid_ids else 0
                return rec
            time.sleep(1.5 ** attempt)
        except Exception:
            time.sleep(1.5 ** attempt)
    return rec

with open(input_path, "r", encoding="utf-8") as f:
    lines = [l for l in f if l.strip()]

print(f"Annotating {len(lines)} heldout sentences with Gemini 3.8 Flash...")
start = time.time()
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
    results = list(executor.map(process_one, lines))

with open(output_path, "w", encoding="utf-8") as out_f:
    for r in results:
        out_f.write(json.dumps(r, ensure_ascii=False) + "\n")

print(f"Completed heldout annotation in {time.time()-start:.1f}s. Saved to {output_path}")

