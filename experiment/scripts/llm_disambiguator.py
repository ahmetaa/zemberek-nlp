#!/usr/bin/env python3
"""
Morphological Ambiguity Disambiguator using Google Gemini LLM.

Reads a JSONL file produced by DisambiguationCandidateExtractor, prompts Gemini
to select the correct morphological analysis for each ambiguous word in context,
and writes out an annotated JSONL file with selected candidate IDs.
"""

import os
import sys
import json
import time
import argparse
import threading
from datetime import datetime, timezone
from typing import List, Dict, Any, Optional, Tuple

# Try to import google.genai, fallback to requests REST API
try:
    from google import genai
    from google.genai import types
    HAS_GOOGLE_GENAI = True
except ImportError:
    HAS_GOOGLE_GENAI = False

try:
    import requests
    HAS_REQUESTS = True
except ImportError:
    HAS_REQUESTS = False


def load_env(filepath: str = ".env") -> None:
    """Manually parse and load environment variables from a .env file if present."""
    if os.path.exists(filepath):
        with open(filepath, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, val = line.split("=", 1)
                    val = val.strip().strip("'").strip('"')
                    os.environ[key.strip()] = val
        print(f"[INFO] Loaded environment variables from {filepath}")


SYSTEM_INSTRUCTION_COMPACT = """You are an expert Turkish computational linguist.
Your task is morphological disambiguation of Turkish sentences.
Given a Turkish sentence and one or more ambiguous words with candidate morphological analyses from Zemberek,
select the single most accurate morphological analysis for each target word in this context.

Return a valid JSON object matching this exact schema:
{
  "selections": [
    {
      "token_index": <int>,
      "selected_candidate_id": <int>
    }
  ]
}
Do not output markdown code fences (```json ... ```), only the raw JSON object.
"""

SYSTEM_INSTRUCTION_VERBOSE = """You are an expert Turkish computational linguist.
Your task is morphological disambiguation of Turkish sentences.
Given a Turkish sentence and one or more ambiguous words with candidate morphological analyses from Zemberek,
select the single most accurate morphological analysis for each target word in this context.

Return a valid JSON object matching this exact schema:
{
  "selections": [
    {
      "token_index": <int>,
      "surface": "<word>",
      "selected_candidate_id": <int>,
      "reasoning": "<short explanation>"
    }
  ]
}
Do not output markdown code fences (```json ... ```), only the raw JSON object.
"""

# Default to compact schema to prevent generating thousands of unnecessary reasoning tokens
SYSTEM_INSTRUCTION = SYSTEM_INSTRUCTION_COMPACT


class TokenMonitor:
    """Thread-safe token usage tracker, cost estimator, and circuit-breaker."""

    def __init__(
        self,
        max_output_tokens_per_call: int = 400,
        max_total_output_tokens: int = 500_000,
        max_budget_usd: float = 5.0,
        price_per_1m_input: float = 0.15,
        price_per_1m_output: float = 0.60
    ):
        self.lock = threading.Lock()
        self.total_prompt_tokens = 0
        self.total_candidates_tokens = 0
        self.total_thoughts_tokens = 0
        self.total_calls = 0
        self.max_output_tokens_per_call = max_output_tokens_per_call
        self.max_total_output_tokens = max_total_output_tokens
        self.max_budget_usd = max_budget_usd
        self.price_per_1m_input = price_per_1m_input
        self.price_per_1m_output = price_per_1m_output
        self.circuit_broken = False
        self.circuit_break_reason = ""

    def record(self, usage: Dict[str, Any], sentence_id: str = "") -> None:
        if not usage:
            return
        prompt_t = usage.get("promptTokenCount", 0)
        cand_t = usage.get("candidatesTokenCount", 0)
        thought_t = usage.get("thoughtsTokenCount", 0)
        total_billed_output = cand_t + thought_t

        with self.lock:
            self.total_calls += 1
            self.total_prompt_tokens += prompt_t
            self.total_candidates_tokens += cand_t
            self.total_thoughts_tokens += thought_t

            # Check single call spike
            if total_billed_output > self.max_output_tokens_per_call:
                print(
                    f"\n[ALERT] High output token spike on sentence {sentence_id}: {total_billed_output} tokens "
                    f"({cand_t} output + {thought_t} thoughts, threshold: {self.max_output_tokens_per_call})"
                )

            # Cumulative circuit-breaker checks
            cumulative_output = self.total_candidates_tokens + self.total_thoughts_tokens
            cost = self.estimated_cost_usd()

            if cumulative_output > self.max_total_output_tokens:
                self.circuit_broken = True
                self.circuit_break_reason = (
                    f"Cumulative output tokens ({cumulative_output:,}) exceeded limit of {self.max_total_output_tokens:,}."
                )

            if cost > self.max_budget_usd:
                self.circuit_broken = True
                self.circuit_break_reason = (
                    f"Estimated cost (${cost:.2f}) exceeded budget of ${self.max_budget_usd:.2f}."
                )

    def estimated_cost_usd(self) -> float:
        input_cost = (self.total_prompt_tokens / 1_000_000.0) * self.price_per_1m_input
        output_cost = ((self.total_candidates_tokens + self.total_thoughts_tokens) / 1_000_000.0) * self.price_per_1m_output
        return input_cost + output_cost

    def summary_str(self) -> str:
        with self.lock:
            out_total = self.total_candidates_tokens + self.total_thoughts_tokens
            return (
                f"[TOKENS] Calls: {self.total_calls} | In: {self.total_prompt_tokens:,} | "
                f"Out: {self.total_candidates_tokens:,} | Thoughts: {self.total_thoughts_tokens:,} "
                f"(Total Out: {out_total:,}) | Est Cost: ${self.estimated_cost_usd():.3f}"
            )


def format_sentence_prompt(record: Dict[str, Any]) -> str:
    """Constructs a detailed prompt for ambiguous tokens in a sentence,

    providing the full sentence context, both Zemberek's internal transition analysis
    and Oflazer's standard morphological tag parse, lemma, and POS.
    """
    sentence = record["text"]
    ambiguous_tokens = [t for t in record["tokens"] if t.get("is_ambiguous", False)]

    lines = [
        f'Turkish Sentence: "{sentence}"',
        "",
        f"There are {len(ambiguous_tokens)} ambiguous word(s) to disambiguate in this sentence context:",
        ""
    ]

    for t in ambiguous_tokens:
        lines.append(f"--- Word: \"{t['surface']}\" (token index: {t['index']}) ---")
        lines.append("Candidate Analyses:")
        for c in t.get("candidates", []):
            c_id = c.get("id", 0)
            lemma = c.get("lemma", "")
            pos = c.get("pos", "")
            sec_pos = c.get("secondary_pos", "")
            zemberek = c.get("zemberek_key", "")
            oflazer = c.get("oflazer_style", "")
            informal = " [Informal]" if c.get("is_informal") else ""
            pos_info = f"{pos}" + (f", {sec_pos}" if sec_pos and sec_pos != "None" else "")

            lines.append(
                f"  [{c_id}] Lemma: '{lemma}' ({pos_info}){informal}\n"
                f"       Zemberek Analysis : {zemberek}\n"
                f"       Oflazer Standard  : {oflazer}"
            )
        lines.append("")

    lines.append(
        "Carefully analyze how each word functions in the sentence syntax "
        "(e.g., modifier vs. head, finite verb vs. participle/converb, "
        "accusative vs. possessive object, noun vs. adjective) "
        "and select the single most accurate candidate ID for each ambiguous word."
    )
    return "\n".join(lines)


def call_gemini_sdk(
    client: Any,
    prompt: str,
    model: str = "gemini-flash-latest",
    temperature: float = 0.0,
    thinking_budget: int = 0,
    system_instruction: str = SYSTEM_INSTRUCTION_COMPACT,
    max_retries: int = 4
) -> Tuple[Optional[str], Dict[str, Any]]:
    """Calls Gemini API using the official google-genai SDK."""
    for attempt in range(max_retries):
        try:
            config_kwargs: Dict[str, Any] = {
                "temperature": temperature,
                "response_mime_type": "application/json",
                "system_instruction": system_instruction,
            }
            if thinking_budget is not None:
                try:
                    config_kwargs["thinking_config"] = types.ThinkingConfig(thinking_budget=thinking_budget)
                except Exception:
                    config_kwargs["thinking_config"] = {"thinking_budget": thinking_budget}

            config = types.GenerateContentConfig(**config_kwargs)
            response = client.models.generate_content(
                model=model,
                contents=prompt,
                config=config
            )
            usage: Dict[str, Any] = {}
            if hasattr(response, "usage_metadata") and response.usage_metadata:
                um = response.usage_metadata
                usage = {
                    "promptTokenCount": getattr(um, "prompt_token_count", 0),
                    "candidatesTokenCount": getattr(um, "candidates_token_count", 0),
                    "thoughtsTokenCount": getattr(um, "thoughts_token_count", 0),
                    "totalTokenCount": getattr(um, "total_token_count", 0),
                }
            return response.text.strip(), usage
        except Exception as e:
            wait = 2 ** attempt
            print(f"[WARN] Gemini SDK call failed (attempt {attempt + 1}/{max_retries}): {e}. Retrying in {wait}s...")
            time.sleep(wait)
    return None, {}


def call_gemini_rest(
    api_key: str,
    prompt: str,
    model: str = "gemini-flash-latest",
    temperature: float = 0.0,
    thinking_budget: int = 0,
    system_instruction: str = SYSTEM_INSTRUCTION_COMPACT,
    max_retries: int = 4
) -> Tuple[Optional[str], Dict[str, Any]]:
    """Calls Gemini REST API directly using requests."""
    if not HAS_REQUESTS:
        raise RuntimeError("Neither google-genai nor requests is available. Please install one of them.")

    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    generation_config: Dict[str, Any] = {
        "temperature": temperature,
        "responseMimeType": "application/json"
    }
    if thinking_budget is not None:
        generation_config["thinkingConfig"] = {
            "thinkingBudget": thinking_budget
        }

    payload = {
        "system_instruction": {
            "parts": [{"text": system_instruction}]
        },
        "contents": [
            {
                "parts": [{"text": prompt}]
            }
        ],
        "generationConfig": generation_config
    }

    for attempt in range(max_retries):
        try:
            resp = requests.post(url, json=payload, timeout=30)
            if resp.status_code == 200:
                data = resp.json()
                usage = data.get("usageMetadata", {})
                candidates = data.get("candidates", [])
                if candidates:
                    parts = candidates[0].get("content", {}).get("parts", [])
                    if parts:
                        return parts[0].get("text", "").strip(), usage
            elif resp.status_code == 429:
                wait = (2 ** attempt) * 2
                print(f"[WARN] Rate limited (429). Retrying in {wait}s...")
                time.sleep(wait)
            else:
                print(f"[WARN] HTTP {resp.status_code}: {resp.text}")
                wait = 2 ** attempt
                time.sleep(wait)
        except Exception as e:
            wait = 2 ** attempt
            print(f"[WARN] REST call error: {e}. Retrying in {wait}s...")
            time.sleep(wait)
    return None, {}


def parse_llm_json(response_text: str) -> Optional[Dict[str, Any]]:
    """Safely extracts and parses JSON from the LLM output."""
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
    except json.JSONDecodeError as e:
        print(f"[ERROR] Failed to parse LLM JSON: {e}\nRaw content:\n{response_text}")
        return None


def apply_selections(record: Dict[str, Any], selections_data: Dict[str, Any]) -> int:
    """Applies the LLM's selected_candidate_id to the ambiguous tokens in record."""
    selections = selections_data.get("selections", [])
    sel_map = {s["token_index"]: s for s in selections if "token_index" in s}

    applied_count = 0
    for token in record["tokens"]:
        if not token.get("is_ambiguous", False):
            continue

        idx = token["index"]
        if idx in sel_map:
            sel = sel_map[idx]
            chosen_id = sel.get("selected_candidate_id")
            # Validate that chosen_id exists in candidates
            valid_ids = [c["id"] for c in token.get("candidates", [])]
            if chosen_id in valid_ids:
                token["selected_candidate_id"] = chosen_id
                token["selection_reasoning"] = sel.get("reasoning", "")
                applied_count += 1
            else:
                print(f"[WARN] Candidate ID {chosen_id} is out of bounds for token '{token['surface']}'. Valid: {valid_ids}")
                # Fallback to candidate 0
                token["selected_candidate_id"] = valid_ids[0] if valid_ids else 0
                token["selection_reasoning"] = "Fallback: invalid ID from LLM"
        else:
            print(f"[WARN] No LLM selection returned for ambiguous token index {idx} ('{token['surface']}')")
            token["selected_candidate_id"] = 0
            token["selection_reasoning"] = "Fallback: missing from LLM response"

    return applied_count


def process_file(
    input_path: str,
    output_path: str,
    api_key: Optional[str],
    model: str = "gemini-flash-latest",
    thinking_budget: int = 0,
    with_reasoning: bool = False,
    max_output_per_call: int = 400,
    max_total_output_tokens: int = 500_000,
    max_budget_usd: float = 5.0,
    max_sentences: int = -1,
    delay: float = 0.0,
    concurrency: int = 5,
    meta_path: Optional[str] = None,
    dry_run: bool = False
) -> None:
    """Main processing loop with optional concurrency, token monitoring, and circuit breaking."""
    import concurrent.futures

    client = None
    if not dry_run:
        if not api_key:
            raise ValueError("GEMINI_API_KEY must be provided via environment variable, .env, or --api-key.")
        if HAS_GOOGLE_GENAI:
            print(f"[INFO] Using google-genai SDK with model: {model} (thinkingBudget={thinking_budget})")
            client = genai.Client(api_key=api_key)
        else:
            print(f"[INFO] google-genai SDK not installed. Falling back to REST API with model: {model} (thinkingBudget={thinking_budget})")

    system_instruction = SYSTEM_INSTRUCTION_VERBOSE if with_reasoning else SYSTEM_INSTRUCTION_COMPACT
    token_monitor = TokenMonitor(
        max_output_tokens_per_call=max_output_per_call,
        max_total_output_tokens=max_total_output_tokens,
        max_budget_usd=max_budget_usd
    )

    output_dir = os.path.dirname(output_path)
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)

    records = []
    with open(input_path, "r", encoding="utf-8") as in_f:
        for idx, line in enumerate(in_f):
            line = line.strip()
            if not line:
                continue
            records.append((idx, json.loads(line)))
            if max_sentences > 0 and len(records) >= max_sentences:
                break

    print(f"[INFO] Loaded {len(records)} sentences from {input_path}. Concurrency: {concurrency}")
    print(f"[INFO] Cost Safeguards: thinkingBudget={thinking_budget} | max_output_per_call={max_output_per_call} | "
          f"max_total_output={max_total_output_tokens:,} | max_budget=${max_budget_usd:.2f}")

    # Check for existing completed sentences (resume capability)
    existing_done_ids = set()
    total_sentences = 0
    annotated_sentences = 0
    annotated_tokens = 0

    if os.path.exists(output_path):
        with open(output_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    rec = json.loads(line)
                    sid = rec.get("sentence_id")
                    if sid:
                        existing_done_ids.add(sid)
                    total_sentences += 1
                    amb = [t for t in rec.get("tokens", []) if t.get("is_ambiguous")]
                    if amb:
                        annotated_sentences += 1
                        annotated_tokens += sum(1 for t in amb if "selected_candidate_id" in t)
                except Exception:
                    pass

        if existing_done_ids:
            print(f"[RESUME] Found {len(existing_done_ids)} existing annotated sentences in {output_path}. Resuming...")

    remaining_records = [r for r in records if r[1].get("sentence_id") not in existing_done_ids]
    if not remaining_records:
        print(f"[DONE] All {len(records)} sentences already annotated in {output_path}.")
        return

    print(f"[INFO] Processing {len(remaining_records)} remaining sentences (Total target: {len(records)}).")

    def process_item(item):
        line_num, record = item
        ambiguous_tokens = [t for t in record["tokens"] if t.get("is_ambiguous", False)]
        if not ambiguous_tokens:
            return line_num, record, 0, False

        # Circuit breaker check: immediately abort if budget/token limit exceeded
        if token_monitor.circuit_broken:
            return line_num, record, 0, False

        if dry_run:
            for t in ambiguous_tokens:
                t["selected_candidate_id"] = 0
                t["selection_reasoning"] = "[DRY RUN] Selected first candidate"
            return line_num, record, len(ambiguous_tokens), True

        prompt = format_sentence_prompt(record)
        sid = record.get("sentence_id", str(line_num))
        if client is not None:
            response_text, usage = call_gemini_sdk(
                client, prompt, model=model, thinking_budget=thinking_budget, system_instruction=system_instruction
            )
        else:
            response_text, usage = call_gemini_rest(
                api_key, prompt, model=model, thinking_budget=thinking_budget, system_instruction=system_instruction
            )

        token_monitor.record(usage, sentence_id=sid)

        applied = 0
        parsed_json = parse_llm_json(response_text)
        if parsed_json:
            applied = apply_selections(record, parsed_json)
        else:
            print(f"[ERROR] Skipping LLM annotation for sentence {sid} due to API failure")

        if delay > 0:
            time.sleep(delay)

        return line_num, record, applied, (applied > 0)

    # Open file in append mode if resuming, write mode if starting fresh
    mode = "a" if existing_done_ids else "w"
    with open(output_path, mode, encoding="utf-8") as out_f:
        workers = min(concurrency, max(1, len(remaining_records)))
        with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as executor:
            results = executor.map(process_item, remaining_records)
            for line_num, rec, applied, was_annotated in results:
                if token_monitor.circuit_broken:
                    print(f"\n[CIRCUIT BREAKER TRIGGERED] {token_monitor.circuit_break_reason}")
                    print(f"[HALTING] Stopping pipeline to prevent unwanted charges. Progress so far has been flushed to {output_path}.")
                    break

                out_f.write(json.dumps(rec, ensure_ascii=False) + "\n")
                out_f.flush()
                total_sentences += 1
                if was_annotated:
                    annotated_sentences += 1
                    annotated_tokens += applied

                if total_sentences % 25 == 0 or total_sentences == len(records):
                    print(f"[PROGRESS] Completed {total_sentences}/{len(records)} sentences | {token_monitor.summary_str()}")

    print(f"\n[DONE] Finished processing {total_sentences} sentences.")
    print(f"       Annotated sentences with LLM: {annotated_sentences}")
    print(f"       Annotated ambiguous tokens:   {annotated_tokens}")
    print(f"       {token_monitor.summary_str()}")
    print(f"       Output written to:            {output_path}")

    # Update companion metadata file if present or requested
    target_meta = meta_path
    if not target_meta:
        default_meta = input_path.replace(".jsonl", ".meta.json") if input_path.endswith(".jsonl") else input_path + ".meta.json"
        if os.path.exists(default_meta):
            target_meta = default_meta

    if target_meta and os.path.exists(target_meta):
        try:
            with open(target_meta, "r", encoding="utf-8") as f:
                meta = json.load(f)

            meta["annotation"] = {
                "status": "circuit_broken" if token_monitor.circuit_broken else "completed",
                "annotator_model": "dry-run" if dry_run else model,
                "annotated_at": datetime.now(timezone.utc).isoformat(),
                "annotated_sentences": annotated_sentences,
                "annotated_tokens": annotated_tokens,
                "thinking_budget": thinking_budget,
                "token_usage": {
                    "prompt_tokens": token_monitor.total_prompt_tokens,
                    "candidates_tokens": token_monitor.total_candidates_tokens,
                    "thoughts_tokens": token_monitor.total_thoughts_tokens,
                    "total_output_tokens": token_monitor.total_candidates_tokens + token_monitor.total_thoughts_tokens,
                    "estimated_cost_usd": round(token_monitor.estimated_cost_usd(), 4)
                },
                "output_file": os.path.basename(output_path)
            }

            out_meta = output_path.replace(".jsonl", ".meta.json") if output_path.endswith(".jsonl") else output_path + ".meta.json"
            with open(out_meta, "w", encoding="utf-8") as f:
                json.dump(meta, f, indent=2, ensure_ascii=False)
            print(f"[INFO] Updated annotation metadata in: {out_meta}")
        except Exception as e:
            print(f"[WARN] Could not update metadata file: {e}")


def main():
    load_env()
    load_env(".env.local")

    parser = argparse.ArgumentParser(
        description="Disambiguate Turkish morphological candidates using Google Gemini with cost safeguards and monitoring.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    parser.add_argument("-i", "--input", required=True, help="Input JSONL file produced by DisambiguationCandidateExtractor")
    parser.add_argument("-o", "--output", required=True, help="Output JSONL file to store annotations")
    parser.add_argument("-m", "--model", default="gemini-flash-latest", help="Gemini model name")
    parser.add_argument("--thinking-budget", type=int, default=0, help="Thinking token budget (0 disables hidden reasoning tokens)")
    parser.add_argument("--with-reasoning", action="store_true", help="Include natural language explanations (higher output token cost)")
    parser.add_argument("--max-output-per-call", type=int, default=400, help="Warning threshold for output tokens per single API call")
    parser.add_argument("--max-total-output-tokens", type=int, default=500_000, help="Circuit breaker: stop if cumulative output tokens exceed this")
    parser.add_argument("--max-budget-usd", type=float, default=5.0, help="Circuit breaker: stop if estimated cost in USD exceeds this")
    parser.add_argument("--api-key", default=None, help="Gemini API key (defaults to GEMINI_API_KEY environment variable)")
    parser.add_argument("--max-sentences", "-max", type=int, default=-1, help="Max sentences to process (-1 for unlimited)")
    parser.add_argument("--delay", "-d", type=float, default=0.0, help="Delay between API requests in seconds")
    parser.add_argument("--concurrency", "-c", type=int, default=5, help="Number of concurrent API workers (default: 5)")
    parser.add_argument("--meta", default=None, help="Path to companion .meta.json file to update")
    parser.add_argument("--dry-run", action="store_true", help="Simulate annotation without calling the Gemini API")

    args = parser.parse_args()

    api_key = args.api_key or os.environ.get("GEMINI_API_KEY")

    if not args.dry_run and not api_key:
        print("[ERROR] GEMINI_API_KEY is not set. Use --api-key, set the environment variable, or use --dry-run for testing.")
        sys.exit(1)

    process_file(
        input_path=args.input,
        output_path=args.output,
        api_key=api_key,
        model=args.model,
        thinking_budget=args.thinking_budget,
        with_reasoning=args.with_reasoning,
        max_output_per_call=args.max_output_per_call,
        max_total_output_tokens=args.max_total_output_tokens,
        max_budget_usd=args.max_budget_usd,
        max_sentences=args.max_sentences,
        delay=args.delay,
        concurrency=args.concurrency,
        meta_path=args.meta,
        dry_run=args.dry_run
    )


if __name__ == "__main__":
    main()
