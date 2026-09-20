#!/usr/bin/env python3
"""
Aligns Universal Dependencies (CoNLL-U) gold morphological annotations
with Zemberek morphological candidate analyses from DisambiguationCandidateExtractor.

Outputs an annotated JSONL dataset compatible with Zemberek's TrainAmbiguityModel
and EvaluateAmbiguityModel.
"""

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


def tr_lower(s: str) -> str:
    """Turkish-aware lowercasing handling dotted/dotless I."""
    return s.replace("İ", "i").replace("I", "ı").lower()


def parse_conllu(path: Path) -> List[List[Dict[str, Any]]]:
    """
    Parses a CoNLL-U file into a list of sentences.
    Each sentence is a list of token dicts with surface form and CoNLL-U fields.
    Multi-word tokens (e.g., '2-3 yılındayız') are grouped with their component parts.
    """
    sentences = []
    curr = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                if curr:
                    sentences.append(curr)
                    curr = []
                continue
            if line.startswith("#"):
                continue
            parts = line.split("\t")
            if len(parts) < 6:
                continue
            token_id = parts[0]
            if "-" in token_id:
                # Multi-word token header
                curr.append({"surface": parts[1], "is_mwt": True, "parts": []})
            else:
                if curr and curr[-1].get("is_mwt") and len(curr[-1]["parts"]) < 2:
                    curr[-1]["parts"].append(parts)
                else:
                    curr.append({"surface": parts[1], "is_mwt": False, "parts": [parts]})
    if curr:
        sentences.append(curr)
    return sentences


def score_candidate(cand: Dict[str, Any], conllu_tok: Dict[str, Any]) -> float:
    """
    Scores a Zemberek candidate against a CoNLL-U gold token using:
    - UPOS and XPOS alignment
    - Turkish lemma matching
    - Grammatical Case, Number, Possessive agreement
    - Verb forms (Participles, Converbs, Verbal Nouns)
    - Voice (Passive, Causative)
    - Pronoun Types
    """
    main_part = conllu_tok["parts"][0]
    lemma = tr_lower(main_part[2])
    upos = main_part[3]
    xpos = main_part[4]
    feats = dict(item.split("=", 1) for item in main_part[5].split("|") if "=" in item)

    score = 0.0
    c_pos = cand.get("pos", "")
    c_sec_pos = cand.get("secondary_pos", "")
    morphs = set(cand.get("morphemes", []))
    zkey = cand.get("zemberek_key", "")

    # 1. UPOS matching
    if upos == "PROPN":
        if c_sec_pos == "Prop":
            score += 12.0
        else:
            score -= 5.0
    elif upos == "NOUN":
        if c_sec_pos == "Prop":
            score -= 6.0
        if c_pos == "Noun":
            score += 10.0
        # When UPOS is plain NOUN, penalize predicate-noun zero-derivations
        if "Zero" in morphs:
            score -= 8.0
    elif upos == "VERB" and c_pos == "Verb":
        score += 10.0
    elif upos == "ADJ" and (c_pos == "Adj" or "Adj" in morphs):
        score += 10.0
    elif upos == "ADV" and (c_pos == "Adv" or "Adv" in morphs):
        score += 10.0
    elif upos == "PRON" and c_pos == "Pron":
        score += 10.0
    elif upos == "DET" and c_pos == "Det":
        score += 10.0
    elif upos in ("CCONJ", "SCONJ") and c_pos == "Conj":
        score += 10.0
    elif upos == "PART" and c_pos in ("Conj", "Ques", "Adv"):
        score += 8.0
    elif upos == "NUM" and c_pos == "Num":
        score += 10.0
    elif upos == "ADP" and c_pos == "Postp":
        score += 10.0

    # 2. XPOS alignment
    if xpos and xpos != "_" and xpos in zkey:
        score += 5.0

    # 3. Pronoun type (Personal vs Demonstrative)
    pron_type = feats.get("PronType")
    if pron_type == "Prs" and c_sec_pos == "Pers":
        score += 6.0
    elif pron_type == "Dem" and c_sec_pos == "Demons":
        score += 6.0

    # 4. Lemma matching
    c_lemma = tr_lower(cand.get("lemma", ""))
    if c_lemma == lemma:
        score += 15.0
    elif c_lemma in (lemma + "mek", lemma + "mak"):
        score += 15.0
    elif lemma.startswith(c_lemma) or c_lemma.startswith(lemma):
        score += 5.0

    # 5. Case matching
    case = feats.get("Case")
    if case:
        if case == "Nom" and not any(m in morphs for m in ["Acc", "Dat", "Loc", "Abl", "Gen", "Ins"]):
            score += 5.0
        elif case == "Acc" and "Acc" in morphs:
            score += 5.0
        elif case == "Dat" and "Dat" in morphs:
            score += 5.0
        elif case == "Loc" and "Loc" in morphs:
            score += 5.0
        elif case == "Abl" and "Abl" in morphs:
            score += 5.0
        elif case == "Gen" and "Gen" in morphs:
            score += 5.0
        elif case == "Ins" and "Ins" in morphs:
            score += 5.0

    # 6. Number
    num = feats.get("Number")
    if num == "Plur" and any(m in morphs for m in ["A1pl", "A2pl", "A3pl"]):
        score += 3.0
    elif num == "Sing" and any(m in morphs for m in ["A1sg", "A2sg", "A3sg"]):
        score += 3.0

    # 7. Possessive agreement
    psor_p = feats.get("Person[psor]")
    psor_n = feats.get("Number[psor]")
    if psor_p == "1":
        if psor_n == "Plur" and "P1pl" in morphs:
            score += 7.0
        elif "P1sg" in morphs:
            score += 7.0
    elif psor_p == "2":
        if psor_n == "Plur" and "P2pl" in morphs:
            score += 7.0
        elif "P2sg" in morphs:
            score += 7.0
    elif psor_p == "3":
        if psor_n == "Plur" and "P3pl" in morphs:
            score += 7.0
        elif (psor_n == "Sing" or psor_n is None) and "P3sg" in morphs:
            score += 7.0
    elif psor_p is None:
        if not any(m in morphs for m in ["P1sg", "P1pl", "P2sg", "P2pl", "P3sg", "P3pl"]):
            score += 4.0
        else:
            score -= 4.0

    # 8. Verb forms (Participles, Verbal Nouns, Converbs)
    vf = feats.get("VerbForm")
    if vf == "Part":
        if any(m in morphs for m in ["PastPart", "PresPart", "FutPart"]):
            score += 8.0
        if "Adj" in morphs:
            score += 4.0
    elif vf == "Vnoun":
        if any(m in morphs for m in ["Inf1", "Inf2", "Inf3"]):
            score += 8.0
    elif vf == "Conv":
        if any(m in morphs for m in ["When", "While", "After", "Before", "ByDoingSo", "Since", "WithoutHavingDoneSo"]):
            score += 10.0
    elif vf is None and c_pos == "Verb":
        if any(m in morphs for m in ["Aor", "Past", "Prog", "Fut", "Narr", "Opt", "Des", "Nec", "Imp"]):
            score += 6.0
        if any(m in morphs for m in ["PastPart", "PresPart", "FutPart"]):
            score -= 4.0

    # 9. Voice
    voice = feats.get("Voice")
    if voice == "Pass" and "Pass" in morphs:
        score += 4.0
    elif voice == "Caus" and "Caus" in morphs:
        score += 4.0

    # 10. Complexity penalty (Occam's razor: penalize overly derived improbable paths)
    score -= len(cand.get("morphemes", [])) * 0.1

    return score


