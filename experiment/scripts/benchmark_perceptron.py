#!/usr/bin/env python3
"""
Performance and throughput benchmarking script for Zemberek Morphological Disambiguation (Perceptron).

Measures:
- Words per second (total tokens / sec)
- Lexical words per second (excluding punctuation)
- Ambiguous words per second (pure decoder speed)
- Sentences per second
- Latency percentiles (mean, p50, p95, p99, min, max)
- Stage 1 (Analysis) vs Stage 2 (Disambiguation) breakdown
- Multi-threaded throughput scaling
- Side-by-side comparison between baseline and custom trained models

Usage:
  # 1. Quick benchmark of default baseline on standard corpus:
  python3 experiment/scripts/benchmark_perceptron.py

  # 2. Benchmark our latest trained model:
  python3 experiment/scripts/benchmark_perceptron.py \
    -m data/ambiguity/model-disambiguation.bin

  # 3. Side-by-side comparison of baseline vs custom model:
  python3 experiment/scripts/benchmark_perceptron.py \
    -m data/ambiguity/model-disambiguation.bin \
    --compare-baseline

  # 4. Multi-threaded benchmark across 4 CPU cores:
  python3 experiment/scripts/benchmark_perceptron.py --threads 4

  # 5. Benchmark on custom text file or JSONL:
  python3 experiment/scripts/benchmark_perceptron.py \
    -c data/ambiguity/contrastive_sentences.txt \
    --max-sentences 1000
"""

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path


def find_maven_executable() -> str:
    """Finds available Maven executable in PATH or standard IDE directories."""
    mvn_in_path = shutil.which("mvn")
    if mvn_in_path:
        return mvn_in_path

    # Check common IntelliJ embedded Maven paths
    known_paths = [
        "/home/dndara/work/idea-IC-231.8109.175/plugins/maven/lib/maven3/bin/mvn",
        os.path.expanduser("~/.local/bin/mvn"),
        "/usr/local/bin/mvn",
        "/usr/bin/mvn",
    ]
    for p in known_paths:
        if os.path.isfile(p) and os.access(p, os.X_OK):
            return p

    raise FileNotFoundError("Maven executable ('mvn') not found. Please ensure Maven is installed or set in PATH.")


def find_default_corpus(repo_root: Path) -> str:
    """Finds the best available benchmark corpus in repository."""
    candidates = [
        repo_root / "data/ambiguity/gemini_turkish_sentences_2500.txt",
        repo_root / "data/ambiguity/contrastive_sentences.txt",
        repo_root / "morphology/src/test/resources/corpora/cnn-turk-10k",
        repo_root / "data/ambiguity/gemini_turkish_sentences_500.txt",
        repo_root / "data/ambiguity/contrastive_annotated.jsonl",
    ]
    for c in candidates:
        if c.exists():
            return str(c)
    return str(candidates[0])


