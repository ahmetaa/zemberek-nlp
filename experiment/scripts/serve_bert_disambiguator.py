#!/usr/bin/env python3
"""
HTTP and CLI inference server for Turkish BERT Morphological Disambiguator.
Serves predictions for BertAmbiguityResolver in Zemberek.

Usage (HTTP Server):
  python3 serve_bert_disambiguator.py --port 8000 --mock
  python3 serve_bert_disambiguator.py --port 8000 --model-dir models/bert_disambiguator

Usage (Interactive Stdin/Stdout Pipe for SubprocessPredictor):
  python3 serve_bert_disambiguator.py --pipe --mock
"""

import argparse
import json
import logging
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from typing import Any, Dict, List, Optional

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("ServeBertDisambiguator")


class ModelRunner:
    """Encapsulates either real BERT inference or heuristic/mock inference."""

    def __init__(self, model_dir: Optional[str] = None, mock: bool = False):
        self.mock = mock
        self.model = None
        self.tokenizer = None
        self.device = None

        if not mock and model_dir:
            self._load_model(model_dir)
        else:
            logger.info("Running in mock / heuristic disambiguation mode.")

    def _load_model(self, model_dir: str):
        try:
            import torch
            import torch.nn as nn
            from transformers import AutoModel, AutoTokenizer

            self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
            logger.info("Loading BERT model from %s on %s...", model_dir, self.device)
            self.tokenizer = AutoTokenizer.from_pretrained(model_dir)

            backbone = AutoModel.from_pretrained(model_dir)

            class BertDisambiguator(nn.Module):
                def __init__(self, encoder, hidden_size: int = 768):
                    super().__init__()
                    self.encoder = encoder
                    self.classifier = nn.Sequential(
                        nn.Linear(hidden_size * 2, hidden_size),
                        nn.ReLU(),
                        nn.Dropout(0.1),
                        nn.Linear(hidden_size, 1),
                    )

                def forward(self, input_ids, attention_mask, token_type_ids=None):
                    outputs = self.encoder(input_ids=input_ids, attention_mask=attention_mask)
                    cls_repr = outputs.last_hidden_state[:, 0, :]
                    cand_repr = outputs.last_hidden_state[:, -1, :]
                    combined = torch.cat([cls_repr, cand_repr], dim=-1)
                    return self.classifier(combined).squeeze(-1)

            self.model = BertDisambiguator(backbone, hidden_size=backbone.config.hidden_size)
            weights_path = Path(model_dir) / "bert_disambiguator.pt"
            if weights_path.exists():
                self.model.load_state_dict(torch.load(weights_path, map_location=self.device))
            self.model.to(self.device)
            self.model.eval()
            logger.info("BERT model loaded successfully.")
        except Exception as e:
            logger.warning("Failed to load neural model: %s. Falling back to mock mode.", e)
            self.mock = True

    def predict(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        sentence = request_data.get("sentence", "")
        words = request_data.get("words", [])

        predictions = []
        for word in words:
            index = word.get("index", 0)
            surface = word.get("surface", "")
            candidates = word.get("candidates", [])

            if not candidates:
                predictions.append({"index": index, "selected_candidate_id": 0})
                continue

            if self.mock or self.model is None:
                selected_id = self._mock_predict(surface, candidates)
            else:
                selected_id = self._neural_predict(sentence, surface, candidates)

            predictions.append({
                "index": index,
                "selected_candidate_id": selected_id,
            })

        return {"predictions": predictions}

    def _mock_predict(self, surface: str, candidates: List[Dict[str, Any]]) -> int:
        """Heuristic selection for testing and demonstration."""
        surface_lower = surface.lower()
        if surface_lower == "kimse":
            for c in candidates:
                if c.get("pos") == "Pron":
                    return c.get("id", 0)
        elif surface_lower == "yok":
            for c in candidates:
                if c.get("pos") == "Adj":
                    return c.get("id", 0)

        # Default heuristic: choose first candidate
        return 0

    def _neural_predict(self, sentence: str, surface: str, candidates: List[Dict[str, Any]]) -> int:
        import torch
        cand_texts = [
            f"{surface} -> {c.get('lemma', '')}:{c.get('pos', '')} {c.get('oflazer_style', '')}"
            for c in candidates
        ]
        pairs = [(sentence, cand) for cand in cand_texts]
        encoded = self.tokenizer(
            [p[0] for p in pairs],
            [p[1] for p in pairs],
            padding=True,
            truncation=True,
            max_length=128,
            return_tensors="pt",
        ).to(self.device)

        with torch.no_grad():
            scores = self.model(
                input_ids=encoded["input_ids"],
                attention_mask=encoded["attention_mask"],
            )
            return int(torch.argmax(scores).item())


def run_http_server(host: str, port: int, runner: ModelRunner):
    class DisambiguateHandler(BaseHTTPRequestHandler):
        def do_POST(self):
            if self.path != "/disambiguate":
                self.send_response(404)
                self.end_headers()
                return

            try:
                content_length = int(self.headers.get("Content-Length", 0))
                body = self.rfile.read(content_length)
                request_data = json.loads(body.decode("utf-8"))
                response_data = runner.predict(request_data)

                response_bytes = json.dumps(response_data).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(response_bytes)))
                self.end_headers()
                self.wfile.write(response_bytes)
            except Exception as e:
                logger.error("Error processing request: %s", e)
                self.send_response(500)
                self.end_headers()

        def log_message(self, format, *args):
            logger.info("%s - - [%s] %s", self.address_string(), self.log_date_time_string(), format % args)

    server = HTTPServer((host, port), DisambiguateHandler)
    logger.info("Serving BERT disambiguation server on http://%s:%d/disambiguate", host, port)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        logger.info("Server stopped.")
    finally:
        server.server_close()


def run_pipe_mode(runner: ModelRunner):
    logger.info("Reading JSONL requests from stdin and writing to stdout...")
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
            res = runner.predict(req)
            sys.stdout.write(json.dumps(res) + "\n")
            sys.stdout.flush()
        except Exception as e:
            err_res = {"error": str(e), "predictions": []}
            sys.stdout.write(json.dumps(err_res) + "\n")
            sys.stdout.flush()


def main():
    parser = argparse.ArgumentParser(description="Serve BERT morphological disambiguator")
    parser.add_argument("--host", default="127.0.0.1", help="Host address (default: 127.0.0.1)")
    parser.add_argument("--port", type=int, default=8000, help="Port to listen on (default: 8000)")
    parser.add_argument("--model-dir", help="Directory containing trained model weights and tokenizer")
    parser.add_argument("--mock", action="store_true", help="Run with heuristic/mock predictions (no GPU/model needed)")
    parser.add_argument("--pipe", action="store_true", help="Run in interactive stdin/stdout pipe mode for SubprocessPredictor")

    args = parser.parse_args()
    runner = ModelRunner(model_dir=args.model_dir, mock=args.mock or (args.model_dir is None))

    if args.pipe:
        run_pipe_mode(runner)
    else:
        run_http_server(args.host, args.port, runner)


if __name__ == "__main__":
    main()

