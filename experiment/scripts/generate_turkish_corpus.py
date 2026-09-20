#!/usr/bin/env python3
"""
Generates high-quality, diverse Turkish sentences using Google Gemini for
morphological disambiguation dataset creation, corpus analysis, and evaluation.

Supports:
- Small batch generation (e.g. 20-25 sentences per batch)
- Rich diversity across 25 distinct topics, styles, and sentence structures
- Conversational dialogues, casual speech, news, science, literature, questions, etc.
- Checkpointing and automatic resume: incrementally flushes to disk so no progress is lost
- Deduplication and text cleaning

Usage:
  python3 experiment/scripts/generate_turkish_corpus.py \
    -o data/ambiguity/gemini_turkish_sentences_2500.txt \
    --count 2500 \
    --batch-size 25
"""

import argparse
import json
import os
import sys
import time
from pathlib import Path
from typing import List, Set, Tuple, Dict, Any

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


# 25 Diverse Topics and Register Domains
TOPICS = [
    {
        "id": "daily_dialogue_casual",
        "title": "Günlük Konuşma, Diyalog ve Ev Yaşamı",
        "prompt": (
            "Günlük ev içi konuşmalar, aile fertleri arası diyaloglar, samimi selamlaşmalar, "
            "çay ve kahve ikramları, market alışverişi, evdeki küçük tamiratlar ve günlük rutinler."
        ),
        "style": "Samimi, canlı, konuşma dili kalıpları ve doğal soru-cevaplar."
    },
    {
        "id": "phone_chat_messaging",
        "title": "Telefon Görüşmeleri ve Mesajlaşma Dili",
        "prompt": (
            "Telefonda hal hatır sorma, gecikme haber verme, konum isteme, buluşma yeri kararlaştırma, "
            "akşam planları yapma ve kısa mesaj niteliğindeki doğal konuşmalar."
        ),
        "style": "Akıcı, kısa ve orta uzunlukta, doğrudan hitap içeren cümleler."
    },
    {
        "id": "workplace_office",
        "title": "İş Hayatı, Ofis İletişimi ve Projeler",
        "prompt": (
            "Ofis toplantıları, e-posta yazışmaları, sunum hazırlıkları, proje teslim tarihleri, "
            "iş arkadaşlarıyla fikir alışverişi, bütçe görüşmeleri ve görev dağılımı."
        ),
        "style": "Yarı resmi ve profesyonel, güncel iş terminolojisi içeren akıcı Türkçe."
    },
    {
        "id": "customer_service_retail",
        "title": "Alışveriş, Müşteri Hizmetleri ve Hizmet Sektörü",
        "prompt": (
            "Mağazada beden sorma, ürün iadesi, kargo takibi, restoran ve kafede sipariş verme, "
            "garsonla konuşma, garanti süresi sorgulama ve pazar tezgâhı pazarlığı."
        ),
        "style": "Nezaket bildiren ekler (-ebilir misiniz, rica etsem), doğrudan diyaloglar."
    },
    {
        "id": "news_journalism",
        "title": "Haber Bülteni, Gazetecilik ve Şehir Olayları",
        "prompt": (
            "Hava durumu uyarıları, belediye yol çalışmaları, metro ve toplu taşıma duyuruları, "
            "ekonomik gelişmeler, enflasyon ve tarım rekoltesi bültenleri."
        ),
        "style": "Haber spikeri ve gazete dili; bildirme kipleri, edilgen çatılı fiiller."
    },
    {
        "id": "science_technology",
        "title": "Bilim, Yapay Zeka, Yazılım ve Gelecek",
        "prompt": (
            "Yapay zeka modelleri, siber güvenlik, kuantum bilgisayarlar, uzay teleskopları, "
            "açık kaynak yazılımlar, veri analizi ve çevre dostu batarya teknolojileri."
        ),
        "style": "Açıklayıcı, terim içeren, neden-sonuç bağlaçları (-dığı için, amacıyla)."
    },
    {
        "id": "health_medicine",
        "title": "Sağlık, Tıp ve Hasta-Doktor İletişimi",
        "prompt": (
            "Doktor muayenesi, şikayet anlatımı (baş ağrısı, mevsimsel grip), ilaç kullanımı, "
            "tahlil sonuçları bekleme, diş randevusu ve sağlıklı beslenme önerileri."
        ),
        "style": "Hasta ve hekim cümleleri, tavsiye ve gereklilik kipleri (-meli, gerek)."
    },
    {
        "id": "education_student_life",
        "title": "Eğitim, Okul, Üniversite ve Öğrenci Hayatı",
        "prompt": (
            "Vize ve final haftası, kütüphanede ders çalışma, ödev hazırlama, kampüs hayatı, "
            "öğretmen-öğrenci diyaloğu ve yurt odası sohbetleri."
        ),
        "style": "Dinamik, gençlik dili ile akademik dilin harmanı."
    },
    {
        "id": "literature_storytelling",
        "title": "Edebi Anlatı, Roman ve Hikaye Üslubu",
        "prompt": (
            "Hüzünlü sonbahar akşamları, eski sokaklar, hatıralar, karakter iç dünyası, "
            "nostalji, yağmurlu pencereler ve şiirsel tasvirler."
        ),
        "style": "Zengin sıfat ve zarf tamlamaları, birleşik zamanlı fiiller (-iyordu, -mişti)."
    },
    {
        "id": "philosophy_thought",
        "title": "Felsefe, Düşünce ve İnsan Doğası",
        "prompt": (
            "Zamanın akışı, adalet kavramı, vicdan muhasebesi, bilgiye ulaşma arzusu, "
            "yalnızlık, özgür irade ve toplum sözleşmesi üzerine sorgulamalar."
        ),
        "style": "Derinlikli, soyut isimler, şartlı ve soru yapılı cümleler."
    },
    {
        "id": "nature_geography",
        "title": "Doğa, Coğrafya ve Çevre",
        "prompt": (
            "Karadeniz yaylaları, Toros dağları, Ege zeytinlikleri, göçmen kuşlar, "
            "orman yangınları sonrası yeniden yeşerme, nehirler ve mevsim döngüleri."
        ),
        "style": "Gözleme dayalı betimlemeler, canlı tabiat tasvirleri."
    },
    {
        "id": "travel_exploration",
        "title": "Seyahat, Gezi ve Yolculuk Deneyimleri",
        "prompt": (
            "Bavul hazırlama, tren yolculukları, terminal anonsları, pansiyon rezervasyonu, "
            "kaybolup yeni sokaklar keşfetme ve yerel halkla tanışma hikayeleri."
        ),
        "style": "Gezgin bakış açısı, zaman zarfları (-ken, -ince, -dikten sonra)."
    },
    {
        "id": "cuisine_gastronomy",
        "title": "Türk Mutfağı, Yemek Kültürü ve Gastronomi",
        "prompt": (
            "Geleneksel tencere yemekleri, taş fırın pidesi, zeytinyağlı sarmalar, baharatlar, "
            "misafir sofrası hazırlığı, köfte harcı yoğurma ve tatlı ikramları."
        ),
        "style": "Lezzet ve duyusal niteleyiciler (taze, çıtır, sıcak, baharatlı)."
    },
    {
        "id": "arts_cinema_theater",
        "title": "Kültür, Sanat, Tiyatro ve Sinema",
        "prompt": (
            "Tiyatro oyunu prömiyeri, film festivali değerlendirmeleri, sergi açılışları, "
            "oyunculuk performansları, senaryo örgüsü ve sahne ışıkları."
        ),
        "style": "Eleştirel ve tanıtıcı, karşılaştırma ifadeleri (daha, en, göre)."
    },
    {
        "id": "music_instruments",
        "title": "Müzik, Enstrümanlar ve Konserler",
        "prompt": (
            "Bağlama akordu yapma, piyano resitali, konser provası, kulaklıkla sokakta yürüme, "
            "plak koleksiyonu, ritim tutma ve klasik Türk müziği makamları."
        ),
        "style": "İşitsel tasvirler, duygu yüklü ifadeler."
    },
    {
        "id": "sports_fitness",
        "title": "Spor, Futbol, Atletizm ve Fiziksel Antrenman",
        "prompt": (
            "Hafta sonu derbi heyecanı, hakem kararları, sabah koşusu, esneme hareketleri, "
            "yüzme havuzu, sakatlık sonrası rehabilitasyon ve maraton hazırlığı."
        ),
        "style": "Hareket ve eylem odaklı, dinamik fiiller."
    },
    {
        "id": "history_heritage",
        "title": "Tarih, Arkeoloji ve Mimari Miras",
        "prompt": (
            "Göbeklitepe kazıları, Selçuklu kervansarayları, Osmanlı çinileri, antik tiyatrolar, "
            "arşiv belgeleri inceleme ve tarihi yarımada surları."
        ),
        "style": "Geçmiş zaman anlatımı, tarihi isim tamlamaları."
    },
    {
        "id": "transport_traffic",
        "title": "Ulaşım, Trafik ve Şehir İçi Yolculuk",
        "prompt": (
            "Vapur iskeleleri ve martılar, metrobüs kalabalığı, köprü trafiği, kavşak sinyalizasyonu, "
            "taksi çağırma, bisiklet yolu ve feribot seferleri."
        ),
        "style": "Gündelik hareketlilik, yer-yön bildiren ekler (-e, -den, -e doğru)."
    },
    {
        "id": "housing_neighborhood",
        "title": "Mahalle, Komşuluk ve Ev Yaşamı",
        "prompt": (
            "Bakkal Muhittin amca, apartman yönetimi toplantısı, su kesintisi haberi, "
            "balkonda çamaşır asma, halı yıkama ve komşuya tabak götürme."
        ),
        "style": "Sıcak, yerel kültür ögeleri barındıran doğal cümleler."
    },
    {
        "id": "idioms_proverbs",
        "title": "Deyimler, Atasözleri ve Kalıplaşmış Sözler",
        "prompt": (
            "Türkçedeki zengin deyimlerin doğal bağlamda geçtiği cümleler (göz yummak, pabucu dama atılmak, "
            "eteğindeki taşları dökmek, eli kulağında olmak, kulak kabartmak)."
        ),
        "style": "Deyimlerin yapay durmadığı, hikaye içi organik kullanımı."
    },
    {
        "id": "questions_inquiries",
        "title": "Sorular, Merak ve Bilgi Alma Cümleleri",
        "prompt": (
            "Günlük yaşamda sorulan doğrudan sorular, yol sorma, fiyat sorma, fikir danışma, "
            "retorik sorular ve derin felsefi sorgulama cümleleri."
        ),
        "style": "Soru edatları (mı/mi/mu/mü), soru zamirleri (kim, ne, nasıl, neden, nerede)."
    },
    {
        "id": "exclamations_emotions",
        "title": "Duygular, Ünlemler, Şaşkınlık ve Tepkiler",
        "prompt": (
            "Şaşırma (Vay canına, inanamıyorum), sevinç, sitem, hayıflanma (keşke daha önce söyleseydin), "
            "övgü, tebrik ve teselli bildiren duygusal cümleler."
        ),
        "style": "Ünlemli, duygu yüklü, devrik ve eksiltili cümle çeşitliliği."
    },
    {
        "id": "law_bureaucracy",
        "title": "Hukuk, Resmi Daireler ve Bürokrasi",
        "prompt": (
            "Noter işlemleri, tapu devri, sözleşme maddeleri, dilekçe yazımı, nüfus müdürlüğü randevusu "
            "ve resmi tebligat bildirimleri."
        ),
        "style": "Resmi Türkçe, mastar ekleri (-mek, -me), edilgen çatı."
    },
    {
        "id": "weather_seasons",
        "title": "Hava Durumu, Mevsimler ve İklim",
        "prompt": (
            "İstanbul lodosu, aniden bastıran sağanak, fındık büyüklüğünde dolu, karlı yayla sabahı, "
            "boğucu yaz sıcağı ve ilkbahar cemreleri."
        ),
        "style": "Doğa olayları, zaman zarfları ve duyusal betimlemeler."
    },
    {
        "id": "digital_culture_gaming",
        "title": "Dijital Kültür, Sosyal Medya ve Oyun Dünyası",
        "prompt": (
            "Canlı yayın sohbetleri, podcast dinleme, online oyun stratejileri, bildirimleri sessize alma, "
            "fotoğraf filtreleri ve teknoloji topluluğu tartışmaları."
        ),
        "style": "Modern kentli ve dijital jenerasyon dili, güncel sözcükler."
    },
]


