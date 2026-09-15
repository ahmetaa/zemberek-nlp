#!/usr/bin/env python3
"""
Analyzes and compares morphological disambiguation results between:
  1. Primary trained model (e.g. model-gemini-500.bin)
  2. Optional secondary trained model (e.g. model-gemini-2500.bin)
  3. Default legacy Zemberek baseline
against Gemini's ground truth annotations.

Usage:
  python3 experiment/scripts/evaluate_and_compare.py -r data/ambiguity/evaluation_results.json
"""

import argparse
import json
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any, Dict, List


def analyze_report(report_path: Path, output_md: str = None):
    with open(report_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    total_sentences = data.get("total_sentences", 0)
    total_tokens = data.get("total_tokens", 0)
    ambiguous_tokens = data.get("ambiguous_tokens", 0)
    has_model2 = data.get("has_model2", False)
    m1_name = data.get("model1_name", "model-gemini-500.bin")
    m2_name = data.get("model2_name", "model-gemini-2500.bin")

    # M1 metrics
    m1_amb_acc = data.get("trained_ambiguity_accuracy", 0.0) * 100
    m1_ovr_acc = data.get("trained_overall_accuracy", 0.0) * 100
    m1_sem_acc = data.get("trained_sentence_exact_match_rate", 0.0) * 100
    m1_amb_hits = data.get("trained_ambiguous_matches", 0)
    m1_ovr_hits = data.get("trained_overall_matches", 0)
    m1_sem_hits = data.get("trained_sentence_exact_matches", 0)

    # Baseline metrics
    def_amb_acc = data.get("default_ambiguity_accuracy", 0.0) * 100
    def_ovr_acc = data.get("default_overall_accuracy", 0.0) * 100
    def_sem_acc = data.get("default_sentence_exact_match_rate", 0.0) * 100
    def_amb_hits = data.get("default_ambiguous_matches", 0)
    def_ovr_hits = data.get("default_overall_matches", 0)
    def_sem_hits = data.get("default_sentence_exact_matches", 0)

    # M2 metrics (if present)
    m2_amb_acc = data.get("model2_ambiguity_accuracy", 0.0) * 100 if has_model2 else 0.0
    m2_ovr_acc = data.get("model2_overall_accuracy", 0.0) * 100 if has_model2 else 0.0
    m2_sem_acc = data.get("model2_sentence_exact_match_rate", 0.0) * 100 if has_model2 else 0.0
    m2_amb_hits = data.get("model2_ambiguous_matches", 0) if has_model2 else 0
    m2_ovr_hits = data.get("model2_overall_matches", 0) if has_model2 else 0
    m2_sem_hits = data.get("model2_sentence_exact_matches", 0) if has_model2 else 0

    # POS stats
    pos_stats = defaultdict(lambda: {
        "total": 0,
        "m1_correct": 0,
        "m2_correct": 0,
        "def_correct": 0
    })

    m2_fixes_m1 = []
    disagreements = []
    sentences = data.get("sentences", [])
    for s in sentences:
        sent_text = s.get("text", "")
        for t in s.get("tokens", []):
            if not t.get("is_ambiguous", False):
                continue

            pos = t.get("gemini_pos", "Other")
            pos_stats[pos]["total"] += 1
            if t.get("trained_matches_gemini", False):
                pos_stats[pos]["m1_correct"] += 1
            if has_model2 and t.get("model2_matches_gemini", False):
                pos_stats[pos]["m2_correct"] += 1
            if t.get("default_matches_gemini", False):
                pos_stats[pos]["def_correct"] += 1

            if not t.get("trained_matches_gemini", False):
                disagreements.append({
                    "sentence": sent_text,
                    "surface": t.get("surface", ""),
                    "gemini": t.get("gemini_selected_key", ""),
                    "m1": t.get("trained_selected_key", ""),
                    "default": t.get("default_selected_key", "")
                })

            if has_model2:
                # Did model 2 get it right when model 1 was wrong?
                if t.get("model2_matches_gemini") and not t.get("trained_matches_gemini"):
                    m2_fixes_m1.append({
                        "sentence": sent_text,
                        "surface": t.get("surface", ""),
                        "gemini": t.get("gemini_selected_key", ""),
                        "m1": t.get("trained_selected_key", ""),
                        "m2": t.get("model2_selected_key", ""),
                        "default": t.get("default_selected_key", "")
                    })

    if has_model2:
        print("=" * 100)
        print("                 MORPHOLOGICAL DISAMBIGUATION EVALUATION REPORT (3-WAY)")
        print("=" * 100)
        print(f"Test Corpus:             {total_sentences} sentences ({total_tokens} tokens)")
        print(f"Ambiguous Tokens Tested: {ambiguous_tokens} tokens ({ambiguous_tokens/max(1, total_tokens)*100:.1f}% ambiguity rate)")
        print("-" * 100)
        print(f"{'Metric':<30} | {m1_name:<20} | {m2_name:<20} | {'Default Baseline':<18}")
        print("-" * 100)
        print(f"{'Ambiguity Accuracy (Hard)':<30} | {m1_amb_acc:6.2f}% ({m1_amb_hits:>3}/{ambiguous_tokens}) | {m2_amb_acc:6.2f}% ({m2_amb_hits:>3}/{ambiguous_tokens}) | {def_amb_acc:6.2f}% ({def_amb_hits:>3}/{ambiguous_tokens})")
        print(f"{'Overall Token Accuracy':<30} | {m1_ovr_acc:6.2f}% ({m1_ovr_hits:>3}/{total_tokens}) | {m2_ovr_acc:6.2f}% ({m2_ovr_hits:>3}/{total_tokens}) | {def_ovr_acc:6.2f}% ({def_ovr_hits:>3}/{total_tokens})")
        print(f"{'Sentence Exact Match Rate':<30} | {m1_sem_acc:6.2f}% ({m1_sem_hits:>3}/{total_sentences}) | {m2_sem_acc:6.2f}% ({m2_sem_hits:>3}/{total_sentences}) | {def_sem_acc:6.2f}% ({def_sem_hits:>3}/{total_sentences})")
        print("=" * 100)

        # POS Breakdown Table
        print("\n" + "-" * 100)
        print(f"{'POS Category':<20} | {'Count':<6} | {m1_name:<20} | {m2_name:<20} | {'Default Baseline':<18}")
        print("-" * 100)
        sorted_pos = sorted(pos_stats.items(), key=lambda kv: kv[1]["total"], reverse=True)
        for pos, st in sorted_pos:
            cnt = st["total"]
            if cnt == 0:
                continue
            m1_acc = (st["m1_correct"] / cnt) * 100
            m2_acc = (st["m2_correct"] / cnt) * 100
            df_acc = (st["def_correct"] / cnt) * 100
            print(f"{pos:<20} | {cnt:<6} | {m1_acc:6.2f}% ({st['m1_correct']:>3}/{cnt}) | {m2_acc:6.2f}% ({st['m2_correct']:>3}/{cnt}) | {df_acc:6.2f}% ({st['def_correct']:>3}/{cnt})")
        print("-" * 100)

        if m2_fixes_m1:
            print(f"\nTop Sample Fixes where {m2_name} won over {m1_name} (Total Fixes: {len(m2_fixes_m1)}):")
            print("-" * 100)
            for idx, f_item in enumerate(m2_fixes_m1[:6], 1):
                print(f"[{idx}] Word: \"{f_item['surface']}\" in sentence: \"{f_item['sentence']}\"")
                print(f"    - Gemini Ground Truth: {f_item['gemini']}")
                print(f"    - {m2_name} (Fixed):   {f_item['m2']}")
                print(f"    - {m1_name} (Failed):  {f_item['m1']}")
                print(f"    - Default Baseline:    {f_item['default']}")
                print("")
    else:
        print("=" * 80)
        print("        MORPHOLOGICAL DISAMBIGUATION EVALUATION REPORT")
        print("=" * 80)
        print(f"Test Corpus:             {total_sentences} sentences ({total_tokens} tokens)")
        print(f"Ambiguous Tokens Tested: {ambiguous_tokens} tokens ({ambiguous_tokens/max(1, total_tokens)*100:.1f}% ambiguity rate)")
        print("-" * 80)
        print(f"{'Metric':<34} | {m1_name:<18} | {'Default Baseline':<16} | {'Delta':<8}")
        print("-" * 80)
        delta_amb = m1_amb_acc - def_amb_acc
        sign_amb = "+" if delta_amb >= 0 else ""
        print(f"{'Ambiguity Accuracy (Hard)':<34} | {m1_amb_acc:6.2f}% ({m1_amb_hits:>3}/{ambiguous_tokens}) | {def_amb_acc:6.2f}% ({def_amb_hits:>3}/{ambiguous_tokens}) | {sign_amb}{delta_amb:5.2f}%")

        delta_ovr = m1_ovr_acc - def_ovr_acc
        sign_ovr = "+" if delta_ovr >= 0 else ""
        print(f"{'Overall Token Accuracy':<34} | {m1_ovr_acc:6.2f}% ({m1_ovr_hits:>3}/{total_tokens}) | {def_ovr_acc:6.2f}% ({def_ovr_hits:>3}/{total_tokens}) | {sign_ovr}{delta_ovr:5.2f}%")

        delta_sem = m1_sem_acc - def_sem_acc
        sign_sem = "+" if delta_sem >= 0 else ""
        print(f"{'Sentence Exact Match Rate':<34} | {m1_sem_acc:6.2f}% ({m1_sem_hits:>3}/{total_sentences}) | {def_sem_acc:6.2f}% ({def_sem_hits:>3}/{total_sentences}) | {sign_sem}{delta_sem:5.2f}%")
        print("=" * 80)


def main():
    parser = argparse.ArgumentParser(description="Evaluate and compare disambiguation results")
    parser.add_argument("-r", "--report", required=True, help="Path to evaluation_results.json")
    parser.add_argument("-o", "--output-md", help="Optional markdown output path")
    args = parser.parse_args()

    analyze_report(Path(args.report), args.output_md)


if __name__ == "__main__":
    main()
