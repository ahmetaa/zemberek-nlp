#!/usr/bin/env python3
"""
Generates high-yield, filtered contrastive minimal sentence pairs for Turkish morphological disambiguation at scale.

Evaluates ambiguous words from targeted_ambiguity_specs_large.json using an LLM semantic gating check:
- If both parses represent common, natural Turkish -> Generates 2 diverse, authentic sentences per sense (4 sentences total per target word).
- If one parse is archaic, a contrived FST dictionary artifact, or unnatural -> Skips with concise reason.

Preserves previously generated sentences via --existing flag, appending newly generated sentences without duplicates.

Usage:
  python3 experiment/scripts/generate_contrastive_pairs.py \
    -i data/ambiguity/sources/targeted_ambiguity_specs_large.json \
    -o data/ambiguity/contrastive_sentences.txt \
    --existing data/ambiguity/contrastive_sentences_part1_338.txt \
    --batch-size 6 \
    --concurrency 6
"""

import argparse
import json
import os
import re
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Dict, List, Any, Optional

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


def load_api_key() -> str:
    key = os.environ.get("GEMINI_API_KEY", "")
    if key:
        return key

    for p in [Path(".env"), Path("../.env"), Path.home() / ".gemini" / ".env"]:
        if p.exists():
            try:
                for line in p.read_text(encoding="utf-8").splitlines():
                    line = line.strip()
                    if line.startswith("GEMINI_API_KEY="):
                        val = line.split("=", 1)[1].strip().strip('"').strip("'")
                        if val:
                            return val
            except Exception:
                pass
    return ""


def clean_sentence(s: str) -> str:
    if not isinstance(s, str):
        return ""
    s = re.sub(r"\s+", " ", s).strip()
    s = re.sub(r"^[\d\.\-\*\)\s]+", "", s).strip()
    return s


SYSTEM_INSTRUCTION = """You are an expert computational linguist specializing in Turkish morphology, syntax, and morphological disambiguation.
You are given ambiguous Turkish words along with two candidate morphological/syntactic analyses (Sense 1 and Sense 2).

YOUR TASKS:
1. SEMANTIC & USAGE GATING (Filter Out Artificial / Archaic Senses):
   - Evaluate whether BOTH Sense 1 and Sense 2 represent active, common, natural usage in contemporary standard Turkish.
   - If EITHER sense is:
     * An archaic, obsolete, or literary-only form (e.g. 'için' as imperative verb, 'ben' as facial mole),
     * An artificial rule-based morphological analyzer / FST dictionary artifact (e.g. zero-derivation duplicates, accidental homographs),
     * A contrived imperative verb or forced inflection (e.g. 'vardır' as causative imperative of varmak, 'gerek' as 2nd person imperative),
     * Extremely rare, bizarre, or unnatural in everyday Turkish:
     -> Set status: "SKIP"
     -> Set reason: A concise explanation of why it is unnatural or an artifact.
   - If BOTH senses represent genuine, frequent, competing interpretations in modern Turkish:
     -> Set status: "ACCEPTED"

2. CONTRASTIVE SENTENCE GENERATION (Only when status == "ACCEPTED"):
   - For Sense 1: Generate TWO distinctly varied, authentic Turkish sentences (sentences_sense1) where the target word UNAMBIGUOUSLY has Sense 1.
   - For Sense 2: Generate TWO distinctly varied, authentic Turkish sentences (sentences_sense2) where the target word UNAMBIGUOUSLY has Sense 2.
   - Vary the grammatical context between the two sentences (e.g., past vs. future/present tense, question vs. declarative, different subjects or clausal positions).

SYNTACTIC CUE GUIDELINES:
- For Accusative case (-i / -ı / -u / -ü): Use an overt transitive verb governing the word as a definite direct object (e.g., 'evi temizledik', 'kitabı okudum', 'sistemi baştan başlattı').
- For 3rd-person Possessive (-i / -ı / -u / -ü): Provide clear possessive context, preferably with an overt genitive modifier (e.g., 'Ahmet'in evi', 'onun kitabı', 'yazarın son eseri', 'arabanın motoru').
- For Genitive case (-(n)ın / -(n)in / -(n)un / -(n)ün): Must serve as the possessor modifier in a genitive construction governing a possessed head (e.g., 'evin kapısı', 'işin sonu', 'arabanın tekeri').
- For 2nd-person Possessive (-(ı)n / -(i)n / -(u)n / -(ü)n): Must clearly indicate possession by 'sen' (e.g., 'senin evin çok güzel', 'yeni aldığın araban', 'senin işin ne zaman biter?').
- For Past Participle Relative Clause (-dık/-dik + P3sg as Adj): Must modify an adjacent nominal head (e.g., 'yaptığı yemek', 'aldığı karar', 'yaşadığı şehir', 'gördüğü manzara').
- For Nominalized Participle (Noun+Acc / Noun): Must function as an independent direct object or clausal argument (e.g., 'Yaptığını hiç beğenmedim.', 'Gördüğünü derhal polise anlattı.').
- For Determiner: Place directly before the nominal head it modifies (e.g., 'bu proje', 'o gün', 'böyle insanlar').
- For Pronoun: Use as an independent syntactic argument (subject or object), often set off by punctuation or case marking (e.g., 'Bu, hepimizi şaşırttı.', 'Ona gerçeği anlattım.').
- For Temporal Adverb (e.g. 'önce', 'sonra'): Use as an independent clausal adverb meaning 'first/initially' or 'later/afterwards', WITHOUT any ablative-marked complement (e.g., 'Önce ellerini yıkadı.', 'Bunu biraz sonra konuşalım.').
- For Postposition (e.g. 'önce', 'sonra', 'doğru'): Must govern an overt complement with the required case (e.g. ablative 'toplantıdan sonra', 'dersten önce'; dative 'eve doğru').
- For Adjective vs Adverb: Adjectives must modify a noun (e.g., 'güzel bir film', 'hızlı tren'); Adverbs must modify a verb or predicate (e.g., 'güzel konuştu', 'hızlı yürüdü').
- For Conjunction (e.g. 'ne ... ne', 'değil'): Ensure genuine clausal or correlative conjunction structure (e.g., 'ne aradı ne sordu', 'yalnız ben değil, herkes katıldı').

OUTPUT REQUIREMENTS:
- All generated sentences MUST be in natural, grammatically flawless contemporary standard Turkish.
- Keep sentences realistic, fluent, and varied (as found in reputable Turkish journalism, essays, literature, or natural modern discourse).
- Output must strictly conform to the requested JSON schema.
"""