def align_tokens_dynamic(
    z_tokens: List[Dict[str, Any]], c_tokens: List[Dict[str, Any]]
) -> List[Tuple[Optional[Dict[str, Any]], Optional[Dict[str, Any]]]]:
    """
    Aligns Zemberek tokens and CoNLL-U tokens using dynamic programming
    based on surface string equality.
    """
    n = len(z_tokens)
    m = len(c_tokens)
    dp = [[0] * (m + 1) for _ in range(n + 1)]

    for i in range(1, n + 1):
        for j in range(1, m + 1):
            if z_tokens[i - 1]["surface"] == c_tokens[j - 1]["surface"]:
                dp[i][j] = dp[i - 1][j - 1] + 1
            else:
                dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])

    aligned = []
    i, j = n, m
    while i > 0 or j > 0:
        if i > 0 and j > 0 and z_tokens[i - 1]["surface"] == c_tokens[j - 1]["surface"]:
            aligned.append((z_tokens[i - 1], c_tokens[j - 1]))
            i -= 1
            j -= 1
        elif j > 0 and (i == 0 or dp[i][j - 1] >= dp[i - 1][j]):
            aligned.append((None, c_tokens[j - 1]))
            j -= 1
        else:
            aligned.append((z_tokens[i - 1], None))
            i -= 1

    aligned.reverse()
    return aligned


