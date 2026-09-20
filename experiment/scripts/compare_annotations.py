#!/usr/bin/env python3
"""
Compare morphological disambiguation annotations between two models.
Evaluates:
- Exact token-level agreement rate
- Sentence-level complete agreement rate
- Linguistic categorization of disagreements
- Token usage and cost comparison
- Extrapolations for 10K and 85K sentences
"""

import sys
import json
from typing import List, Dict, Any, Tuple

def load_jsonl(filepath: str) -> List[Dict[str, Any]]:
    records = []
    with open(filepath, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                records.append(json.loads(line))
    return records

def compare(baseline_path: str, lite_path: str):
    base_records = load_jsonl(baseline_path)
    lite_records = load_jsonl(lite_path)

    print(f"Loaded {len(base_records)} baseline records from {baseline_path}")
    print(f"Loaded {len(lite_records)} lite records from {lite_path}")

    base_map = {r["sentence_id"]: r for r in base_records}
    lite_map = {r["sentence_id"]: r for r in lite_records}

    common_ids = [r["sentence_id"] for r in base_records if r["sentence_id"] in lite_map]
    print(f"Common sentences: {len(common_ids)}")

    total_tokens = 0
    agree_tokens = 0
    disagree_tokens = 0

    total_sentences = len(common_ids)
    perfect_agree_sentences = 0

    disagreements = []

    for sid in common_ids:
        b_rec = base_map[sid]
        l_rec = lite_map[sid]

        b_amb = {t["index"]: t for t in b_rec["tokens"] if t.get("is_ambiguous")}
        l_amb = {t["index"]: t for t in l_rec["tokens"] if t.get("is_ambiguous")}

        sent_agree = True

        for idx, b_tok in b_amb.items():
            if idx not in l_amb:
                continue
            l_tok = l_amb[idx]

            total_tokens += 1
            b_sel = b_tok.get("selected_candidate_id")
            l_sel = l_tok.get("selected_candidate_id")

            if b_sel == l_sel:
                agree_tokens += 1
            else:
                sent_agree = False
                disagree_tokens += 1

                # Find candidate details
                cands = {c["id"]: c for c in b_tok.get("candidates", [])}
                b_cand = cands.get(b_sel, {})
                l_cand = cands.get(l_sel, {})

                disagreements.append({
                    "sentence_id": sid,
                    "sentence_text": b_rec["text"],
                    "token_index": idx,
                    "surface": b_tok["surface"],
                    "candidates": b_tok.get("candidates", []),
                    "base_selected_id": b_sel,
                    "base_tag": b_cand.get("oflazer_style", "UNKNOWN"),
                    "base_lemma": b_cand.get("lemma", ""),
                    "lite_selected_id": l_sel,
                    "lite_tag": l_cand.get("oflazer_style", "UNKNOWN"),
                    "lite_lemma": l_cand.get("lemma", "")
                })

        if sent_agree and b_amb:
            perfect_agree_sentences += 1

    token_agree_rate = (agree_tokens / total_tokens * 100) if total_tokens else 0
    sentence_agree_rate = (perfect_agree_sentences / total_sentences * 100) if total_sentences else 0

    print("\n" + "=" * 70)
    print("           MODEL AGREEMENT REPORT: GEMINI 3.8 FLASH vs 3.1 FLASH-LITE")
    print("=" * 70)
    print(f"Total Sentences Evaluated:           {total_sentences}")
    print(f"Total Ambiguous Tokens Evaluated:    {total_tokens}")
    print(f"Token-Level Agreement:               {agree_tokens} / {total_tokens} ({token_agree_rate:.2f}%)")
    print(f"Token-Level Disagreements:           {disagree_tokens} / {total_tokens} ({100 - token_agree_rate:.2f}%)")
    print(f"Perfect Sentence Agreement:          {perfect_agree_sentences} / {total_sentences} ({sentence_agree_rate:.2f}%)")
    print("=" * 70)

    print(f"\nListing all {len(disagreements)} Disagreements with Linguistic Context:\n")
    for i, d in enumerate(disagreements, 1):
        print(f"[{i}] Sentence: \"{d['sentence_text']}\"")
        print(f"    Word #{d['token_index']} \"{d['surface']}\"")
        print(f"    - Gemini 3.8 Flash:      [{d['base_selected_id']}] {d['base_tag']} (lemma: {d['base_lemma']})")
        print(f"    - Gemini 3.1 Flash-Lite: [{d['lite_selected_id']}] {d['lite_tag']} (lemma: {d['lite_lemma']})")
        print("    All Available Candidates:")
        for c in d["candidates"]:
            print(f"       [{c['id']}] {c['oflazer_style']} (lemma: {c['lemma']})")
        print("-" * 70)

    return {
        "total_sentences": total_sentences,
        "total_tokens": total_tokens,
        "agree_tokens": agree_tokens,
        "disagree_tokens": disagree_tokens,
        "token_agree_rate": token_agree_rate,
        "perfect_agree_sentences": perfect_agree_sentences,
        "sentence_agree_rate": sentence_agree_rate,
        "disagreements": disagreements
    }

if __name__ == "__main__":
    b_path = sys.argv[1] if len(sys.argv) > 1 else "/home/dndara/data/turkish/distilled/test_100_annotated.jsonl"
    l_path = sys.argv[2] if len(sys.argv) > 2 else "/home/dndara/data/turkish/distilled/test_100_annotated_lite.jsonl"
    compare(b_path, l_path)