def build_batch_prompt(batch: List[Dict[str, Any]]) -> str:
    lines = ["Evaluate each of the following ambiguous Turkish words and return the results in JSON format:\n"]
    for idx, item in enumerate(batch):
        w = item["word"]
        s1 = item.get("sense1", {}).get("desc") or item.get("sense1", {}).get("key", "")
        s2 = item.get("sense2", {}).get("desc") or item.get("sense2", {}).get("key", "")
        collocs = ", ".join(item.get("anchor_collocations", []))

        lines.append(f"Word {idx + 1}: '{w}'")
        lines.append(f"  Sense 1: {s1}")
        lines.append(f"  Sense 2: {s2}")
        if collocs:
            lines.append(f"  (Common contextual collocations: {collocs})")
        lines.append("")

    lines.append("""
JSON Output Schema:
{
  "evaluations": [
    {
      "word": "target_word",
      "status": "ACCEPTED" or "SKIP",
      "reason": "Brief rejection reason if SKIP, else empty string",
      "sentences_sense1": [
        "First natural Turkish sentence demonstrating Sense 1",
        "Second natural Turkish sentence demonstrating Sense 1 (varied syntactic context)"
      ],
      "sentences_sense2": [
        "First natural Turkish sentence demonstrating Sense 2",
        "Second natural Turkish sentence demonstrating Sense 2 (varied syntactic context)"
      ]
    }
  ]
}
""")
    return "\n".join(lines)


