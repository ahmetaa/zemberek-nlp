#!/usr/bin/env python3
"""
Extracts empirically balanced ambiguous words and regular inflectional homonyms
from Universal Dependencies treebanks (BOUN + IMST) and zemberek_shared resources
at large scale (~600-750 high-yield targets across 6 major categories).

Filters out rare, skewed (>90% dominant), archaic, and contrived FST artifacts,
leaving only genuine, high-yield competing ambiguities for contrastive pair generation.

Usage:
  python3 experiment/scripts/extract_balanced_ambiguities.py \
    -o data/ambiguity/sources/targeted_ambiguity_specs_large.json
"""

import argparse
import json
import re
from collections import defaultdict, Counter
from pathlib import Path
from typing import Dict, List, Any


def extract_treebank_balanced_words(
    treebank_files: List[Path],
    min_count: int = 8,
    min_ratio: float = 0.15,
    max_targets: int = 250
) -> List[Dict[str, Any]]:
    word_parses = defaultdict(Counter)

    for fn in treebank_files:
        if not fn.exists():
            continue
        with open(fn, "r", encoding="utf-8") as f:
            for line in f:
                sent = json.loads(line)
                for tok in sent.get("tokens", []):
                    if tok.get("is_ambiguous") and "selected_candidate_id" in tok:
                        surf = tok["surface"].lower()
                        # Skip punctuation or single letter tokens
                        if len(surf) <= 1 or not surf.isalpha():
                            continue
                        sel_id = tok["selected_candidate_id"]
                        if 0 <= sel_id < len(tok["candidates"]):
                            cand = tok["candidates"][sel_id]
                            key = cand.get("zemberek_key") or cand.get("oflazer_style", "")
                            word_parses[surf][key] += 1

    balanced = []
    for word, counts in word_parses.items():
        total = sum(counts.values())
        if total >= min_count and len(counts) >= 2:
            top2 = counts.most_common(2)
            c1, c2 = top2[0][1], top2[1][1]
            ratio = c2 / total
            if ratio >= min_ratio:
                balanced.append({
                    "word": word,
                    "category": "functional_competition",
                    "total_corpus_count": total,
                    "balance_ratio": round(ratio, 3),
                    "sense1": {
                        "key": top2[0][0],
                        "count": c1,
                        "ratio": round(c1 / total, 3)
                    },
                    "sense2": {
                        "key": top2[1][0],
                        "count": c2,
                        "ratio": round(c2 / total, 3)
                    }
                })

    balanced.sort(key=lambda x: x["total_corpus_count"], reverse=True)
    return balanced[:max_targets]