def call_gemini_batch(
    api_key: str,
    topic_info: dict,
    batch_size: int = 25,
    model: str = "gemini-flash-latest",
    thinking_budget: int = 0
) -> Tuple[List[str], Dict[str, Any]]:
    """Generates batch_size Turkish sentences for a given topic using Gemini with thinkingBudget controls."""
    system_instruction = (
        "You are an expert computational linguist and native Turkish prose writer. "
        "Your task is to generate grammatically flawless, highly varied, fluent, and authentic "
        "Turkish sentences to train and benchmark Turkish morphological disambiguation and NLP models."
    )

    prompt = f"""Generate exactly {batch_size} high-quality, completely independent Turkish sentences adhering to the following theme and style:
Topic: {topic_info['title']}
Context & Theme: {topic_info['prompt']}
Stylistic Register: {topic_info.get('style', 'Natural, fluent Turkish.')}

Guidelines & Constraints:
1. Varied Sentence Length and Structure:
   - Short conversational dialogue turns or colloquial remarks (3-6 words).
   - Medium-length descriptive or declarative statements (7-14 words).
   - Complex compound sentences containing converbial clauses (-erek, -ince, -dikçe, -ken), relative participles (-en, -dik, -ecek), or conditional moods (15-25 words).
2. Rich Morphological Diversity:
   - Use diverse verb tenses and modalities (progressive, future, past, aorist, necessitative, optative, conditional).
   - Include diverse noun case markers (accusative, dative, locative, ablative), possessive suffixes (1st, 2nd, 3rd person singular/plural), and question particles (mı/mi/mu/mü).
3. Contextual Novelty: Sentences must not repeat patterns, cliches, or vocabulary within the batch; each sentence must depict an original, distinct scenario.
4. Orthography: Strictly follow Turkish Language Association (TDK) orthographic, apostrophe, and punctuation rules.
5. Target Language: Every generated sentence in the output array MUST be in natural, authentic Turkish.

Output Format:
Return ONLY a valid JSON object matching the following structure (no markdown formatting or fences, raw JSON only):
{{
  "sentences": [
    "Turkish sentence 1...",
    "Turkish sentence 2..."
  ]
}}
"""

    # 1. Try official SDK
    if HAS_GOOGLE_GENAI:
        try:
            client = genai.Client(api_key=api_key)
            config_kwargs: Dict[str, Any] = {
                "temperature": 0.85,
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
                config=config,
            )
            raw = response.text
            sentences = parse_sentences_json(raw)
            usage: Dict[str, Any] = {}
            if hasattr(response, "usage_metadata") and response.usage_metadata:
                um = response.usage_metadata
                usage = {
                    "promptTokenCount": getattr(um, "prompt_token_count", 0),
                    "candidatesTokenCount": getattr(um, "candidates_token_count", 0),
                    "thoughtsTokenCount": getattr(um, "thoughts_token_count", 0),
                    "totalTokenCount": getattr(um, "total_token_count", 0),
                }
            if sentences:
                return sentences, usage
        except Exception as e:
            print(f"[WARN] SDK call failed ({e}), falling back to REST...")

    # 2. Try REST API with retry & backoff
    if HAS_REQUESTS:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
        generation_config: Dict[str, Any] = {
            "temperature": 0.85,
            "responseMimeType": "application/json"
        }
        if thinking_budget is not None:
            generation_config["thinkingConfig"] = {
                "thinkingBudget": thinking_budget
            }

        payload = {
            "system_instruction": {"parts": [{"text": system_instruction}]},
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": generation_config
        }
        for attempt in range(5):
            try:
                resp = requests.post(url, json=payload, timeout=45)
                if resp.status_code == 200:
                    data = resp.json()
                    usage = data.get("usageMetadata", {})
                    parts = data.get("candidates", [])[0].get("content", {}).get("parts", [])
                    if parts:
                        raw = parts[0].get("text", "")
                        sentences = parse_sentences_json(raw)
                        if sentences:
                            return sentences, usage
                elif resp.status_code in (429, 503):
                    wait = 2 * (attempt + 1)
                    print(f"[WARN] HTTP {resp.status_code} (Demand/Rate). Retrying in {wait}s...")
                    time.sleep(wait)
                else:
                    print(f"[ERROR] HTTP {resp.status_code}: {resp.text[:200]}")
                    time.sleep(2)
            except Exception as e:
                print(f"[ERROR] REST request exception: {e}. Retrying in 2s...")
                time.sleep(2)

    return [], {}


