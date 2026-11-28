"""
PyTorch implementation of the ZDEP v1 compact Turkish dependency parser matching
`zemberek.dependency.BiaffineDependencyModel` bit-for-bit, plus CoNLL-U I/O,
exact single-root Chu-Liu/Edmonds MST decoding, and FP32/INT8 binary export.
"""

from __future__ import annotations

import gzip
import math
import struct
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F

ZDEP_MAGIC = 0x5A444550  # "ZDEP"
ZDEP_VERSION = 1

PAD_ID = 0
ROOT_ID = 1
UNK_ID = 2

CHAR_PAD_ID = 0
CHAR_BOS_ID = 1
CHAR_EOS_ID = 2
CHAR_UNK_ID = 3
MAX_WORD_CHARS = 32

DTYPE_FP32 = 0
DTYPE_INT8 = 1


def tr_lower(s: str) -> str:
    """Turkish-aware lowercase matching Java's `String.toLowerCase(Turkish.LOCALE)`."""
    return s.replace("İ", "i").replace("I", "ı").lower()


@dataclass
class ConlluTokenRec:
    id: int
    form: str
    lemma: str
    upos: str
    xpos: str
    feats: str
    head: int
    deprel: str
    deps: str = "_"
    misc: str = "_"

    def feat_pairs(self) -> List[str]:
        if not self.feats or self.feats == "_":
            return []
        return self.feats.split("|")


@dataclass
class ConlluSentenceRec:
    sent_id: Optional[str]
    text: Optional[str]
    tokens: List[ConlluTokenRec]
    mwt_lines: Dict[int, str] = field(default_factory=dict)  # start_id -> raw MWT line


