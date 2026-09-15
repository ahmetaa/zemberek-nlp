#!/usr/bin/env python3
"""
Converts LLM-annotated JSONL dataset into Zemberek's native training text format.

In the Zemberek format, sentences begin with 'S:<sentence>' followed by each token
and its candidate analyses, with an asterisk '*' marking the correct/selected parse.
"""

import sys
import json
import argparse
from typing import Dict, Any


def convert_jsonl_to_zemberek_format(input_jsonl_path: str, output_txt_path: str) -> int:
    sentence_count = 0
    token_count = 0

    with open(input_jsonl_path, "r", encoding="utf-8") as in_f, \
         open(output_txt_path, "w", encoding="utf-8") as out_f:

        for line in in_f:
            line = line.strip()
            if not line:
                continue

            record = json.loads(line)
            sentence_text = record.get("text", "").strip()
            if not sentence_text:
                continue

            tokens = record.get("tokens", [])
            if not tokens:
                continue

            out_f.write(f"S:{sentence_text}\n")
            sentence_count += 1

            for token in tokens:
                surface = token.get("surface", "")
                out_f.write(f"{surface}\n")
                token_count += 1

                candidates = token.get("candidates", [])
                selected_id = token.get("selected_candidate_id", 0)

                # If no candidates, output dummy
                if not candidates:
                    continue

                is_ambiguous = token.get("is_ambiguous", len(candidates) > 1)
                for c in candidates:
                    key = c.get("zemberek_key", "")
                    if not key:
                        continue
                    cid = c.get("id", 0)
                    star = "*" if (is_ambiguous and cid == selected_id) else ""
                    out_f.write(f"{key}{star}\n")

    print(f"[DONE] Converted {sentence_count} sentences and {token_count} tokens to: {output_txt_path}")
    return sentence_count


def main():
    parser = argparse.ArgumentParser(
        description="Convert annotated JSONL dataset to Zemberek Perceptron training text format.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    parser.add_argument("-i", "--input", required=True, help="Input annotated JSONL file")
    parser.add_argument("-o", "--output", required=True, help="Output Zemberek training .txt file")

    args = parser.parse_args()
    convert_jsonl_to_zemberek_format(args.input, args.output)


if __name__ == "__main__":
    main()