def extract_ambiguity_group_blocks(
    ambiguity_groups_file: Path,
    max_acc_p3sg: int = 180,
    max_gen_p2sg: int = 80,
    max_participle: int = 60,
    max_adj_noun: int = 60,
    max_adv_adj: int = 40
) -> List[Dict[str, Any]]:
    if not ambiguity_groups_file.exists():
        return []

    text = ambiguity_groups_file.read_text(encoding="utf-8")
    blocks = text.split("\n\n")

    acc_p3sg_targets = []
    gen_p2sg_targets = []
    participle_targets = []
    adj_noun_targets = []
    adv_adj_targets = []

    seen = set()

    for b in blocks:
        lines = [l.strip() for l in b.split("\n") if l.strip()]
        if not lines:
            continue
        headers = [l for l in lines if l.startswith("[")]
        words = [l for l in lines if not l.startswith("[") and ":" in l]
        h = " / ".join(headers)

        # 1. Accusative vs Possessive 3sg
        if "Acc" in h and "P3sg" in h and len(acc_p3sg_targets) < max_acc_p3sg:
            for w_line in words:
                p = w_line.split(":")
                w = p[0].strip().lower()
                freq = int(p[1].strip().split()[0])
                if len(w) >= 3 and w.isalpha() and freq >= 30 and w not in seen:
                    seen.add(w)
                    acc_p3sg_targets.append({
                        "word": w,
                        "category": "noun_acc_vs_poss3sg",
                        "corpus_freq": freq,
                        "sense1": {
                            "desc": "Definite direct object with Accusative case (-i/-ı/-u/-ü) governed by an overt transitive verb"
                        },
                        "sense2": {
                            "desc": "Possessed noun with 3rd person possessive suffix (-i/-ı/-u/-ü) in a genitive or possessive construction"
                        }
                    })
                    if len(acc_p3sg_targets) >= max_acc_p3sg:
                        break

        # 2. Genitive vs Possessive 2sg
        elif "Gen" in h and "P2sg" in h and len(gen_p2sg_targets) < max_gen_p2sg:
            for w_line in words:
                p = w_line.split(":")
                w = p[0].strip().lower()
                freq = int(p[1].strip().split()[0])
                if len(w) >= 3 and w.isalpha() and freq >= 25 and w not in seen:
                    seen.add(w)
                    gen_p2sg_targets.append({
                        "word": w,
                        "category": "noun_gen_vs_poss2sg",
                        "corpus_freq": freq,
                        "sense1": {
                            "desc": "Genitive case suffix -(n)ın/-(n)in/-(n)un/-(n)ün marking a noun as possessor in a genitive noun phrase (e.g. evin kapısı)"
                        },
                        "sense2": {
                            "desc": "2nd person possessive agreement suffix -(ı)n/-(i)n/-(u)n/-(ü)n marking possession by 'sen' (e.g. senin evin)"
                        }
                    })
                    if len(gen_p2sg_targets) >= max_gen_p2sg:
                        break

        # 3. Past Participle Relative Clause vs Nominalized Participle
        elif "PastPart" in h and len(participle_targets) < max_participle:
            for w_line in words:
                p = w_line.split(":")
                w = p[0].strip().lower()
                freq = int(p[1].strip().split()[0])
                if len(w) >= 3 and w.isalpha() and freq >= 30 and w not in seen:
                    seen.add(w)
                    participle_targets.append({
                        "word": w,
                        "category": "participle_adj_vs_noun",
                        "corpus_freq": freq,
                        "sense1": {
                            "desc": "Past participle suffix -dık/-dik with 3sg possessive acting as a relative clause adjective modifying a following noun (e.g. yaptığı iş)"
                        },
                        "sense2": {
                            "desc": "Nominalized past participle acting as an independent noun argument or direct object (e.g. Yaptığını hiç beğenmedim)"
                        }
                    })
                    if len(participle_targets) >= max_participle:
                        break

        # 4. Adjective vs Substantive Noun
        elif "[(Adj)]" in h and "[(Noun;)]" in h and len(adj_noun_targets) < max_adj_noun:
            for w_line in words:
                p = w_line.split(":")
                w = p[0].strip().lower()
                freq = int(p[1].strip().split()[0])
                if len(w) >= 3 and w.isalpha() and freq >= 30 and w not in seen:
                    seen.add(w)
                    adj_noun_targets.append({
                        "word": w,
                        "category": "adj_vs_noun",
                        "corpus_freq": freq,
                        "sense1": {
                            "desc": "Descriptive Adjective modifying an immediately following nominal head (e.g. genç adam, hasta yolcu)"
                        },
                        "sense2": {
                            "desc": "Substantive Noun functioning as an independent clause subject or object (e.g. Gençler öne çıktı, Hasta doktora gitti)"
                        }
                    })
                    if len(adj_noun_targets) >= max_adj_noun:
                        break

        # 5. Manner Adverb vs Descriptive Adjective
        elif "[(Adv)]" in h and "[(Adj)]" in h and len(adv_adj_targets) < max_adv_adj:
            for w_line in words:
                p = w_line.split(":")
                w = p[0].strip().lower()
                freq = int(p[1].strip().split()[0])
                if len(w) >= 3 and w.isalpha() and freq >= 20 and w not in seen:
                    seen.add(w)
                    adv_adj_targets.append({
                        "word": w,
                        "category": "adv_vs_adj",
                        "corpus_freq": freq,
                        "sense1": {
                            "desc": "Manner Adverb modifying a verb or predicate (e.g. güzel konuştu, hızlı yürüdü, kolay bitti)"
                        },
                        "sense2": {
                            "desc": "Descriptive Adjective modifying a following noun (e.g. güzel bir film, hızlı tren, kolay soru)"
                        }
                    })
                    if len(adv_adj_targets) >= max_adv_adj:
                        break

    return acc_p3sg_targets + gen_p2sg_targets + participle_targets + adj_noun_targets + adv_adj_targets


