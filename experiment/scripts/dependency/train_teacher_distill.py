#!/usr/bin/env python3
"""
BERTurk / ConvBERT-Turkish Deep Biaffine Teacher trainer & unlabeled-corpus labeler
for teacher-student distillation into Zemberek's compact pure-Java `ZDEP` parser.

Two subcommands:
  1. `train`: Fine-tunes a HuggingFace Turkish Transformer + Deep Biaffine parser on
     Zemberek-aligned `UD_Turkish-BOUN` (fp16/bf16, ~10-15 min on Colab T4).
  2. `label`: Runs batched fp16 teacher inference + exact Chu-Liu/Edmonds MST over
     a Zemberek-projected unlabeled CoNLL-U corpus, filtering to high-confidence trees
     for clean BOUN-consistent student distillation.
"""

from __future__ import annotations

import argparse
import math
import random
import time
from pathlib import Path
from typing import Dict, List, Sequence, Tuple

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from transformers import AutoModel, AutoTokenizer

from model_zdep import (
    ConlluSentenceRec,
    decode_single_root_mst,
    read_conllu,
    write_conllu,
)


def set_seed(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


class TransformerBiaffineTeacher(nn.Module):
    def __init__(
        self,
        model_name: str,
        num_labels: int,
        arc_dim: int = 384,
        rel_dim: int = 128,
        dropout: float = 0.3,
    ) -> None:
        super().__init__()
        self.encoder = AutoModel.from_pretrained(model_name)
        hidden = self.encoder.config.hidden_size
        self.drop = nn.Dropout(dropout)

        self.arc_dep_mlp = nn.Linear(hidden, arc_dim)
        self.arc_head_mlp = nn.Linear(hidden, arc_dim)
        self.rel_dep_mlp = nn.Linear(hidden, rel_dim)
        self.rel_head_mlp = nn.Linear(hidden, rel_dim)

        self.arc_biaffine = nn.Parameter(torch.zeros(arc_dim + 1, arc_dim + 1))
        self.rel_biaffine = nn.Parameter(torch.zeros(num_labels, rel_dim + 1, rel_dim + 1))
        nn.init.xavier_uniform_(self.arc_biaffine)
        nn.init.xavier_uniform_(self.rel_biaffine)

    def forward(
        self,
        input_ids: torch.Tensor,
        attention_mask: torch.Tensor,
        word_first_subword: torch.Tensor,  # [B, T_words] index into subword seq (pos 0 = [CLS])
    ) -> Tuple[torch.Tensor, torch.Tensor, torch.Tensor]:
        out = self.encoder(input_ids=input_ids, attention_mask=attention_mask)
        sub_h = out.last_hidden_state  # [B, S, H]
        B, T = word_first_subword.shape
        H = sub_h.shape[-1]

        idx = word_first_subword.clamp(min=0, max=sub_h.shape[1] - 1).unsqueeze(-1).expand(B, T, H)
        word_h = self.drop(torch.gather(sub_h, dim=1, index=idx))  # [B, T, H]

        ones = torch.ones((B, T, 1), device=word_h.device, dtype=word_h.dtype)
        arc_dep = self.drop(F.leaky_relu(self.arc_dep_mlp(word_h), negative_slope=0.1))
        arc_head = self.drop(F.leaky_relu(self.arc_head_mlp(word_h), negative_slope=0.1))
        arc_scores = torch.einsum(
            "bix,xy,bjy->bij",
            torch.cat([arc_dep, ones], dim=-1),
            self.arc_biaffine,
            torch.cat([arc_head, ones], dim=-1),
        )

        rel_dep = self.drop(F.leaky_relu(self.rel_dep_mlp(word_h), negative_slope=0.1))
        rel_head = self.drop(F.leaky_relu(self.rel_head_mlp(word_h), negative_slope=0.1))
        return (
            arc_scores,
            torch.cat([rel_dep, ones], dim=-1),
            torch.cat([rel_head, ones], dim=-1),
        )

    def score_labels(
        self, rel_dep_b: torch.Tensor, rel_head_b: torch.Tensor, heads: torch.Tensor
    ) -> torch.Tensor:
        B, T, D = rel_head_b.shape
        safe = heads.clamp(min=0, max=T - 1).unsqueeze(-1).expand(B, T, D)
        gathered = torch.gather(rel_head_b, dim=1, index=safe)
        return torch.einsum("bix,cxy,biy->bic", rel_dep_b, self.rel_biaffine, gathered)


def tokenize_batch(
    sentences: Sequence[ConlluSentenceRec],
    tokenizer,
    label2id: Dict[str, int],
    max_subwords: int = 384,
) -> Tuple[Dict[str, torch.Tensor], List[int]]:
    """Tokenizes a batch of CoNLL-U sentences, returning valid sentence indices (skipping > max_subwords)."""
    kept_indices: List[int] = []
    batch_sub_ids: List[List[int]] = []
    batch_first_idx: List[List[int]] = []
    batch_heads: List[List[int]] = []
    batch_rels: List[List[int]] = []

    cls_id = tokenizer.cls_token_id
    sep_id = tokenizer.sep_token_id
    unk_id = tokenizer.unk_token_id

    for si, sent in enumerate(sentences):
        sub_ids = [cls_id]
        first_idx = [0]  # word 0 = <ROOT> maps to [CLS]
        heads = [-1]
        rels = [-1]
        overflow = False
        for tok in sent.tokens:
            pieces = tokenizer.encode(tok.form, add_special_tokens=False)
            if not pieces:
                pieces = [unk_id]
            if len(sub_ids) + len(pieces) + 1 > max_subwords:
                overflow = True
                break
            first_idx.append(len(sub_ids))
            sub_ids.extend(pieces)
            heads.append(tok.head)
            rels.append(label2id.get(tok.deprel, -1))
        if overflow or len(first_idx) <= 1:
            continue
        sub_ids.append(sep_id)
        kept_indices.append(si)
        batch_sub_ids.append(sub_ids)
        batch_first_idx.append(first_idx)
        batch_heads.append(heads)
        batch_rels.append(rels)

    if not kept_indices:
        return {}, []

    B = len(kept_indices)
    S = max(len(x) for x in batch_sub_ids)
    T = max(len(x) for x in batch_first_idx)

    pad_id = tokenizer.pad_token_id if tokenizer.pad_token_id is not None else 0
    input_ids = torch.full((B, S), pad_id, dtype=torch.long)
    attn_mask = torch.zeros((B, S), dtype=torch.long)
    first_sw = torch.zeros((B, T), dtype=torch.long)
    head_t = torch.full((B, T), -1, dtype=torch.long)
    rel_t = torch.full((B, T), -1, dtype=torch.long)
    len_t = torch.zeros(B, dtype=torch.long)

    for b in range(B):
        s_len = len(batch_sub_ids[b])
        t_len = len(batch_first_idx[b])
        input_ids[b, :s_len] = torch.tensor(batch_sub_ids[b], dtype=torch.long)
        attn_mask[b, :s_len] = 1
        first_sw[b, :t_len] = torch.tensor(batch_first_idx[b], dtype=torch.long)
        head_t[b, :t_len] = torch.tensor(batch_heads[b], dtype=torch.long)
        rel_t[b, :t_len] = torch.tensor(batch_rels[b], dtype=torch.long)
        len_t[b] = t_len

    return {
        "input_ids": input_ids,
        "attention_mask": attn_mask,
        "first_sw": first_sw,
        "heads": head_t,
        "rels": rel_t,
        "lengths": len_t,
    }, kept_indices


@torch.no_grad()
def predict_batch(
    model: TransformerBiaffineTeacher,
    batch: Dict[str, torch.Tensor],
    labels: Sequence[str],
    root_label_id: int,
    device: torch.device,
) -> Tuple[List[List[int]], List[List[str]], List[float]]:
    model.eval()
    b_dev = {k: v.to(device) for k, v in batch.items()}
    with torch.amp.autocast("cuda", enabled=(device.type == "cuda")):
        arc_scores, rel_dep_b, rel_head_b = model(
            b_dev["input_ids"], b_dev["attention_mask"], b_dev["first_sw"]
        )

    B, T, _ = arc_scores.shape
    lengths = batch["lengths"].numpy()
    arc_fp32 = arc_scores.float()
    pos = torch.arange(T, device=device).unsqueeze(0)
    valid_head = pos < b_dev["lengths"].unsqueeze(1)
    arc_masked = arc_fp32.masked_fill(~valid_head.unsqueeze(1), -1e9)
    eye = torch.eye(T, device=device, dtype=torch.bool).unsqueeze(0)
    arc_masked = arc_masked.masked_fill(eye, -1e9)
    arc_probs = F.softmax(arc_masked, dim=-1).cpu().numpy()
    arc_np = arc_fp32.cpu().numpy()

    mst_heads_t = torch.zeros((B, T), device=device, dtype=torch.long)
    decoded_heads: List[np.ndarray] = []
    mean_confs: List[float] = []

    for bi in range(B):
        L = int(lengths[bi])
        mat = arc_np[bi, :L, :L].astype(np.float64)
        np.fill_diagonal(mat, -1e9)
        heads = decode_single_root_mst(mat)
        decoded_heads.append(heads)
        mst_heads_t[bi, 1:L] = torch.tensor(heads[1:L], device=device, dtype=torch.long)
        confs = [float(arc_probs[bi, dep, heads[dep]]) for dep in range(1, L)]
        mean_confs.append(float(np.mean(confs)) if confs else 0.0)

    with torch.amp.autocast("cuda", enabled=(device.type == "cuda")):
        rel_logits = model.score_labels(rel_dep_b, rel_head_b, mst_heads_t).float().cpu().numpy()

    out_heads: List[List[int]] = []
    out_rels: List[List[str]] = []
    for bi in range(B):
        L = int(lengths[bi])
        heads = decoded_heads[bi]
        s_h: List[int] = []
        s_r: List[str] = []
        for dep in range(1, L):
            h = int(heads[dep])
            if h == 0 and root_label_id >= 0:
                r_id = root_label_id
            else:
                logits = rel_logits[bi, dep].copy()
                if root_label_id >= 0:
                    logits[root_label_id] = -1e9
                r_id = int(np.argmax(logits))
            s_h.append(h)
            s_r.append(labels[r_id])
        out_heads.append(s_h)
        out_rels.append(s_r)
    return out_heads, out_rels, mean_confs


def evaluate_teacher(
    model: TransformerBiaffineTeacher,
    sentences: Sequence[ConlluSentenceRec],
    tokenizer,
    labels: Sequence[str],
    label2id: Dict[str, int],
    device: torch.device,
    batch_size: int = 32,
) -> Tuple[float, float]:
    root_id = label2id.get("root", -1)
    toks = uas = las = 0
    for start in range(0, len(sentences), batch_size):
        chunk = sentences[start : start + batch_size]
        batch, kept = tokenize_batch(chunk, tokenizer, label2id)
        if not kept:
            continue
        p_heads, p_rels, _ = predict_batch(model, batch, labels, root_id, device)
        for bi, orig_idx in enumerate(kept):
            sent = chunk[orig_idx]
            for ti, tok in enumerate(sent.tokens):
                toks += 1
                if tok.head == p_heads[bi][ti]:
                    uas += 1
                    if tok.deprel == p_rels[bi][ti]:
                        las += 1
    return 100.0 * uas / max(1, toks), 100.0 * las / max(1, toks)


def cmd_train(args: argparse.Namespace) -> None:
    set_seed(args.seed)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    train_sents = read_conllu(args.train)
    dev_sents = read_conllu(args.dev)
    test_sents = read_conllu(args.test) if args.test else []

    from collections import Counter

    label_c = Counter(t.deprel for s in train_sents for t in s.tokens if t.deprel != "_")
    labels = [l for l, _ in label_c.most_common()]
    if "root" in labels:
        labels.remove("root")
        labels.insert(0, "root")
    label2id = {l: i for i, l in enumerate(labels)}

    tokenizer = AutoTokenizer.from_pretrained(args.backbone)
    model = TransformerBiaffineTeacher(args.backbone, len(labels)).to(device)

    enc_params = list(model.encoder.parameters())
    head_params = [p for n, p in model.named_parameters() if not n.startswith("encoder.")]
    optimizer = torch.optim.AdamW(
        [
            {"params": enc_params, "lr": args.encoder_lr, "weight_decay": 0.01},
            {"params": head_params, "lr": args.head_lr, "weight_decay": 0.01},
        ]
    )
    steps_per_ep = max(1, math.ceil(len(train_sents) / args.batch_size))
    total_steps = args.epochs * steps_per_ep
    warmup = max(1, int(0.1 * total_steps))

    def lr_fn(step: int) -> float:
        if step < warmup:
            return float(step + 1) / float(warmup)
        prog = (step - warmup) / max(1, total_steps - warmup)
        return max(0.02, 0.5 * (1.0 + math.cos(math.pi * prog)))

    scheduler = torch.optim.lr_scheduler.LambdaLR(optimizer, lr_fn)
    scaler = torch.amp.GradScaler("cuda", enabled=(device.type == "cuda"))

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    best_las = -1.0

    for epoch in range(1, args.epochs + 1):
        t0 = time.time()
        model.train()
        order = list(train_sents)
        random.shuffle(order)
        total_loss = 0.0
        n_steps = 0

        for start in range(0, len(order), args.batch_size):
            chunk = order[start : start + args.batch_size]
            batch, kept = tokenize_batch(chunk, tokenizer, label2id)
            if not kept:
                continue
            b_dev = {k: v.to(device) for k, v in batch.items()}
            optimizer.zero_grad(set_to_none=True)

            with torch.amp.autocast("cuda", enabled=(device.type == "cuda")):
                arc_scores, rel_dep_b, rel_head_b = model(
                    b_dev["input_ids"], b_dev["attention_mask"], b_dev["first_sw"]
                )
                B, T, _ = arc_scores.shape
                pos = torch.arange(T, device=device).unsqueeze(0)
                valid_head = pos < b_dev["lengths"].unsqueeze(1)
                arc_scores = arc_scores.masked_fill(~valid_head.unsqueeze(1), -1e4)
                eye = torch.eye(T, device=device, dtype=torch.bool).unsqueeze(0)
                arc_scores = arc_scores.masked_fill(eye, -1e4)

                g_heads = b_dev["heads"]
                g_rels = b_dev["rels"]
                arc_loss = F.cross_entropy(arc_scores[g_heads >= 0], g_heads[g_heads >= 0])
                rel_logits = model.score_labels(rel_dep_b, rel_head_b, g_heads)
                rel_loss = F.cross_entropy(rel_logits[g_rels >= 0], g_rels[g_rels >= 0])
                loss = arc_loss + rel_loss

            scaler.scale(loss).backward()
            scaler.unscale_(optimizer)
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            scaler.step(optimizer)
            scaler.update()
            scheduler.step()

            total_loss += float(loss.item())
            n_steps += 1

        dev_uas, dev_las = evaluate_teacher(model, dev_sents, tokenizer, labels, label2id, device)
        dt = time.time() - t0
        tag = ""
        if dev_las > best_las:
            best_las = dev_las
            torch.save(
                {"state_dict": model.state_dict(), "backbone": args.backbone, "labels": labels},
                out_path,
            )
            tag = "  <-- BEST"
        print(
            f"[teacher] Epoch {epoch:02d}/{args.epochs:02d} | loss={total_loss / max(1, n_steps):.4f} | "
            f"dev UAS={dev_uas:.2f}% LAS={dev_las:.2f}% | {dt:.1f}s{tag}"
        )

    if test_sents:
        ckpt = torch.load(out_path, map_location=device, weights_only=False)
        model.load_state_dict(ckpt["state_dict"])
        t_uas, t_las = evaluate_teacher(model, test_sents, tokenizer, labels, label2id, device)
        print(f"[teacher best test] UAS={t_uas:.2f}% | LAS={t_las:.2f}%")


def cmd_label(args: argparse.Namespace) -> None:
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    ckpt = torch.load(args.checkpoint, map_location=device, weights_only=False)
    backbone = ckpt["backbone"]
    labels: List[str] = ckpt["labels"]
    label2id = {l: i for i, l in enumerate(labels)}
    root_id = label2id.get("root", -1)

    tokenizer = AutoTokenizer.from_pretrained(backbone)
    model = TransformerBiaffineTeacher(backbone, len(labels)).to(device)
    model.load_state_dict(ckpt["state_dict"])
    model.eval()

    unlabeled = read_conllu(args.input)
    kept_sents: List[ConlluSentenceRec] = []
    kept_heads: List[List[int]] = []
    kept_rels: List[List[str]] = []

    t0 = time.time()
    for start in range(0, len(unlabeled), args.batch_size):
        chunk = unlabeled[start : start + args.batch_size]
        batch, kept = tokenize_batch(chunk, tokenizer, label2id)
        if not kept:
            continue
        p_heads, p_rels, confs = predict_batch(model, batch, labels, root_id, device)
        for bi, orig_idx in enumerate(kept):
            sent = chunk[orig_idx]
            if len(sent.tokens) < args.min_tokens or len(sent.tokens) > args.max_tokens:
                continue
            if confs[bi] >= args.min_conf:
                kept_sents.append(sent)
                kept_heads.append(p_heads[bi])
                kept_rels.append(p_rels[bi])
        if (start // args.batch_size) % 100 == 0:
            print(
                f"[label] processed {min(start + args.batch_size, len(unlabeled))}/{len(unlabeled)} "
                f"| kept={len(kept_sents)} ({time.time() - t0:.1f}s)"
            )

    write_conllu(args.output, kept_sents, kept_heads, kept_rels)
    print(
        f"[label] Done: kept {len(kept_sents)}/{len(unlabeled)} sentences "
        f"(min_conf={args.min_conf}) -> {args.output}"
    )


def main() -> None:
    ap = argparse.ArgumentParser(description="BERTurk/ConvBERT Biaffine Teacher for ZDEP Distillation")
    sub = ap.add_subparsers(dest="cmd", required=True)

    p_tr = sub.add_parser("train", help="Train Transformer Biaffine teacher")
    p_tr.add_argument("--train", required=True)
    p_tr.add_argument("--dev", required=True)
    p_tr.add_argument("--test", default=None)
    p_tr.add_argument("--backbone", default="dbmdz/bert-base-turkish-cased")
    p_tr.add_argument("--epochs", type=int, default=15)
    p_tr.add_argument("--batch-size", type=int, default=24)
    p_tr.add_argument("--encoder-lr", type=float, default=4e-5)
    p_tr.add_argument("--head-lr", type=float, default=1e-3)
    p_tr.add_argument("--seed", type=int, default=42)
    p_tr.add_argument("--out", required=True)

    p_lb = sub.add_parser("label", help="Label Zemberek-projected unlabeled CoNLL-U with trained teacher")
    p_lb.add_argument("--checkpoint", required=True)
    p_lb.add_argument("--input", required=True)
    p_lb.add_argument("--output", required=True)
    p_lb.add_argument("--batch-size", type=int, default=64)
    p_lb.add_argument("--min-conf", type=float, default=0.85)
    p_lb.add_argument("--min-tokens", type=int, default=4)
    p_lb.add_argument("--max-tokens", type=int, default=60)

    args = ap.parse_args()
    if args.cmd == "train":
        cmd_train(args)
    else:
        cmd_label(args)


if __name__ == "__main__":
    main()
