#!/usr/bin/env python3
"""
Cost and Performance Optimized LLM Morphological Disambiguator for Large Batches.

Features:
1. Inlined, ultra-compact prompt representation (-65% prompt tokens).
2. Noise candidate pruning: filters out implausible parses (score < top - 15.0).
3. Strictly zero thinking budget (thinkingBudget: 0) to avoid hidden reasoning costs.
4. JSON mode with direct schema mapping: {"<token_index>": <chosen_id>}.
5. Resume capability from existing output file.
6. Real-time cost monitoring, circuit breakers, and comprehensive report generation.
"""

import os
import sys
import json
import time
import argparse
import threading
import concurrent.futures
from datetime import datetime, timezone
from typing import List, Dict, Any, Optional, Tuple

try:
    import requests
    HAS_REQUESTS = True
except ImportError:
    HAS_REQUESTS = False


def load_env(filepath: str = ".env") -> None:
    """Load environment variables from a .env file if present."""
    if os.path.exists(filepath):
        with open(filepath, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, val = line.split("=", 1)
                    val = val.strip().strip("'").strip('"')
                    os.environ[key.strip()] = val


SYSTEM_INSTRUCTION = (
    "You are an expert Turkish computational linguist specialized in morphological disambiguation. "
    "Select the single most accurate morphological analysis ID for each ambiguous word in the given sentence context. "
    "Return only valid JSON mapping token index string to candidate ID integer. Example: {\"0\": 3, \"1\": 0}"
)


class CostMonitor:
    """Thread-safe token usage tracker, cost estimator, and circuit-breaker."""

    def __init__(
        self,
        price_per_1m_input: float = 0.75,
        price_per_1m_output: float = 3.75,
        max_budget_usd: float = 12.0
    ):
        self.lock = threading.Lock()
        self.total_prompt_tokens = 0
        self.total_candidates_tokens = 0
        self.total_thoughts_tokens = 0
        self.total_calls = 0
        self.price_per_1m_input = price_per_1m_input
        self.price_per_1m_output = price_per_1m_output
        self.max_budget_usd = max_budget_usd
        self.circuit_broken = False
        self.circuit_break_reason = ""
        self.start_time = time.time()

    def record(self, usage: Dict[str, Any], sentence_id: str = "") -> None:
        if not usage:
            return
        prompt_t = usage.get("promptTokenCount", 0)
        cand_t = usage.get("candidatesTokenCount", 0)
        thought_t = usage.get("thoughtsTokenCount", 0)

        with self.lock:
            self.total_calls += 1
            self.total_prompt_tokens += prompt_t
            self.total_candidates_tokens += cand_t
            self.total_thoughts_tokens += thought_t

            cost = self.estimated_cost_usd()
            if cost > self.max_budget_usd:
                self.circuit_broken = True
                self.circuit_break_reason = (
                    f"Estimated cost (${cost:.2f}) exceeded budget limit (${self.max_budget_usd:.2f})."
                )

    def estimated_cost_usd(self) -> float:
        input_cost = (self.total_prompt_tokens / 1_000_000.0) * self.price_per_1m_input
        output_cost = ((self.total_candidates_tokens + self.total_thoughts_tokens) / 1_000_000.0) * self.price_per_1m_output
        return input_cost + output_cost

    def batch_api_cost_usd(self) -> float:
        """Estimated cost if run via Google Gemini Batch API (50% discount)."""
        return self.estimated_cost_usd() * 0.50

    def summary_str(self) -> str:
        with self.lock:
            elapsed = max(0.1, time.time() - self.start_time)
            rate = self.total_calls / elapsed
            out_total = self.total_candidates_tokens + self.total_thoughts_tokens
            return (
                f"[TOKENS] Calls: {self.total_calls:,} ({rate:.1f} sent/s) | "
                f"In: {self.total_prompt_tokens:,} | Out: {self.total_candidates_tokens:,} | "
                f"Thoughts: {self.total_thoughts_tokens:,} (Total Out: {out_total:,}) | "
                f"Cost: ${self.estimated_cost_usd():.4f} (Batch: ${self.batch_api_cost_usd():.4f})"
            )


def format_compact_prompt(record: Dict[str, Any], prune_noise: bool = True) -> str:
    """Builds an ultra-compact prompt with optional score-margin candidate pruning."""
    sentence = record["text"]
    ambiguous_tokens = [t for t in record["tokens"] if t.get("is_ambiguous", False)]

    lines = [
        f'Sentence: "{sentence}"',
        "Select correct candidate ID for each ambiguous word:"
    ]

    for t in ambiguous_tokens:
        cands = t.get("candidates", [])
        if prune_noise and cands:
            top_score = max(c.get("score", 0.0) for c in cands)
            pruned = [c for c in cands if c.get("score", 0.0) >= top_score - 15.0]
            if pruned:
                cands = pruned

        cands_str = "  ".join(f"[{c['id']}] {c['oflazer_style']}" for c in cands)
        lines.append(f"- #{t['index']} \"{t['surface']}\": {cands_str}")

    lines.append('Return JSON: {"<index>": <chosen_id>}')
    return "\n".join(lines)


def call_gemini_rest(
    api_key: str,
    prompt: str,
    model: str = "gemini-3.8-flash",
    temperature: float = 0.0,
    thinking_budget: int = 0,
    max_retries: int = 5
) -> Tuple[Optional[str], Dict[str, Any]]:
    """Calls Gemini REST API directly with explicit zero-thinking budget and JSON mode."""
    if not HAS_REQUESTS:
        raise RuntimeError("The requests library is required.")

    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    generation_config: Dict[str, Any] = {
        "temperature": temperature,
        "responseMimeType": "application/json",
        "thinkingConfig": {"thinkingBudget": thinking_budget}
    }

    payload = {
        "system_instruction": {"parts": [{"text": SYSTEM_INSTRUCTION}]},
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": generation_config
    }

    for attempt in range(max_retries):
        try:
            resp = requests.post(url, json=payload, timeout=40)
            if resp.status_code == 200:
                data = resp.json()
                usage = data.get("usageMetadata", {})
                candidates = data.get("candidates", [])
                if candidates:
                    parts = candidates[0].get("content", {}).get("parts", [])
                    if parts:
                        return parts[0].get("text", "").strip(), usage
            elif resp.status_code == 429:
                wait = (2 ** attempt) * 2.5
                time.sleep(wait)
            else:
                wait = 2 ** attempt
                time.sleep(wait)
        except Exception:
            wait = 2 ** attempt
            time.sleep(wait)
    return None, {}


def parse_llm_json(response_text: str) -> Optional[Dict[str, Any]]:
    """Extracts and parses JSON safely."""
    if not response_text:
        return None
    cleaned = response_text.strip()
    if cleaned.startswith("```json"):
        cleaned = cleaned[7:]
    elif cleaned.startswith("```"):
        cleaned = cleaned[3:]
    if cleaned.endswith("```"):
        cleaned = cleaned[:-3]
    cleaned = cleaned.strip()

    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        return None


def apply_selections(record: Dict[str, Any], selections_data: Dict[str, Any]) -> int:
    """Applies candidate selections to record tokens."""
    sel_map = {}
    if isinstance(selections_data, dict):
        if "selections" in selections_data and isinstance(selections_data["selections"], list):
            for s in selections_data["selections"]:
                if isinstance(s, dict) and "token_index" in s:
                    sel_map[int(s["token_index"])] = s.get("selected_candidate_id")
        else:
            for k, v in selections_data.items():
                try:
                    if isinstance(v, dict):
                        sel_map[int(k)] = int(v.get("selected_candidate_id", 0))
                    else:
                        sel_map[int(k)] = int(v)
                except (ValueError, TypeError):
                    continue

    applied_count = 0
    for token in record["tokens"]:
        if not token.get("is_ambiguous", False):
            continue

        idx = token["index"]
        valid_ids = [c["id"] for c in token.get("candidates", [])]

        if idx in sel_map:
            chosen_id = sel_map[idx]
            if chosen_id in valid_ids:
                token["selected_candidate_id"] = chosen_id
                token["selection_reasoning"] = "LLM Disambiguation (Optimized)"
                applied_count += 1
            else:
                token["selected_candidate_id"] = valid_ids[0] if valid_ids else 0
                token["selection_reasoning"] = "Fallback: invalid candidate ID"
        else:
            token["selected_candidate_id"] = valid_ids[0] if valid_ids else 0
            token["selection_reasoning"] = "Fallback: missing from LLM response"

    return applied_count


def process_dataset(
    input_path: str,
    output_path: str,
    api_key: str,
    model: str = "gemini-3.8-flash",
    max_sentences: int = 10000,
    concurrency: int = 10,
    max_budget: float = 12.0,
    prune_noise: bool = True
) -> None:
    # Model pricing table
    price_in = 0.25 if "lite" in model.lower() else 0.75
    price_out = 1.50 if "lite" in model.lower() else 3.75

    cost_monitor = CostMonitor(
        price_per_1m_input=price_in,
        price_per_1m_output=price_out,
        max_budget_usd=max_budget
    )

    # Load records to process
    records = []
    with open(input_path, "r", encoding="utf-8") as f:
        for idx, line in enumerate(f):
            line = line.strip()
            if not line:
                continue
            records.append((idx, json.loads(line)))
            if max_sentences > 0 and len(records) >= max_sentences:
                break

    print(f"[INFO] Target sentences: {len(records)} from {input_path}")
    print(f"[INFO] Output file:      {output_path}")
    print(f"[INFO] Model: {model} | Concurrency: {concurrency} | Rates: (${price_in:.2f} in, ${price_out:.2f} out / 1M)")
    print(f"[INFO] Budget Limit: ${max_budget:.2f} | Noise Pruning: {prune_noise}")

    # Check for existing progress (Resume capability)
    existing_done_ids = set()
    if os.path.exists(output_path):
        with open(output_path, "r", encoding="utf-8") as out_f:
            for line in out_f:
                line = line.strip()
                if not line:
                    continue
                try:
                    d = json.loads(line)
                    sid = d.get("sentence_id")
                    if sid:
                        existing_done_ids.add(sid)
                except Exception:
                    pass
        if existing_done_ids:
            print(f"[RESUME] Found {len(existing_done_ids)} previously processed sentences in {output_path}. Resuming...")

    remaining_records = [r for r in records if r[1].get("sentence_id") not in existing_done_ids]
    if not remaining_records:
        print(f"[DONE] All {len(records)} sentences already annotated in {output_path}.")
        return

    print(f"[INFO] Processing {len(remaining_records)} remaining sentences...")

    def process_item(item):
        line_num, record = item
        ambiguous_tokens = [t for t in record["tokens"] if t.get("is_ambiguous", False)]
        if not ambiguous_tokens:
            return line_num, record, 0

        if cost_monitor.circuit_broken:
            return line_num, record, 0

        prompt = format_compact_prompt(record, prune_noise=prune_noise)
        response_text, usage = call_gemini_rest(
            api_key=api_key,
            prompt=prompt,
            model=model,
            temperature=0.0,
            thinking_budget=0
        )

        cost_monitor.record(usage, sentence_id=record.get("sentence_id", str(line_num)))

        applied = 0
        parsed = parse_llm_json(response_text)
        if parsed:
            applied = apply_selections(record, parsed)

        return line_num, record, applied

    mode = "a" if existing_done_ids else "w"
    completed_count = len(existing_done_ids)
    total_ambiguous_tokens = 0
    rank_selections = {1: 0, 2: 0, 3: 0, "4+": 0}
    fallback_count = 0

    log_interval = 250 if len(records) >= 1000 else 25

    with open(output_path, mode, encoding="utf-8") as out_f:
        workers = min(concurrency, max(1, len(remaining_records)))
        with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as executor:
            for line_num, rec, applied in executor.map(process_item, remaining_records):
                if cost_monitor.circuit_broken:
                    print(f"\n[CIRCUIT BREAKER] {cost_monitor.circuit_break_reason}")
                    break

                out_f.write(json.dumps(rec, ensure_ascii=False) + "\n")
                out_f.flush()
                completed_count += 1
                total_ambiguous_tokens += applied

                # Track selection rank statistics
                for t in rec.get("tokens", []):
                    if t.get("is_ambiguous"):
                        sel_id = t.get("selected_candidate_id")
                        if "Fallback" in t.get("selection_reasoning", ""):
                            fallback_count += 1
                        for c in t.get("candidates", []):
                            if c.get("id") == sel_id:
                                rk = c.get("rank", 1)
                                if rk in rank_selections:
                                    rank_selections[rk] += 1
                                else:
                                    rank_selections["4+"] += 1
                                break

                if completed_count % log_interval == 0 or completed_count == len(records):
                    print(f"[PROGRESS] {completed_count:,}/{len(records):,} sentences | {cost_monitor.summary_str()}")

    elapsed_time = time.time() - cost_monitor.start_time
    print(f"\n[COMPLETE] Processed {completed_count:,} sentences in {elapsed_time:.1f}s ({completed_count/max(0.1, elapsed_time):.1f} sent/s).")
    print(f"           Disambiguated Ambiguous Tokens: {total_ambiguous_tokens:,}")
    print(f"           Final Stats: {cost_monitor.summary_str()}")
    print(f"           Saved to:    {output_path}")

    # Write detailed run report
    report_md_path = output_path.replace(".jsonl", "_report.md")
    report_json_path = output_path.replace(".jsonl", "_report.json")

    report_data = {
        "model": model,
        "completed_at": datetime.now(timezone.utc).isoformat(),
        "total_sentences": completed_count,
        "total_ambiguous_tokens": total_ambiguous_tokens,
        "fallback_count": fallback_count,
        "elapsed_seconds": round(elapsed_time, 1),
        "sentences_per_second": round(completed_count / max(0.1, elapsed_time), 2),
        "rank_distribution": rank_selections,
        "token_usage": {
            "prompt_tokens": cost_monitor.total_prompt_tokens,
            "candidates_tokens": cost_monitor.total_candidates_tokens,
            "thoughts_tokens": cost_monitor.total_thoughts_tokens,
            "total_tokens": cost_monitor.total_prompt_tokens + cost_monitor.total_candidates_tokens + cost_monitor.total_thoughts_tokens,
            "avg_prompt_tokens_per_sentence": round(cost_monitor.total_prompt_tokens / max(1, cost_monitor.total_calls), 1),
            "avg_output_tokens_per_sentence": round(cost_monitor.total_candidates_tokens / max(1, cost_monitor.total_calls), 1),
        },
        "cost": {
            "realtime_cost_usd": round(cost_monitor.estimated_cost_usd(), 4),
            "batch_api_cost_usd": round(cost_monitor.batch_api_cost_usd(), 4)
        }
    }

    with open(report_json_path, "w", encoding="utf-8") as jf:
        json.dump(report_data, jf, indent=2, ensure_ascii=False)

    report_md = f"""# LLM Morphological Disambiguation Batch 2 Report

## Run Summary
- **Model**: `{model}`
- **Completed At**: {report_data['completed_at']}
- **Sentences Processed**: {completed_count:,}
- **Ambiguous Tokens Disambiguated**: {total_ambiguous_tokens:,}
- **Fallback Tokens**: {fallback_count:,} ({fallback_count / max(1, total_ambiguous_tokens) * 100:.2f}%)
- **Processing Time**: {elapsed_time:.1f} seconds ({report_data['sentences_per_second']} sentences/sec)
- **Output Dataset**: `{output_path}`

## Cost & Token Accounting
| Metric | Value |
| :--- | :--- |
| **Prompt Input Tokens** | {cost_monitor.total_prompt_tokens:,} (avg {report_data['token_usage']['avg_prompt_tokens_per_sentence']} / sent) |
| **Candidates Output Tokens** | {cost_monitor.total_candidates_tokens:,} (avg {report_data['token_usage']['avg_output_tokens_per_sentence']} / sent) |
| **Thinking Tokens** | {cost_monitor.total_thoughts_tokens:,} |
| **Total Billed Tokens** | {report_data['token_usage']['total_tokens']:,} |
| **Actual Real-Time Cost** | **${cost_monitor.estimated_cost_usd():.4f}** |
| **Equivalent Batch API Cost** | **${cost_monitor.batch_api_cost_usd():.4f}** |

## Selected Candidate Rank Distribution
| Rank | Selections | Percentage |
| :---: | :---: | :---: |
| **Rank 1** (Top Perceptron Parse) | {rank_selections.get(1, 0):,} | {rank_selections.get(1, 0) / max(1, total_ambiguous_tokens) * 100:.1f}% |
| **Rank 2** (Runner-up Parse) | {rank_selections.get(2, 0):,} | {rank_selections.get(2, 0) / max(1, total_ambiguous_tokens) * 100:.1f}% |
| **Rank 3** | {rank_selections.get(3, 0):,} | {rank_selections.get(3, 0) / max(1, total_ambiguous_tokens) * 100:.1f}% |
| **Rank 4+** | {rank_selections.get('4+', 0):,} | {rank_selections.get('4+', 0) / max(1, total_ambiguous_tokens) * 100:.1f}% |
"""

    with open(report_md_path, "w", encoding="utf-8") as mf:
        mf.write(report_md)

    print(f"[REPORT] Saved report to {report_md_path} and {report_json_path}")


def main():
    load_env()
    load_env(".env.local")

    parser = argparse.ArgumentParser(description="Optimized LLM Morphological Disambiguator for Batch Execution")
    parser.add_argument("-i", "--input", required=True, help="Input candidates JSONL file")
    parser.add_argument("-o", "--output", required=True, help="Output annotated JSONL file")
    parser.add_argument("-m", "--model", default="gemini-3.8-flash", help="Model name (default: gemini-3.8-flash)")
    parser.add_argument("--max-sentences", "-max", type=int, default=10000, help="Max sentences to process (default: 10000)")
    parser.add_argument("--concurrency", "-c", type=int, default=10, help="Concurrency level (default: 10)")
    parser.add_argument("--max-budget", type=float, default=12.0, help="Circuit breaker budget limit in USD (default: 12.0)")
    parser.add_argument("--no-prune", action="store_true", help="Disable candidate noise pruning")

    args = parser.parse_args()

    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("[ERROR] GEMINI_API_KEY environment variable is not set.")
        sys.exit(1)

    process_dataset(
        input_path=args.input,
        output_path=args.output,
        api_key=api_key,
        model=args.model,
        max_sentences=args.max_sentences,
        concurrency=args.concurrency,
        max_budget=args.max_budget,
        prune_noise=not args.no_prune
    )


if __name__ == "__main__":
    main()