def parse_sentences_json(raw: str) -> List[str]:
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
        if isinstance(data, dict) and "sentences" in data:
            return [clean_sentence(s) for s in data["sentences"] if clean_sentence(s)]
        elif isinstance(data, list):
            return [clean_sentence(s) for s in data if clean_sentence(s)]
    except Exception as e:
        print(f"[WARN] JSON parsing error: {e}")

    # Fallback line-by-line parsing
    lines = []
    for line in cleaned.splitlines():
        c = clean_sentence(line)
        if c:
            lines.append(c)
    return lines


def clean_sentence(s: str) -> str:
    s = s.strip().strip('"').strip("'").strip()
    # Strip leading numbers like "1. " or "- "
    if s and (s[0].isdigit() or s.startswith("-")):
        parts = s.split(" ", 1)
        if len(parts) > 1:
            s = parts[1].strip()

    if not s:
        return ""

    # Ensure ending punctuation
    if s[-1] not in ".?!…":
        s += "."

    return s


def load_existing(file_path: Path) -> List[str]:
    if not file_path.exists():
        return []
    sentences = []
    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            line = clean_sentence(line)
            if line:
                sentences.append(line)
    return sentences


def main():
    parser = argparse.ArgumentParser(description="Generate diverse Turkish sentences with Gemini in small batches with token monitoring")
    parser.add_argument("-o", "--output", default="data/ambiguity/gemini_turkish_sentences_2500.txt", help="Output text file")
    parser.add_argument("-m", "--model", default="gemini-flash-latest", help="Gemini model name")
    parser.add_argument("--thinking-budget", type=int, default=0, help="Thinking token budget (0 disables hidden reasoning tokens)")
    parser.add_argument("--max-budget-usd", type=float, default=5.0, help="Circuit breaker: stop if estimated cost in USD exceeds this")
    parser.add_argument("--count", type=int, default=2500, help="Target total sentence count (default: 2500)")
    parser.add_argument("--batch-size", type=int, default=25, help="Batch size per Gemini API call (default: 25)")
    parser.add_argument("--api-key", default=None, help="Gemini API key")

    args = parser.parse_args()
    api_key = args.api_key or os.environ.get("GEMINI_API_KEY")

    if not api_key:
        print("[ERROR] GEMINI_API_KEY environment variable is not set.")
        sys.exit(1)

    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    # Check for existing sentences (resume capability)
    existing_sentences = load_existing(out_path)
    seen: Set[str] = set(existing_sentences)
    total_collected = len(existing_sentences)

    if total_collected > 0:
        print(f"[RESUME] Found {total_collected} existing sentences in {out_path}. Continuing...")

    if total_collected >= args.count:
        print(f"[DONE] File already contains {total_collected} sentences (>= target {args.count}). Nothing to do.")
        return

    print(f"[INFO] Target: {args.count} sentences across {len(TOPICS)} diverse topics.")
    print(f"[INFO] Batch size: {args.batch_size} sentences per call | Model: {args.model} | thinkingBudget={args.thinking_budget}")
    print(f"[INFO] Cost Safeguards: max_budget=${args.max_budget_usd:.2f}")
    print(f"[INFO] Saving incrementally to: {out_path.resolve()}\n")

    topic_idx = 0
    batch_num = 0
    cum_prompt_tokens = 0
    cum_output_tokens = 0
    cum_thoughts_tokens = 0

    # Open in append mode so every batch is saved immediately
    with open(out_path, "a", encoding="utf-8") as out_file:
        while total_collected < args.count:
            topic = TOPICS[topic_idx % len(TOPICS)]
            needed = args.count - total_collected
            curr_batch_size = min(args.batch_size, needed)

            batch_num += 1
            print(f"[Batch {batch_num:3d}] Requesting {curr_batch_size} sentences on topic: '{topic['title']}'...")

            batch_sentences, usage = call_gemini_batch(
                api_key=api_key,
                topic_info=topic,
                batch_size=curr_batch_size,
                model=args.model,
                thinking_budget=args.thinking_budget
            )

            prompt_t = usage.get("promptTokenCount", 0)
            cand_t = usage.get("candidatesTokenCount", 0)
            thought_t = usage.get("thoughtsTokenCount", 0)
            cum_prompt_tokens += prompt_t
            cum_output_tokens += cand_t
            cum_thoughts_tokens += thought_t

            added_this_batch = 0
            for s in batch_sentences:
                if s and s not in seen:
                    seen.add(s)
                    out_file.write(s + "\n")
                    out_file.flush()
                    total_collected += 1
                    added_this_batch += 1
                    if total_collected >= args.count:
                        break

            est_cost = (cum_prompt_tokens / 1_000_000.0 * 0.15) + ((cum_output_tokens + cum_thoughts_tokens) / 1_000_000.0 * 0.60)
            print(f"            Added {added_this_batch}/{len(batch_sentences)} valid unique sentences. (Progress: {total_collected}/{args.count} - {total_collected/args.count*100:.1f}%)")
            print(f"            [TOKENS] Batch: {cand_t} out, {thought_t} thoughts | Total Out: {cum_output_tokens + cum_thoughts_tokens:,} | Est Cost: ${est_cost:.3f}")

            if est_cost > args.max_budget_usd:
                print(f"\n[CIRCUIT BREAKER TRIGGERED] Estimated cost (${est_cost:.2f}) exceeded budget of ${args.max_budget_usd:.2f}!")
                print(f"[HALTING] Stopping sentence generation to prevent unexpected charges. Saved {total_collected} sentences.")
                break

            topic_idx += 1
            time.sleep(0.3)

    print(f"\n=======================================================")
    print(f"[SUCCESS] Successfully generated and verified {total_collected} sentences!")
    print(f"[OUTPUT]  Saved to: {out_path.resolve()}")

    # Compute final statistics
    all_final = load_existing(out_path)
    word_counts = [len(s.split()) for s in all_final]
    avg_words = sum(word_counts) / max(len(word_counts), 1)
    min_words = min(word_counts) if word_counts else 0
    max_words = max(word_counts) if word_counts else 0

    print(f"[STATS]   Word counts: min={min_words}, max={max_words}, avg={avg_words:.1f} words/sentence")
    print(f"=======================================================\n")

    print("Sample generated sentences:")
    for i in range(min(8, len(all_final))):
        print(f"  {i+1:2d}. {all_final[i]}")
    print("  ...")
    for i in range(max(0, len(all_final) - 5), len(all_final)):
        print(f"  {i+1:2d}. {all_final[i]}")


if __name__ == "__main__":
    main()