def load_anchor_collocations(
    collocations_file: Path,
    trigrams_file: Path
) -> Dict[str, List[str]]:
    anchors = defaultdict(list)

    if collocations_file.exists():
        with open(collocations_file, "r", encoding="utf-8") as f:
            for line in f:
                if ":" in line and not line.strip().startswith("(") and not line.strip().startswith("["):
                    phrase = line.split(":")[0].strip().lower()
                    words = phrase.split()
                    for w in words:
                        if len(anchors[w]) < 4 and len(phrase) > len(w):
                            anchors[w].append(phrase)

    if trigrams_file.exists():
        with open(trigrams_file, "r", encoding="utf-8") as f:
            for line in f:
                if ":" in line and not line.strip().startswith("(") and not line.strip().startswith("["):
                    phrase = line.split(":")[0].strip().lower()
                    words = phrase.split()
                    for w in words:
                        if len(anchors[w]) < 4 and phrase not in anchors[w]:
                            anchors[w].append(phrase)

    return anchors


def main():
    parser = argparse.ArgumentParser(description="Extract balanced, high-yield ambiguity targets at large scale")
    parser.add_argument("-o", "--output", required=True, type=Path, help="Output JSON path")
    args = parser.parse_args()

    treebank_files = [
        Path("data/ambiguity/sources/UD_Turkish-BOUN/boun_train_annotated.jsonl"),
        Path("data/ambiguity/sources/UD_Turkish-BOUN/boun_dev_annotated.jsonl"),
        Path("data/ambiguity/sources/UD_Turkish-IMST/imst_train_annotated.jsonl"),
        Path("data/ambiguity/sources/UD_Turkish-IMST/imst_dev_annotated.jsonl")
    ]
    ambiguity_groups = Path("data/ambiguity/sources/zemberek_shared/ambiguity_groups.txt")
    collocations_file = Path("data/ambiguity/sources/zemberek_shared/significant-bigrams.txt")
    trigrams_file = Path("data/ambiguity/sources/zemberek_shared/significant_trigrams.txt")

    print("[INFO] Extracting treebank balanced words (min_count=8, min_ratio=0.15)...")
    treebank_targets = extract_treebank_balanced_words(treebank_files, min_count=8, min_ratio=0.15, max_targets=250)
    print(f"[INFO] Found {len(treebank_targets)} empirically balanced functional targets.")

    print("[INFO] Extracting structured ambiguity group targets from zemberek_shared...")
    group_targets = extract_ambiguity_group_blocks(ambiguity_groups)
    print(f"[INFO] Found {len(group_targets)} structured group targets.")

    print("[INFO] Loading anchor collocations...")
    anchors = load_anchor_collocations(collocations_file, trigrams_file)

    # Merge and annotate with collocations
    all_targets = []
    seen = set()

    for t in treebank_targets:
        w = t["word"]
        if w not in seen:
            seen.add(w)
            t["anchor_collocations"] = anchors.get(w, [])
            all_targets.append(t)

    for t in group_targets:
        w = t["word"]
        if w not in seen:
            seen.add(w)
            t["anchor_collocations"] = anchors.get(w, [])
            all_targets.append(t)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(all_targets, f, ensure_ascii=False, indent=2)

    cat_counts = Counter(t["category"] for t in all_targets)
    print(f"[SUCCESS] Saved {len(all_targets)} targeted ambiguity specs to: {args.output}")
    print("Category breakdown:")
    for cat, count in cat_counts.most_common():
        print(f"  * {cat:30s}: {count}")


if __name__ == "__main__":
    main()
