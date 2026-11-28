#!/usr/bin/env python3
"""
Trains a compact ZDEP v1 Turkish dependency parser (BiGRU + Deep Biaffine) on
Zemberek-aligned UD_Turkish-BOUN data, optionally mixing teacher-distilled silver
trees and pretrained Turkish FastText lemma embeddings, and exports both FP32 and
INT8 `.bin.gz` models for `zemberek-dependency`.
"""

from __future__ import annotations

import argparse
import math
import random
import time
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple

import numpy as np
import torch
import torch.nn.functional as F

from model_zdep import (
    PRESETS,
    ZdepBiaffineParser,
    ZdepVocabs,
    collate_batch,
    decode_single_root_mst,
    encode_sentence,
    export_zdep,
    read_conllu,
    tr_lower,
    write_conllu,
)


def set_seed(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


def load_fasttext_vec(
    vec_path: str | Path, vocabs: ZdepVocabs, lemma_dim: int
) -> Optional[np.ndarray]:
    """Loads pretrained FastText `.vec` word vectors for known lemmas (projects via SVD if needed)."""
    needed = set(vocabs.lemmas[3:])
    found: Dict[str, np.ndarray] = {}
    src_dim = 0
    with open(vec_path, "r", encoding="utf-8", errors="ignore") as f:
        header = f.readline().strip().split()
        if len(header) == 2 and header[0].isdigit():
            src_dim = int(header[1])
        else:
            f.seek(0)
        for line in f:
            parts = line.rstrip().split(" ")
            if len(parts) < 10:
                continue
            w = tr_lower(parts[0])
            if w in needed and w not in found:
                vec = np.asarray(parts[1:], dtype=np.float32)
                if src_dim == 0:
                    src_dim = vec.size
                if vec.size == src_dim:
                    found[w] = vec

    if not found:
        print(f"[warn] No overlapping lemmas found in {vec_path}")
        return None

    print(f"[info] Loaded {len(found)}/{len(needed)} pretrained lemma vectors (src_dim={src_dim})")
    keys = list(found.keys())
    mat = np.stack([found[k] for k in keys], axis=0)
    if src_dim != lemma_dim:
        # Mean-center and project via truncated SVD to match lemma_dim cleanly
        mean = mat.mean(axis=0, keepdims=True)
        centered = mat - mean
        _, _, vt = np.linalg.svd(centered, full_matrices=False)
        proj = centered @ vt[:lemma_dim].T
        proj = proj / (np.std(proj) + 1e-6) * 0.15
    else:
        proj = mat / (np.std(mat) + 1e-6) * 0.15

    init = np.random.normal(0.0, 0.1, size=(len(vocabs.lemmas), lemma_dim)).astype(np.float32)
    init[0] = 0.0
    for idx, k in enumerate(keys):
        init[vocabs.lemma2id[k]] = proj[idx]
    return init


@torch.no_grad()
def evaluate(
    model: ZdepBiaffineParser,
    sentences: Sequence,
    encoded: Sequence[Dict[str, object]],
    device: torch.device,
    batch_size: int = 64,
    return_preds: bool = False,
) -> Tuple[float, float, float, Optional[List[List[int]]], Optional[List[List[str]]]]:
    model.eval()
    vocabs = model.vocabs
    max_k = max(model.cfg.char_kernel_sizes)

    total_toks = 0
    uas_ok = 0
    las_ok = 0
    exact_ok = 0
    all_heads: List[List[int]] = []
    all_rels: List[List[str]] = []

    for start in range(0, len(encoded), batch_size):
        chunk = encoded[start : start + batch_size]
        batch = collate_batch(chunk, max_kernel=max_k, training=False)
        batch_dev = {k: v.to(device) for k, v in batch.items()}
        arc_scores, rel_dep_b, rel_head_b = model(
            batch_dev["lemma_ids"],
            batch_dev["char_ids"],
            batch_dev["char_lens"],
            batch_dev["upos_ids"],
            batch_dev["xpos_ids"],
            batch_dev["feat_ids"],
            batch_dev["feat_counts"],
            batch_dev["lengths"],
        )
        arc_np = arc_scores.cpu().numpy()
        lengths = batch["lengths"].numpy()

        mst_heads_batch = torch.zeros_like(batch_dev["heads"])
        decoded_list: List[np.ndarray] = []
        for bi in range(len(chunk)):
            L = int(lengths[bi])
            scores = arc_np[bi, :L, :L].astype(np.float64)
            np.fill_diagonal(scores, -1e9)
            heads = decode_single_root_mst(scores)
            decoded_list.append(heads)
            mst_heads_batch[bi, 1:L] = torch.tensor(heads[1:L], device=device, dtype=torch.long)

        rel_logits = model.score_labels_for_heads(rel_dep_b, rel_head_b, mst_heads_batch).cpu().numpy()

        for bi in range(len(chunk)):
            L = int(lengths[bi])
            sent = sentences[start + bi]
            heads = decoded_list[bi]
            sent_heads: List[int] = []
            sent_rels: List[str] = []
            sent_exact = True

            for dep in range(1, L):
                h = int(heads[dep])
                if h == 0 and vocabs.root_label_id >= 0:
                    r_id = vocabs.root_label_id
                else:
                    logits = rel_logits[bi, dep].copy()
                    if vocabs.root_label_id >= 0:
                        logits[vocabs.root_label_id] = -1e9
                    r_id = int(np.argmax(logits))
                r_str = vocabs.labels[r_id]
                sent_heads.append(h)
                sent_rels.append(r_str)

                g_tok = sent.tokens[dep - 1]
                total_toks += 1
                if g_tok.head == h:
                    uas_ok += 1
                    if g_tok.deprel == r_str:
                        las_ok += 1
                    else:
                        sent_exact = False
                else:
                    sent_exact = False

            if sent_exact:
                exact_ok += 1
            if return_preds:
                all_heads.append(sent_heads)
                all_rels.append(sent_rels)

    uas = 100.0 * uas_ok / max(1, total_toks)
    las = 100.0 * las_ok / max(1, total_toks)
    exact = 100.0 * exact_ok / max(1, len(sentences))
    return uas, las, exact, (all_heads if return_preds else None), (all_rels if return_preds else None)


def compute_loss(
    model: ZdepBiaffineParser,
    batch: Dict[str, torch.Tensor],
    label_smoothing: float = 0.05,
) -> torch.Tensor:
    arc_scores, rel_dep_b, rel_head_b = model(
        batch["lemma_ids"],
        batch["char_ids"],
        batch["char_lens"],
        batch["upos_ids"],
        batch["xpos_ids"],
        batch["feat_ids"],
        batch["feat_counts"],
        batch["lengths"],
    )
    B, T, _ = arc_scores.shape
    device = arc_scores.device
    lengths = batch["lengths"]

    # Mask padded head positions and self-loops
    pos = torch.arange(T, device=device).unsqueeze(0)  # [1, T]
    valid_head = pos < lengths.unsqueeze(1)  # [B, T]
    arc_scores = arc_scores.masked_fill(~valid_head.unsqueeze(1), -1e9)
    eye = torch.eye(T, device=device, dtype=torch.bool).unsqueeze(0)
    arc_scores = arc_scores.masked_fill(eye, -1e9)

    gold_heads = batch["heads"]  # [B, T], -1 for ROOT & padded
    gold_rels = batch["rels"]

    arc_mask = gold_heads >= 0
    arc_loss = F.cross_entropy(
        arc_scores[arc_mask], gold_heads[arc_mask], label_smoothing=label_smoothing
    )

    rel_logits = model.score_labels_for_heads(rel_dep_b, rel_head_b, gold_heads)
    rel_mask = gold_rels >= 0
    rel_loss = F.cross_entropy(
        rel_logits[rel_mask], gold_rels[rel_mask], label_smoothing=label_smoothing
    )
    return arc_loss + rel_loss


def main() -> None:
    ap = argparse.ArgumentParser(description="Train compact ZDEP Turkish dependency parser")
    ap.add_argument("--train", required=True, help="Zemberek-aligned gold training CoNLL-U")
    ap.add_argument("--dev", required=True, help="Zemberek-aligned dev CoNLL-U")
    ap.add_argument("--test", default=None, help="Optional Zemberek-aligned test CoNLL-U")
    ap.add_argument("--silver", default=None, help="Optional teacher-labeled distillation CoNLL-U")
    ap.add_argument("--silver-weight", type=float, default=0.6, help="Loss weight on silver batches")
    ap.add_argument("--gold-repeat", type=int, default=3, help="Gold upsampling factor when silver is used")
    ap.add_argument("--embeddings", default=None, help="Optional FastText `.vec` file for lemma init")
    ap.add_argument("--preset", choices=list(PRESETS.keys()), default="medium")
    ap.add_argument("--epochs", type=int, default=35)
    ap.add_argument("--batch-size", type=int, default=48)
    ap.add_argument("--lr", type=float, default=2e-3)
    ap.add_argument("--weight-decay", type=float, default=1e-2)
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--out-dir", required=True, help="Output directory for checkpoints & exported ZDEP models")
    args = ap.parse_args()

    set_seed(args.seed)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"[info] Device: {device} | Preset: {args.preset}")

    train_sents = read_conllu(args.train)
    dev_sents = read_conllu(args.dev)
    test_sents = read_conllu(args.test) if args.test else []
    silver_sents = read_conllu(args.silver) if args.silver else []

    print(
        f"[info] Loaded train={len(train_sents)}, dev={len(dev_sents)}, "
        f"test={len(test_sents)}, silver={len(silver_sents)}"
    )

    cfg, max_lemmas = PRESETS[args.preset]
    vocabs = ZdepVocabs.build(
        train_sents, max_lemmas=max_lemmas, extra_sentences=silver_sents if silver_sents else None
    )
    print(
        f"[info] Vocabs: lemmas={len(vocabs.lemmas)}, chars={len(vocabs.chars)}, "
        f"upos={len(vocabs.upos)}, xpos={len(vocabs.xpos)}, "
        f"feats={len(vocabs.feats)}, labels={len(vocabs.labels)}"
    )

    model = ZdepBiaffineParser(cfg, vocabs).to(device)
    if args.embeddings:
        init_emb = load_fasttext_vec(args.embeddings, vocabs, cfg.lemma_dim)
        if init_emb is not None:
            with torch.no_grad():
                model.lemma_emb.weight.copy_(torch.from_numpy(init_emb).to(device))

    total_params = sum(p.numel() for p in model.parameters())
    print(f"[info] Model parameters: {total_params:,} (~{total_params / 1e6:.2f}M)")

    enc_train = [encode_sentence(s, vocabs) for s in train_sents]
    enc_dev = [encode_sentence(s, vocabs) for s in dev_sents]
    enc_test = [encode_sentence(s, vocabs) for s in test_sents]
    enc_silver = [encode_sentence(s, vocabs) for s in silver_sents]

    # Build combined training schedule (gold upsampled when silver is present)
    gold_items = [(item, 1.0) for item in enc_train]
    if enc_silver:
        train_pool = gold_items * max(1, args.gold_repeat) + [
            (item, args.silver_weight) for item in enc_silver
        ]
    else:
        train_pool = gold_items

    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=args.weight_decay)
    steps_per_epoch = max(1, math.ceil(len(train_pool) / args.batch_size))
    total_steps = args.epochs * steps_per_epoch
    warmup_steps = max(1, int(0.06 * total_steps))

    def lr_lambda(step: int) -> float:
        if step < warmup_steps:
            return float(step + 1) / float(warmup_steps)
        progress = (step - warmup_steps) / max(1, total_steps - warmup_steps)
        return 0.05 + 0.95 * 0.5 * (1.0 + math.cos(math.pi * progress))

    scheduler = torch.optim.lr_scheduler.LambdaLR(optimizer, lr_lambda)
    max_k = max(cfg.char_kernel_sizes)

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    best_ckpt = out_dir / "best_student.pt"
    best_las = -1.0

    for epoch in range(1, args.epochs + 1):
        t0 = time.time()
        model.train()
        random.shuffle(train_pool)
        running_loss = 0.0
        n_batches = 0

        for start in range(0, len(train_pool), args.batch_size):
            slice_pool = train_pool[start : start + args.batch_size]
            items = [p[0] for p in slice_pool]
            batch_weight = float(np.mean([p[1] for p in slice_pool]))
            batch = collate_batch(items, max_kernel=max_k, word_dropout=cfg.word_dropout, training=True)
            batch = {k: v.to(device) for k, v in batch.items()}

            optimizer.zero_grad(set_to_none=True)
            loss = compute_loss(model, batch) * batch_weight
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 5.0)
            optimizer.step()
            scheduler.step()

            running_loss += float(loss.item())
            n_batches += 1

        dev_uas, dev_las, dev_exact, _, _ = evaluate(model, dev_sents, enc_dev, device)
        dt = time.time() - t0
        marker = ""
        if dev_las > best_las:
            best_las = dev_las
            torch.save({"state_dict": model.state_dict(), "cfg": cfg, "vocabs": vocabs}, best_ckpt)
            marker = "  <-- BEST"
        print(
            f"Epoch {epoch:02d}/{args.epochs:02d} | loss={running_loss / max(1, n_batches):.4f} | "
            f"dev UAS={dev_uas:.2f}% LAS={dev_las:.2f}% Exact={dev_exact:.2f}% | {dt:.1f}s{marker}"
        )

    # Load best checkpoint, evaluate on dev & test, export FP32 + INT8 ZDEP models
    ckpt = torch.load(best_ckpt, map_location=device, weights_only=False)
    model.load_state_dict(ckpt["state_dict"])

    dev_uas, dev_las, dev_exact, dev_heads, dev_rels = evaluate(
        model, dev_sents, enc_dev, device, return_preds=True
    )
    print(f"\n[best dev]  UAS={dev_uas:.2f}% | LAS={dev_las:.2f}% | Exact={dev_exact:.2f}%")
    write_conllu(out_dir / "pytorch_dev_preds.conllu", dev_sents, dev_heads, dev_rels)

    if test_sents:
        test_uas, test_las, test_exact, test_heads, test_rels = evaluate(
            model, test_sents, enc_test, device, return_preds=True
        )
        print(f"[best test] UAS={test_uas:.2f}% | LAS={test_las:.2f}% | Exact={test_exact:.2f}%")
        write_conllu(out_dir / "pytorch_test_preds.conllu", test_sents, test_heads, test_rels)

    fp32_path = export_zdep(model, out_dir / f"dep_boun_{args.preset}_fp32.bin.gz", quantize_int8=False)
    int8_path = export_zdep(model, out_dir / f"dep_boun_{args.preset}_int8.bin.gz", quantize_int8=True)
    print(f"[export] FP32: {fp32_path} ({fp32_path.stat().st_size / (1024 * 1024):.2f} MB)")
    print(f"[export] INT8: {int8_path} ({int8_path.stat().st_size / (1024 * 1024):.2f} MB)")


if __name__ == "__main__":
    main()