def read_conllu(path: str | Path) -> List[ConlluSentenceRec]:
    """Reads a UD v2 CoNLL-U file, preserving comments and MWT lines, skipping empty nodes."""
    sentences: List[ConlluSentenceRec] = []
    sent_id: Optional[str] = None
    text: Optional[str] = None
    tokens: List[ConlluTokenRec] = []
    mwt_lines: Dict[int, str] = {}

    with open(path, "r", encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()
            if not line:
                if tokens:
                    sentences.append(ConlluSentenceRec(sent_id, text, tokens, mwt_lines))
                    sent_id, text, tokens, mwt_lines = None, None, [], {}
                continue
            if line.startswith("#"):
                if line.startswith("# sent_id ="):
                    sent_id = line[len("# sent_id =") :].strip()
                elif line.startswith("# text ="):
                    text = line[len("# text =") :].strip()
                continue
            cols = line.split("\t")
            if len(cols) != 10:
                continue
            id_col = cols[0]
            if "." in id_col:
                continue
            if "-" in id_col:
                start_id = int(id_col.split("-")[0])
                mwt_lines[start_id] = line
                continue
            head = -1 if cols[6] == "_" else int(cols[6])
            tokens.append(
                ConlluTokenRec(
                    id=int(id_col),
                    form=cols[1],
                    lemma=cols[2],
                    upos=cols[3],
                    xpos=cols[4],
                    feats=cols[5],
                    head=head,
                    deprel=cols[7],
                    deps=cols[8],
                    misc=cols[9],
                )
            )
    if tokens:
        sentences.append(ConlluSentenceRec(sent_id, text, tokens, mwt_lines))
    return sentences


def write_conllu(
    path: str | Path,
    sentences: Sequence[ConlluSentenceRec],
    pred_heads: Optional[Sequence[Sequence[int]]] = None,
    pred_rels: Optional[Sequence[Sequence[str]]] = None,
) -> None:
    """Writes sentences in standard 10-column CoNLL-U v2 format."""
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        for idx, sent in enumerate(sentences):
            if idx > 0:
                f.write("\n")
            if sent.sent_id:
                f.write(f"# sent_id = {sent.sent_id}\n")
            if sent.text:
                f.write(f"# text = {sent.text}\n")
            heads = pred_heads[idx] if pred_heads is not None else None
            rels = pred_rels[idx] if pred_rels is not None else None
            for ti, tok in enumerate(sent.tokens):
                if tok.id in sent.mwt_lines:
                    f.write(sent.mwt_lines[tok.id] + "\n")
                h = heads[ti] if heads is not None else tok.head
                r = rels[ti] if rels is not None else tok.deprel
                h_str = "_" if h < 0 else str(h)
                f.write(
                    f"{tok.id}\t{tok.form}\t{tok.lemma}\t{tok.upos}\t{tok.xpos}\t"
                    f"{tok.feats}\t{h_str}\t{r}\t{tok.deps}\t{tok.misc}\n"
                )


# ==================== Exact Single-Root Chu-Liu / Edmonds MST ====================


def _cle_arborescence(scores: np.ndarray) -> np.ndarray:
    """Standard Chu-Liu/Edmonds maximum spanning arborescence rooted at 0."""
    n = scores.shape[0]
    best_head = np.full(n, -1, dtype=np.int64)
    for dep in range(1, n):
        row = scores[dep].copy()
        row[dep] = -1e18
        best_head[dep] = int(np.argmax(row))

    visited = np.zeros(n, dtype=np.int64)
    visited[0] = -1
    in_cycle = np.zeros(n, dtype=bool)
    cycle_found = False

    for start in range(1, n):
        if visited[start] != 0:
            continue
        cur = start
        while cur != 0 and visited[cur] == 0:
            visited[cur] = start
            cur = int(best_head[cur])
        if cur != 0 and visited[cur] == start:
            node = cur
            while True:
                in_cycle[node] = True
                node = int(best_head[node])
                if node == cur:
                    break
            cycle_found = True
            break
        walk = start
        while walk != 0 and visited[walk] == start:
            visited[walk] = -1
            walk = int(best_head[walk])

    if not cycle_found:
        return best_head

    comp = np.zeros(n, dtype=np.int64)
    orig_of_non_cycle = [0]
    next_id = 1
    for v in range(1, n):
        if not in_cycle[v]:
            comp[v] = next_id
            orig_of_non_cycle.append(v)
            next_id += 1
    cycle_id = next_id
    next_id += 1
    for v in range(1, n):
        if in_cycle[v]:
            comp[v] = cycle_id

    m = next_id
    c_scores = np.full((m, m), -1e18, dtype=np.float64)
    best_orig_h = np.full((m, m), -1, dtype=np.int64)
    best_orig_d = np.full((m, m), -1, dtype=np.int64)

    for dep in range(1, n):
        cd = int(comp[dep])
        for head in range(n):
            if head == dep:
                continue
            ch = int(comp[head])
            if ch == cd:
                continue
            w = float(scores[dep, head])
            if in_cycle[dep]:
                w -= float(scores[dep, best_head[dep]])
            if w > c_scores[cd, ch]:
                c_scores[cd, ch] = w
                best_orig_h[cd, ch] = head
                best_orig_d[cd, ch] = dep

    c_heads = _cle_arborescence(c_scores)
    res = np.full(n, -1, dtype=np.int64)
    for c_node in range(1, m):
        if c_node == cycle_id:
            continue
        orig_dep = orig_of_non_cycle[c_node]
        res[orig_dep] = best_orig_h[c_node, c_heads[c_node]]

    c_head_of_cycle = int(c_heads[cycle_id])
    kick_dep = int(best_orig_d[cycle_id, c_head_of_cycle])
    kick_head = int(best_orig_h[cycle_id, c_head_of_cycle])
    for v in range(1, n):
        if in_cycle[v]:
            res[v] = kick_head if v == kick_dep else best_head[v]
    return res


def decode_single_root_mst(scores: np.ndarray) -> np.ndarray:
    """Exact single-root MST decoder matching Java's `ChuLiuEdmonds.decodeSingleRoot`."""
    n = scores.shape[0]
    if n <= 1:
        return np.array([-1], dtype=np.int64)
    if n == 2:
        return np.array([-1, 0], dtype=np.int64)

    unconstrained = _cle_arborescence(scores)
    if int(np.sum(unconstrained[1:] == 0)) == 1:
        return unconstrained

    best_total = -1e30
    best_heads = unconstrained
    for r in range(1, n):
        constrained = scores.copy()
        constrained[1:, 0] = -1e18
        constrained[r, :] = -1e18
        constrained[r, 0] = scores[r, 0]
        cand = _cle_arborescence(constrained)
        total = sum(float(scores[d, cand[d]]) for d in range(1, n))
        if total > best_total:
            best_total = total
            best_heads = cand
    return best_heads


# ==================== Vocabularies & Feature Encoding ====================


@dataclass
class ZdepVocabs:
    lemmas: List[str]
    chars: List[str]
    upos: List[str]
    xpos: List[str]
    feats: List[str]
    labels: List[str]

    def __post_init__(self) -> None:
        self.lemma2id = {s: i for i, s in enumerate(self.lemmas)}
        self.char2id = {s: i for i, s in enumerate(self.chars) if i > CHAR_UNK_ID}
        self.upos2id = {s: i for i, s in enumerate(self.upos)}
        self.xpos2id = {s: i for i, s in enumerate(self.xpos)}
        self.feat2id = {s: i for i, s in enumerate(self.feats)}
        self.label2id = {s: i for i, s in enumerate(self.labels)}
        self.root_label_id = self.label2id.get("root", -1)

    @classmethod
    def build(
        cls,
        sentences: Sequence[ConlluSentenceRec],
        max_lemmas: int = 25000,
        min_lemma_freq: int = 2,
        min_char_freq: int = 2,
        min_feat_freq: int = 2,
        extra_sentences: Optional[Sequence[ConlluSentenceRec]] = None,
    ) -> "ZdepVocabs":
        from collections import Counter

        lemma_c: Counter[str] = Counter()
        char_c: Counter[str] = Counter()
        upos_c: Counter[str] = Counter()
        xpos_c: Counter[str] = Counter()
        feat_c: Counter[str] = Counter()
        label_c: Counter[str] = Counter()

        for sent in sentences:
            for tok in sent.tokens:
                lemma_c[tr_lower(tok.lemma)] += 1
                for ch in tok.form[:MAX_WORD_CHARS]:
                    char_c[ch] += 1
                upos_c[tok.upos] += 1
                xpos_c[tok.xpos] += 1
                for fp in tok.feat_pairs():
                    feat_c[fp] += 1
                if tok.deprel and tok.deprel != "_":
                    label_c[tok.deprel] += 1

        if extra_sentences:
            for sent in extra_sentences:
                for tok in sent.tokens:
                    lemma_c[tr_lower(tok.lemma)] += 1
                    for ch in tok.form[:MAX_WORD_CHARS]:
                        char_c[ch] += 1
                    upos_c[tok.upos] += 1
                    xpos_c[tok.xpos] += 1
                    for fp in tok.feat_pairs():
                        feat_c[fp] += 1

        lemmas = ["<PAD>", "<ROOT>", "<UNK>"] + [
            w for w, c in lemma_c.most_common(max_lemmas) if c >= min_lemma_freq
        ]
        chars = ["<PAD>", "<BOS>", "<EOS>", "<UNK>"] + [
            ch for ch, c in char_c.most_common() if c >= min_char_freq
        ]
        upos = ["<PAD>", "<ROOT>", "<UNK>"] + [u for u, _ in upos_c.most_common()]
        xpos = ["<PAD>", "<ROOT>", "<UNK>"] + [x for x, _ in xpos_c.most_common()]
        feats = ["<PAD>", "<UNK>"] + [fp for fp, c in feat_c.most_common() if c >= min_feat_freq]
        labels = [l for l, _ in label_c.most_common()]
        if "root" in labels:
            labels.remove("root")
            labels.insert(0, "root")
        return cls(lemmas=lemmas, chars=chars, upos=upos, xpos=xpos, feats=feats, labels=labels)


@dataclass
class ZdepConfig:
    lemma_dim: int = 96
    char_emb_dim: int = 24
    char_kernel_sizes: Tuple[int, ...] = (2, 3, 4)
    char_num_filters: int = 32
    upos_dim: int = 24
    xpos_dim: int = 24
    feat_dim: int = 48
    input_proj_dim: int = 224
    gru_layers: int = 2
    gru_hidden: int = 192
    arc_mlp_dim: int = 192
    rel_mlp_dim: int = 96
    dropout: float = 0.33
    word_dropout: float = 0.15


PRESETS: Dict[str, Tuple[ZdepConfig, int]] = {
    # (config, max_lemmas)
    "compact": (
        ZdepConfig(
            lemma_dim=64, char_emb_dim=24, char_kernel_sizes=(2, 3, 4), char_num_filters=24,
            upos_dim=24, xpos_dim=16, feat_dim=48, input_proj_dim=192,
            gru_layers=2, gru_hidden=160, arc_mlp_dim=160, rel_mlp_dim=80, dropout=0.33,
        ),
        20000,
    ),
    "medium": (
        ZdepConfig(
            lemma_dim=96, char_emb_dim=24, char_kernel_sizes=(2, 3, 4), char_num_filters=32,
            upos_dim=24, xpos_dim=24, feat_dim=48, input_proj_dim=256,
            gru_layers=2, gru_hidden=224, arc_mlp_dim=224, rel_mlp_dim=96, dropout=0.33,
        ),
        30000,
    ),
    "large": (
        ZdepConfig(
            lemma_dim=128, char_emb_dim=32, char_kernel_sizes=(2, 3, 4), char_num_filters=32,
            upos_dim=32, xpos_dim=24, feat_dim=64, input_proj_dim=320,
            gru_layers=3, gru_hidden=256, arc_mlp_dim=256, rel_mlp_dim=112, dropout=0.35,
        ),
        40000,
    ),
}


# ==================== PyTorch Model ====================


class ZdepBiaffineParser(nn.Module):
    """Exact PyTorch counterpart to Java's `BiaffineDependencyModel`."""

    def __init__(self, cfg: ZdepConfig, vocabs: ZdepVocabs) -> None:
        super().__init__()
        self.cfg = cfg
        self.vocabs = vocabs
        self.num_labels = len(vocabs.labels)
        char_out_dim = len(cfg.char_kernel_sizes) * cfg.char_num_filters
        raw_in_dim = cfg.lemma_dim + char_out_dim + cfg.upos_dim + cfg.xpos_dim + cfg.feat_dim

        self.lemma_emb = nn.Embedding(len(vocabs.lemmas), cfg.lemma_dim, padding_idx=PAD_ID)
        self.char_emb = nn.Embedding(len(vocabs.chars), cfg.char_emb_dim, padding_idx=CHAR_PAD_ID)
        self.char_convs = nn.ModuleList(
            [
                nn.Conv1d(cfg.char_emb_dim, cfg.char_num_filters, kernel_size=k)
                for k in cfg.char_kernel_sizes
            ]
        )
        self.upos_emb = nn.Embedding(len(vocabs.upos), cfg.upos_dim, padding_idx=PAD_ID)
        self.xpos_emb = nn.Embedding(len(vocabs.xpos), cfg.xpos_dim, padding_idx=PAD_ID)
        self.feat_emb = nn.Embedding(len(vocabs.feats), cfg.feat_dim, padding_idx=PAD_ID)

        self.input_proj = nn.Linear(raw_in_dim, cfg.input_proj_dim)
        self.input_ln = nn.LayerNorm(cfg.input_proj_dim, eps=1e-5)
        self.in_drop = nn.Dropout(cfg.dropout)

        self.gru = nn.GRU(
            input_size=cfg.input_proj_dim,
            hidden_size=cfg.gru_hidden,
            num_layers=cfg.gru_layers,
            batch_first=True,
            bidirectional=True,
            dropout=cfg.dropout if cfg.gru_layers > 1 else 0.0,
        )
        enc_dim = 2 * cfg.gru_hidden
        self.enc_drop = nn.Dropout(cfg.dropout)

        self.arc_dep_mlp = nn.Linear(enc_dim, cfg.arc_mlp_dim)
        self.arc_head_mlp = nn.Linear(enc_dim, cfg.arc_mlp_dim)
        self.rel_dep_mlp = nn.Linear(enc_dim, cfg.rel_mlp_dim)
        self.rel_head_mlp = nn.Linear(enc_dim, cfg.rel_mlp_dim)
        self.mlp_drop = nn.Dropout(cfg.dropout)

        self.arc_biaffine = nn.Parameter(torch.zeros(cfg.arc_mlp_dim + 1, cfg.arc_mlp_dim + 1))
        self.rel_biaffine = nn.Parameter(
            torch.zeros(self.num_labels, cfg.rel_mlp_dim + 1, cfg.rel_mlp_dim + 1)
        )
        self._reset_parameters()

    def _reset_parameters(self) -> None:
        for emb in (self.lemma_emb, self.char_emb, self.upos_emb, self.xpos_emb, self.feat_emb):
            nn.init.normal_(emb.weight, mean=0.0, std=0.1)
            with torch.no_grad():
                emb.weight[0].zero_()
        nn.init.xavier_uniform_(self.arc_biaffine)
        nn.init.xavier_uniform_(self.rel_biaffine)

    def _encode_chars(self, char_ids: torch.Tensor, char_valid_mask: torch.Tensor) -> torch.Tensor:
        """
        char_ids: [B, T, C_max]
        char_valid_mask: [B, T] bool (False for <ROOT> and padded token positions)
        Returns: [B, T, len(kernels) * char_num_filters]
        """
        B, T, C = char_ids.shape
        flat = char_ids.view(B * T, C)
        emb = self.char_emb(flat).transpose(1, 2)  # [B*T, D_c, C]
        pooled_parts = []
        for conv in self.char_convs:
            k = conv.kernel_size[0]
            x = emb
            if C < k:
                x = F.pad(x, (0, k - C), value=0.0)
            c_out = F.relu(conv(x))  # [B*T, F, C-k+1]
            # Mask windows that start beyond actual word length? In Java we run up to max(len, k)
            # where trailing PADs are zeros; we mirror exact per-token char_lens in collate!
            pooled_parts.append(c_out.max(dim=-1).values)
        out = torch.cat(pooled_parts, dim=-1).view(B, T, -1)
        return out * char_valid_mask.unsqueeze(-1).to(out.dtype)

    def forward(
        self,
        lemma_ids: torch.Tensor,     # [B, T]
        char_ids: torch.Tensor,      # [B, T, C]
        char_lens: torch.Tensor,     # [B, T] actual char len incl BOS/EOS (0 for ROOT/PAD)
        upos_ids: torch.Tensor,      # [B, T]
        xpos_ids: torch.Tensor,      # [B, T]
        feat_ids: torch.Tensor,      # [B, T, F_max]
        feat_counts: torch.Tensor,   # [B, T]
        lengths: torch.Tensor,       # [B] true seq len = N + 1
    ) -> Tuple[torch.Tensor, torch.Tensor, torch.Tensor]:
        B, T = lemma_ids.shape
        device = lemma_ids.device

        # Exact char-CNN with per-token window masking matching Java's effLen = max(len, k)
        C = char_ids.shape[2]
        flat_c = char_ids.view(B * T, C)
        flat_len = char_lens.view(B * T)  # [B*T]
        c_emb = self.char_emb(flat_c).transpose(1, 2)  # [B*T, D_c, C]
        pooled_parts = []
        for conv in self.char_convs:
            k = conv.kernel_size[0]
            x = F.pad(c_emb, (0, max(0, k - C)), value=0.0) if C < k else c_emb
            c_out = F.relu(conv(x))  # [B*T, F, W]
            W = c_out.shape[-1]
            eff_len = torch.clamp(flat_len, min=k)
            num_win = (eff_len - k + 1).unsqueeze(1)  # [B*T, 1]
            win_idx = torch.arange(W, device=device).unsqueeze(0)  # [1, W]
            valid_win = win_idx < num_win  # [B*T, W]
            c_out = c_out.masked_fill(~valid_win.unsqueeze(1), -1e9)
            pooled = c_out.max(dim=-1).values
            pooled = pooled * (flat_len > 0).unsqueeze(1).to(pooled.dtype)
            pooled_parts.append(pooled)
        e_char = torch.cat(pooled_parts, dim=-1).view(B, T, -1)

        e_lemma = self.lemma_emb(lemma_ids)
        e_upos = self.upos_emb(upos_ids)
        e_xpos = self.xpos_emb(xpos_ids)

        # Mean feature-bag embedding (0 if feat_counts == 0)
        f_emb = self.feat_emb(feat_ids)  # [B, T, F_max, D_f]
        f_mask = (feat_ids != PAD_ID).unsqueeze(-1).to(f_emb.dtype)
        e_feat = (f_emb * f_mask).sum(dim=2) / torch.clamp(feat_counts.unsqueeze(-1).to(f_emb.dtype), min=1.0)

        raw = torch.cat([e_lemma, e_char, e_upos, e_xpos, e_feat], dim=-1)
        x = F.relu(self.input_ln(self.input_proj(raw)))
        x = self.in_drop(x)

        packed = nn.utils.rnn.pack_padded_sequence(
            x, lengths.cpu(), batch_first=True, enforce_sorted=False
        )
        packed_out, _ = self.gru(packed)
        enc, _ = nn.utils.rnn.pad_packed_sequence(packed_out, batch_first=True, total_length=T)
        enc = self.enc_drop(enc)

        ones = torch.ones((B, T, 1), device=device, dtype=enc.dtype)

        arc_dep = self.mlp_drop(F.leaky_relu(self.arc_dep_mlp(enc), negative_slope=0.1))
        arc_head = self.mlp_drop(F.leaky_relu(self.arc_head_mlp(enc), negative_slope=0.1))
        arc_dep_b = torch.cat([arc_dep, ones], dim=-1)
        arc_head_b = torch.cat([arc_head, ones], dim=-1)
        # [B, T_dep, T_head]
        arc_scores = torch.einsum("bix,xy,bjy->bij", arc_dep_b, self.arc_biaffine, arc_head_b)

        rel_dep = self.mlp_drop(F.leaky_relu(self.rel_dep_mlp(enc), negative_slope=0.1))
        rel_head = self.mlp_drop(F.leaky_relu(self.rel_head_mlp(enc), negative_slope=0.1))
        rel_dep_b = torch.cat([rel_dep, ones], dim=-1)
        rel_head_b = torch.cat([rel_head, ones], dim=-1)
        return arc_scores, rel_dep_b, rel_head_b

    def score_labels_for_heads(
        self, rel_dep_b: torch.Tensor, rel_head_b: torch.Tensor, heads: torch.Tensor
    ) -> torch.Tensor:
        """
        Scores relation labels for given head indices.
        rel_dep_b, rel_head_b: [B, T, D_rel+1]
        heads: [B, T] (values in 0..T-1; position 0 ignored)
        Returns: [B, T, num_labels]
        """
        B, T, D = rel_head_b.shape
        safe_heads = heads.clamp(min=0, max=T - 1).unsqueeze(-1).expand(B, T, D)
        gathered_head = torch.gather(rel_head_b, dim=1, index=safe_heads)  # [B, T, D]
        return torch.einsum("bix,cxy,biy->bic", rel_dep_b, self.rel_biaffine, gathered_head)


# ==================== Dataset Encoding & Batch Collation ====================


def encode_sentence(sent: ConlluSentenceRec, vocabs: ZdepVocabs) -> Dict[str, object]:
    n = len(sent.tokens)
    lemmas = [ROOT_ID]
    upos = [ROOT_ID]
    xpos = [ROOT_ID]
    chars: List[List[int]] = [[]]  # empty for <ROOT>
    feats: List[List[int]] = [[]]
    heads = [-1]
    rels = [-1]

    for tok in sent.tokens:
        lemmas.append(vocabs.lemma2id.get(tr_lower(tok.lemma), UNK_ID))
        upos.append(vocabs.upos2id.get(tok.upos, UNK_ID))
        xpos.append(vocabs.xpos2id.get(tok.xpos, UNK_ID))
        c_seq = [CHAR_BOS_ID] + [
            vocabs.char2id.get(ch, CHAR_UNK_ID) for ch in tok.form[:MAX_WORD_CHARS]
        ] + [CHAR_EOS_ID]
        chars.append(c_seq)
        f_seq = [vocabs.feat2id[fp] for fp in tok.feat_pairs() if fp in vocabs.feat2id]
        feats.append(f_seq)
        heads.append(tok.head)
        rels.append(vocabs.label2id.get(tok.deprel, -1))

    return {
        "lemmas": lemmas,
        "upos": upos,
        "xpos": xpos,
        "chars": chars,
        "feats": feats,
        "heads": heads,
        "rels": rels,
        "length": n + 1,
    }


def collate_batch(
    items: Sequence[Dict[str, object]],
    max_kernel: int = 4,
    word_dropout: float = 0.0,
    training: bool = False,
) -> Dict[str, torch.Tensor]:
    B = len(items)
    T = max(int(it["length"]) for it in items)
    C = max(max_kernel, max((len(c) for it in items for c in it["chars"]), default=max_kernel))
    F_max = max(1, max((len(f) for it in items for f in it["feats"]), default=1))

    lemma_t = torch.full((B, T), PAD_ID, dtype=torch.long)
    upos_t = torch.full((B, T), PAD_ID, dtype=torch.long)
    xpos_t = torch.full((B, T), PAD_ID, dtype=torch.long)
    char_t = torch.full((B, T, C), CHAR_PAD_ID, dtype=torch.long)
    char_len_t = torch.zeros((B, T), dtype=torch.long)
    feat_t = torch.full((B, T, F_max), PAD_ID, dtype=torch.long)
    feat_cnt_t = torch.zeros((B, T), dtype=torch.long)
    head_t = torch.full((B, T), -1, dtype=torch.long)
    rel_t = torch.full((B, T), -1, dtype=torch.long)
    len_t = torch.zeros(B, dtype=torch.long)

    for b, it in enumerate(items):
        L = int(it["length"])
        len_t[b] = L
        lemmas = list(it["lemmas"])
        if training and word_dropout > 0.0:
            for ti in range(1, L):
                if np.random.random() < word_dropout:
                    lemmas[ti] = UNK_ID
        lemma_t[b, :L] = torch.tensor(lemmas, dtype=torch.long)
        upos_t[b, :L] = torch.tensor(it["upos"], dtype=torch.long)
        xpos_t[b, :L] = torch.tensor(it["xpos"], dtype=torch.long)
        head_t[b, :L] = torch.tensor(it["heads"], dtype=torch.long)
        rel_t[b, :L] = torch.tensor(it["rels"], dtype=torch.long)
        for ti in range(L):
            c_seq = it["chars"][ti]
            if c_seq:
                char_t[b, ti, : len(c_seq)] = torch.tensor(c_seq, dtype=torch.long)
                char_len_t[b, ti] = len(c_seq)
            f_seq = it["feats"][ti]
            if f_seq:
                feat_t[b, ti, : len(f_seq)] = torch.tensor(f_seq, dtype=torch.long)
                feat_cnt_t[b, ti] = len(f_seq)

    return {
        "lemma_ids": lemma_t,
        "char_ids": char_t,
        "char_lens": char_len_t,
        "upos_ids": upos_t,
        "xpos_ids": xpos_t,
        "feat_ids": feat_t,
        "feat_counts": feat_cnt_t,
        "heads": head_t,
        "rels": rel_t,
        "lengths": len_t,
    }


# ==================== ZDEP v1 Binary Export ====================


def _write_float_array(buf: bytearray, arr: np.ndarray) -> None:
    flat = np.asarray(arr, dtype=">f4").ravel()
    buf.extend(struct.pack(">i", flat.size))
    buf.extend(flat.tobytes())


def _write_string_array(buf: bytearray, items: Sequence[str]) -> None:
    buf.extend(struct.pack(">i", len(items)))
    for s in items:
        b = s.encode("utf-8")
        buf.extend(struct.pack(">i", len(b)))
        buf.extend(b)


def _write_matrix(buf: bytearray, mat: np.ndarray, quantize_int8: bool) -> None:
    mat2d = np.asarray(mat, dtype=np.float32)
    assert mat2d.ndim == 2, f"Expected 2D matrix, got shape {mat2d.shape}"
    rows, cols = mat2d.shape
    if not quantize_int8:
        buf.extend(struct.pack(">bii", DTYPE_FP32, rows, cols))
        buf.extend(mat2d.astype(">f4").tobytes())
    else:
        max_abs = np.max(np.abs(mat2d), axis=1)  # [rows]
        scales = np.where(max_abs > 0.0, max_abs / 127.0, 0.0).astype(np.float32)
        inv_scales = np.where(max_abs > 0.0, 127.0 / max_abs, 0.0).astype(np.float32)
        q = np.clip(np.rint(mat2d * inv_scales[:, None]), -127, 127).astype(np.int8)
        buf.extend(struct.pack(">bii", DTYPE_INT8, rows, cols))
        buf.extend(scales.astype(">f4").tobytes())
        buf.extend(q.tobytes())


@torch.no_grad()
def export_zdep(
    model: ZdepBiaffineParser,
    out_path: str | Path,
    quantize_int8: bool = True,
) -> Path:
    """Exports a trained `ZdepBiaffineParser` to ZDEP v1 binary format (`.bin` or `.bin.gz`)."""
    model.eval()
    cfg = model.cfg
    v = model.vocabs
    buf = bytearray()

    # Header
    buf.extend(struct.pack(">ii", ZDEP_MAGIC, ZDEP_VERSION))
    buf.extend(struct.pack(">iii", cfg.lemma_dim, cfg.char_emb_dim, len(cfg.char_kernel_sizes)))
    for k in cfg.char_kernel_sizes:
        buf.extend(struct.pack(">i", k))
    buf.extend(
        struct.pack(
            ">iiiiiiiii",
            cfg.char_num_filters,
            cfg.upos_dim,
            cfg.xpos_dim,
            cfg.feat_dim,
            cfg.input_proj_dim,
            cfg.gru_layers,
            cfg.gru_hidden,
            cfg.arc_mlp_dim,
            cfg.rel_mlp_dim,
        )
    )

    # Vocabs
    _write_string_array(buf, v.lemmas)
    _write_string_array(buf, v.chars)
    _write_string_array(buf, v.upos)
    _write_string_array(buf, v.xpos)
    _write_string_array(buf, v.feats)
    _write_string_array(buf, v.labels)

    def np_w(t: torch.Tensor) -> np.ndarray:
        return t.detach().cpu().float().numpy()

    # Embeddings & CharCNN (permute Conv1d [F, C, K] -> [F, K, C] -> [F, K*C] for Java time-major)
    _write_matrix(buf, np_w(model.lemma_emb.weight), quantize_int8)
    _write_matrix(buf, np_w(model.char_emb.weight), quantize_int8)
    for conv in model.char_convs:
        w = conv.weight.detach().cpu().float().permute(0, 2, 1).contiguous()
        F_out, K, C_in = w.shape
        _write_matrix(buf, w.view(F_out, K * C_in).numpy(), quantize_int8)
        _write_float_array(buf, np_w(conv.bias))
    _write_matrix(buf, np_w(model.upos_emb.weight), quantize_int8)
    _write_matrix(buf, np_w(model.xpos_emb.weight), quantize_int8)
    _write_matrix(buf, np_w(model.feat_emb.weight), quantize_int8)

    # Input projection & LayerNorm
    _write_matrix(buf, np_w(model.input_proj.weight), quantize_int8)
    _write_float_array(buf, np_w(model.input_proj.bias))
    _write_float_array(buf, np_w(model.input_ln.weight))
    _write_float_array(buf, np_w(model.input_ln.bias))

    # Bidirectional GRU weights per layer (fwd then bwd)
    for l in range(cfg.gru_layers):
        _write_matrix(buf, np_w(getattr(model.gru, f"weight_ih_l{l}")), quantize_int8)
        _write_float_array(buf, np_w(getattr(model.gru, f"bias_ih_l{l}")))
        _write_matrix(buf, np_w(getattr(model.gru, f"weight_hh_l{l}")), quantize_int8)
        _write_float_array(buf, np_w(getattr(model.gru, f"bias_hh_l{l}")))

        _write_matrix(buf, np_w(getattr(model.gru, f"weight_ih_l{l}_reverse")), quantize_int8)
        _write_float_array(buf, np_w(getattr(model.gru, f"bias_ih_l{l}_reverse")))
        _write_matrix(buf, np_w(getattr(model.gru, f"weight_hh_l{l}_reverse")), quantize_int8)
        _write_float_array(buf, np_w(getattr(model.gru, f"bias_hh_l{l}_reverse")))

    # Arc MLP + Biaffine
    _write_matrix(buf, np_w(model.arc_dep_mlp.weight), quantize_int8)
    _write_float_array(buf, np_w(model.arc_dep_mlp.bias))
    _write_matrix(buf, np_w(model.arc_head_mlp.weight), quantize_int8)
    _write_float_array(buf, np_w(model.arc_head_mlp.bias))
    _write_matrix(buf, np_w(model.arc_biaffine), quantize_int8)

    # Rel MLP + Biaffine
    _write_matrix(buf, np_w(model.rel_dep_mlp.weight), quantize_int8)
    _write_float_array(buf, np_w(model.rel_dep_mlp.bias))
    _write_matrix(buf, np_w(model.rel_head_mlp.weight), quantize_int8)
    _write_float_array(buf, np_w(model.rel_head_mlp.bias))
    rel_w = np_w(model.rel_biaffine)
    for c in range(len(v.labels)):
        _write_matrix(buf, rel_w[c], quantize_int8)

    out = Path(out_path)
    out.parent.mkdir(parents=True, exist_ok=True)
    if str(out).endswith(".gz"):
        with gzip.open(out, "wb", compresslevel=6) as gz:
            gz.write(buf)
    else:
        with open(out, "wb") as f:
            f.write(buf)
    return out