def align_dataset(
    conllu_path: Path,
    candidates_path: Path,
    output_path: Path,
    meta_path: Optional[Path] = None,
) -> Dict[str, Any]:
    print(f"Loading CoNLL-U sentences from: {conllu_path}")
    conllu_sents = parse_conllu(conllu_path)

    print(f"Loading Zemberek candidates from: {candidates_path}")
    with open(candidates_path, "r", encoding="utf-8") as f:
        zemb_sents = [json.loads(line) for line in f]

    print(f"CoNLL-U sentences: {len(conllu_sents)}, Zemberek sentences: {len(zemb_sents)}")

    total_sentences = 0
    total_tokens = 0
    total_ambiguous = 0
    aligned_ambiguous = 0

    output_records = []

    for s_idx, z_sent in enumerate(zemb_sents):
        c_sent = conllu_sents[s_idx] if s_idx < len(conllu_sents) else None
        if not c_sent:
            continue

        z_tokens = z_sent.get("tokens", [])
        pairs = align_tokens_dynamic(z_tokens, c_sent)

        annotated_tokens = []
        for zt, ct in pairs:
            if zt is None:
                continue

            token_copy = dict(zt)
            total_tokens += 1

            if zt.get("is_ambiguous", False) and zt.get("candidates"):
                total_ambiguous += 1
                if ct is not None:
                    # Score candidates against gold CoNLL-U
                    scores = []
                    for cand in zt["candidates"]:
                        sc = score_candidate(cand, ct)
                        scores.append((sc, cand["id"]))
                    scores.sort(key=lambda x: x[0], reverse=True)
                    best_cand_id = scores[0][1]

                    gold_part = ct["parts"][0]
                    gold_lemma = gold_part[2]
                    gold_upos = gold_part[3]
                    gold_feats = gold_part[5]

                    token_copy["selected_candidate_id"] = best_cand_id
                    token_copy["selection_reasoning"] = (
                        f"Aligned from UD gold annotation: LEMMA={gold_lemma}, "
                        f"UPOS={gold_upos}, FEATS={gold_feats}"
                    )
                    aligned_ambiguous += 1
                else:
                    # Fallback to candidate 0 if token could not be aligned
                    token_copy["selected_candidate_id"] = 0
                    token_copy["selection_reasoning"] = "Fallback candidate 0 (token unaligned in CoNLL-U)"

            annotated_tokens.append(token_copy)

        z_sent["tokens"] = annotated_tokens
        output_records.append(z_sent)
        total_sentences += 1

    # Write annotated JSONL
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        for record in output_records:
            f.write(json.dumps(record, ensure_ascii=False) + "\n")

    print(f"Annotated sentences written: {total_sentences} to {output_path}")
    print(f"Total tokens: {total_tokens}")
    print(f"Total ambiguous tokens: {total_ambiguous}")
    print(f"Successfully aligned ambiguous tokens: {aligned_ambiguous} ({aligned_ambiguous/total_ambiguous*100:.2f}%)")

    # Write metadata
    if meta_path is None:
        meta_path = output_path.with_suffix(".meta.json") if output_path.suffix == ".jsonl" else Path(str(output_path) + ".meta.json")

    metadata = {
        "schema_version": "1.0",
        "dataset_name": "boun_ud_aligned_disambiguation_dataset",
        "provenance": {
            "source_conllu": str(conllu_path),
            "source_candidates": str(candidates_path),
            "aligner": "align_conllu_to_zemberek.py",
            "created_at": datetime.now(timezone.utc).isoformat(),
        },
        "statistics": {
            "sentence_count": total_sentences,
            "token_count": total_tokens,
            "ambiguous_token_count": total_ambiguous,
            "aligned_ambiguous_count": aligned_ambiguous,
            "alignment_rate": round(aligned_ambiguous / total_ambiguous, 4) if total_ambiguous > 0 else 0.0,
        },
        "annotation": {
            "status": "completed",
            "annotator": "UD_Turkish-BOUN (Human gold annotation via automated alignment)",
            "output_file": output_path.name,
        },
    }

    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)

    print(f"Metadata written to: {meta_path}")
    return metadata


def main():
    parser = argparse.ArgumentParser(
        description="Aligns UD CoNLL-U gold annotations with Zemberek candidate analyses."
    )
    parser.add_argument(
        "--conllu",
        "-c",
        required=True,
        type=Path,
        help="Input CoNLL-U file (e.g. tr_boun-ud-test.conllu)",
    )
    parser.add_argument(
        "--candidates",
        "-i",
        required=True,
        type=Path,
        help="Input candidates JSONL file produced by DisambiguationCandidateExtractor",
    )
    parser.add_argument(
        "--output",
        "-o",
        required=True,
        type=Path,
        help="Output annotated JSONL file",
    )
    parser.add_argument(
        "--meta",
        "-m",
        type=Path,
        help="Output metadata file (defaults to <output>.meta.json)",
    )

    args = parser.parse_args()
    align_dataset(args.conllu, args.candidates, args.output, args.meta)


if __name__ == "__main__":
    main()