def call_gemini(
    prompt: str,
    api_key: str,
    model_name: str = "gemini-flash-latest"
) -> List[Dict[str, Any]]:
    # 1. Try google-genai SDK
    if HAS_GOOGLE_GENAI:
        try:
            client = genai.Client(api_key=api_key)
            resp = client.models.generate_content(
                model=model_name,
                contents=prompt,
                config=types.GenerateContentConfig(
                    system_instruction=SYSTEM_INSTRUCTION,
                    temperature=0.7,
                    response_mime_type="application/json"
                )
            )
            raw = resp.text.strip()
            return parse_evaluations_json(raw)
        except Exception:
            pass

    # 2. Fallback direct REST
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={api_key}"
    payload = {
        "system_instruction": {"parts": [{"text": SYSTEM_INSTRUCTION}]},
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": 0.7,
            "responseMimeType": "application/json"
        }
    }

    for attempt in range(4):
        try:
            resp = requests.post(url, json=payload, timeout=45)
            if resp.status_code == 200:
                data = resp.json()
                parts = data.get("candidates", [])[0].get("content", {}).get("parts", [])
                if parts:
                    return parse_evaluations_json(parts[0].get("text", ""))
            elif resp.status_code in (429, 503):
                time.sleep(2 * (attempt + 1))
            else:
                time.sleep(1)
        except Exception:
            time.sleep(2)

    return []


def parse_evaluations_json(raw: str) -> List[Dict[str, Any]]:
    cleaned = raw.strip()
    if cleaned.startswith("```json"):
        cleaned = cleaned[7:]
    elif cleaned.startswith("```"):
        cleaned = cleaned[3:]
    if cleaned.endswith("```"):
        cleaned = cleaned[:-3]
    cleaned = cleaned.strip()

    try:
        data = json.loads(cleaned)
        if isinstance(data, dict) and "evaluations" in data:
            return data["evaluations"]
        elif isinstance(data, list):
            return data
    except Exception:
        pass
    return []


def extract_sentences_from_eval(ev: Dict[str, Any]) -> List[str]:
    sentences = []
    # 1. sentences_sense1
    s1 = ev.get("sentences_sense1", [])
    if isinstance(s1, list):
        for s in s1:
            cs = clean_sentence(s)
            if cs:
                sentences.append(cs)
    elif isinstance(s1, str):
        cs = clean_sentence(s1)
        if cs:
            sentences.append(cs)

    # 2. sentences_sense2
    s2 = ev.get("sentences_sense2", [])
    if isinstance(s2, list):
        for s in s2:
            cs = clean_sentence(s)
            if cs:
                sentences.append(cs)
    elif isinstance(s2, str):
        cs = clean_sentence(s2)
        if cs:
            sentences.append(cs)

    # Fallbacks for legacy single-sentence keys
    for key in ["sentence_1", "sentence_2", "sentence_1a", "sentence_1b", "sentence_2a", "sentence_2b"]:
        val = ev.get(key, "")
        if isinstance(val, str) and val.strip():
            cs = clean_sentence(val)
            if cs and cs not in sentences:
                sentences.append(cs)

    return sentences