def main():
    parser = argparse.ArgumentParser(
        description="Benchmark Zemberek Perceptron Morphological Disambiguation throughput (words/sec, sentences/sec, latency)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )

    parser.add_argument(
        "-c", "--corpus",
        default=None,
        help="Path to benchmark text corpus (.txt or .jsonl). Default auto-selects available corpus."
    )
    parser.add_argument(
        "-m", "--model",
        default=None,
        help="Path to custom model (.bin). If omitted, benchmarks built-in default baseline."
    )
    parser.add_argument(
        "-cb", "--compare-baseline",
        action="store_true",
        help="When testing a custom model, also benchmark baseline and print side-by-side comparison table."
    )
    parser.add_argument(
        "-w", "--warmup",
        type=int,
        default=2,
        help="Number of warmup iterations to allow HotSpot JIT optimization (default: 2)."
    )
    parser.add_argument(
        "-it", "--iterations",
        type=int,
        default=5,
        help="Number of measured benchmark iterations (default: 5)."
    )
    parser.add_argument(
        "-max", "--max-sentences",
        type=int,
        default=-1,
        help="Maximum sentences to process (default: -1, all)."
    )
    parser.add_argument(
        "-t", "--threads",
        type=int,
        default=1,
        help="Number of concurrent worker threads (default: 1)."
    )
    parser.add_argument(
        "-o", "--output-json",
        default=None,
        help="Optional path to export raw benchmark results in JSON format."
    )
    parser.add_argument(
        "--prior-bigrams",
        default=None,
        help="Path to bigrams prior file (for custom prior models)."
    )
    parser.add_argument(
        "--prior-trigrams",
        default=None,
        help="Path to trigrams prior file (for custom prior models)."
    )
    parser.add_argument(
        "--prior-collocations",
        default=None,
        help="Path to collocations prior file (for custom prior models)."
    )
    parser.add_argument(
        "-b", "--beam-size",
        type=int,
        default=8,
        help="Beam size for decoding (default: 8 for Beam Search, -1 for exact Viterbi)."
    )
    parser.add_argument(
        "--exact",
        action="store_true",
        help="Use exact Viterbi trellis decoding (equivalent to --beam-size -1)."
    )
    parser.add_argument(
        "-g", "--greedy",
        action="store_true",
        help="Enable greedy (1-best) decoding mode."
    )
    parser.add_argument(
        "--all-modes",
        action="store_true",
        help="Benchmark all decoding modes (Exact Viterbi, Beam-8, Greedy) against baseline."
    )

    args = parser.parse_args()
    if args.exact:
        args.beam_size = -1

    # Find repo root
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent.parent

    # Determine corpus
    corpus_path = args.corpus or find_default_corpus(repo_root)

    mvn_bin = find_maven_executable()

    # Assemble Java arguments
    exec_args = [
        f"--corpus {corpus_path}",
        f"--warmup {args.warmup}",
        f"--iterations {args.iterations}",
        f"--threads {args.threads}",
    ]

    if args.max_sentences > 0:
        exec_args.append(f"--maxSentences {args.max_sentences}")

    if args.model:
        exec_args.append(f"--model {args.model}")

    if args.compare_baseline:
        exec_args.append("--compareBaseline")

    if args.exact:
        exec_args.append("--beamSize -1")
    elif args.beam_size > 0:
        exec_args.append(f"--beamSize {args.beam_size}")

    if args.greedy:
        exec_args.append("--greedy")

    if args.all_modes:
        exec_args.append("--benchmarkAllModes")

    if args.output_json:
        exec_args.append(f"--outputJson {args.output_json}")

    if args.prior_bigrams:
        exec_args.append(f"--priorBigrams {args.prior_bigrams}")
    if args.prior_trigrams:
        exec_args.append(f"--priorTrigrams {args.prior_trigrams}")
    if args.prior_collocations:
        exec_args.append(f"--priorCollocations {args.prior_collocations}")

    cmd = [
        mvn_bin,
        "exec:java",
        "-pl", "experiment",
        "-Dexec.cleanupDaemonThreads=false",
        "-Dexec.mainClass=zemberek.morphology.ambiguity.dataset.BenchmarkAmbiguityResolver",
        f"-Dexec.args={' '.join(exec_args)}"
    ]

    print("================================================================================")
    print("  ZEMBEREK PERCEPTRON PERFORMANCE BENCHMARK")
    print("================================================================================")
    print(f"  Maven:         {mvn_bin}")
    print(f"  Corpus:        {corpus_path}")
    print(f"  Model:         {args.model or 'Baseline (Built-in Zemberek)'}")
    print(f"  Warmup:        {args.warmup} passes | Measured: {args.iterations} passes | Threads: {args.threads}")
    if args.max_sentences > 0:
        print(f"  Max Sentences: {args.max_sentences}")
    print("--------------------------------------------------------------------------------\n")

    proc = subprocess.Popen(
        cmd,
        cwd=str(repo_root),
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )

    in_banner = False
    for line in proc.stdout:
        # Filter out verbose maven scanning lines for clean terminal output
        if line.startswith("[INFO]") and ("Scanning" in line or "Detecting" in line or "os.detected" in line or "Building" in line or "exec:3" in line):
            continue
        if "====" in line:
            in_banner = True
        if in_banner or line.startswith("I|") or line.startswith("[ERROR]"):
            print(line, end="")

    proc.wait()
    if proc.returncode != 0:
        sys.exit(proc.returncode)


if __name__ == "__main__":
    main()