def main():
    parser = argparse.ArgumentParser(description="Generate high-yield filtered contrastive sentence pairs at large scale")
    parser.add_argument("-i", "--input", required=True, type=Path, help="Path to targeted ambiguity specs JSON")
    parser.add_argument("-o", "--output", required=True, type=Path, help="Output plain text sentences path")
    parser.add_argument("--existing", type=Path, default=None, help="Existing sentences file to preserve and append to")
    parser.add_argument("--meta", type=Path, default=None, help="Output metadata JSON path (defaults to <out>.meta.json)")
    parser.add_argument("--batch-size", type=int, default=6, help="Target words per LLM prompt (default 6)")
    parser.add_argument("--concurrency", type=int, default=6, help="Parallel worker threads (default 6)")
    parser.add_argument("--limit", type=int, default=None, help="Limit number of target words processed")
    parser.add_argument("--model", type=str, default="gemini-flash-latest", help="Gemini model name")
    args = parser.parse_args()

    api_key = load_api_key()
    if not api_key:
        print("[ERROR] GEMINI_API_KEY is not set. Please set it in your environment or .env file.")
        sys.exit(1)

    if not args.input.exists():
        print(f"[ERROR] Input file not found: {args.input}")
        sys.exit(1)

    with open(args.input, "r", encoding="utf-8") as f:
        targets = json.load(f)

    if args.limit:
        targets = targets[:args.limit]

    print(f"[INFO] Loaded {len(targets)} targets from {args.input}")

    # Load existing sentences to preserve
    existing_sentences: List[str] = []
    if args.existing and args.existing.exists():
        with open(args.existing, "r", encoding="utf-8") as f:
            for line in f:
                s = clean_sentence(line)
                if s and s not in existing_sentences:
                    existing_sentences.append(s)
        print(f"[INFO] Preserved {len(existing_sentences)} existing sentences from {args.existing}")

    meta_path = args.meta or (args.output.parent / (args.output.name + ".meta.json"))
    args.output.parent.mkdir(parents=True, exist_ok=True)

    # Chunk targets into batches
    batches = [targets[i:i + args.batch_size] for i in range(0, len(targets), args.batch_size)]
    print(f"[INFO] Grouped into {len(batches)} batches (batch size {args.batch_size}, concurrency {args.concurrency})")

    results: List[Dict[str, Any]] = []
    accepted_count = 0
    skipped_count = 0
    newly_generated_sentences: List[str] = []

    def process_batch(b_idx: int, b_items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        prompt = build_batch_prompt(b_items)
        evals = call_gemini(prompt, api_key, model_name=args.model)
        eval_map = {}
        for ev in evals:
            if isinstance(ev, dict) and "word" in ev:
                eval_map[str(ev["word"]).strip().lower()] = ev

        batch_res = []
        for idx, orig in enumerate(b_items):
            w_key = orig["word"].strip().lower()
            ev = eval_map.get(w_key)
            if not ev and idx < len(evals) and isinstance(evals[idx], dict):
                ev = evals[idx]
            if not ev:
                ev = {"status": "SKIP", "reason": "No response received for word"}

            res = dict(orig)
            res["status"] = ev.get("status", "SKIP")
            res["reason"] = ev.get("reason", "")
            res["generated_sentences"] = extract_sentences_from_eval(ev)
            batch_res.append(res)
        return batch_res

    print(f"[INFO] Launching generation on {args.model}...")
    start_time = time.time()

    with ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        futures = {executor.submit(process_batch, idx, b): idx for idx, b in enumerate(batches)}
        for future in as_completed(futures):
            b_idx = futures[future]
            try:
                batch_res = future.result()
                for item in batch_res:
                    results.append(item)
                    sents = item.get("generated_sentences", [])
                    if item.get("status") == "ACCEPTED" and sents:
                        accepted_count += 1
                        for s in sents:
                            if s not in newly_generated_sentences:
                                newly_generated_sentences.append(s)
                    else:
                        skipped_count += 1
                sys.stdout.write(f"\r[PROGRESS] Processed {len(results)}/{len(targets)} targets | "
                                 f"Accepted: {accepted_count} ({len(newly_generated_sentences)} new sentences) | "
                                 f"Skipped: {skipped_count}")
                sys.stdout.flush()
            except Exception as e:
                print(f"\n[ERROR] Batch {b_idx} failed: {e}")

    elapsed = time.time() - start_time
    print(f"\n[INFO] Generation finished in {elapsed:.1f}s.")

    # Combine: existing sentences FIRST, then new sentences
    combined_sentences: List[str] = list(existing_sentences)
    new_added_count = 0
    for s in newly_generated_sentences:
        if s not in combined_sentences:
            combined_sentences.append(s)
            new_added_count += 1

    with open(args.output, "w", encoding="utf-8") as f:
        for s in combined_sentences:
            f.write(s + "\n")

    # Write detailed metadata report
    meta_data = {
        "total_targets_evaluated": len(results),
        "accepted_targets": accepted_count,
        "skipped_count": skipped_count,
        "preserved_existing_sentences": len(existing_sentences),
        "newly_added_sentences": new_added_count,
        "total_sentences_in_output": len(combined_sentences),
        "model": args.model,
        "elapsed_seconds": round(elapsed, 2),
        "evaluations": results
    }
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(meta_data, f, ensure_ascii=False, indent=2)

    print(f"\n[SUCCESS] Combined total sentences: {len(combined_sentences)}")
    print(f"          - Preserved existing sentences: {len(existing_sentences)}")
    print(f"          - Newly generated & appended:   {new_added_count}")
    print(f"[SUCCESS] Wrote dataset to: {args.output}")
    print(f"[SUCCESS] Saved metadata to: {meta_path}")


if __name__ == "__main__":
    main()
