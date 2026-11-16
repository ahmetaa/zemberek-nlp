(() => {
  "use strict";

  const PRESETS = [
    {
      label: "Informal & Clitics",
      text: "yarın bende gelicem ama saat 7 de orda olurmusun bilmiyom"
    },
    {
      label: "Full ASCII",
      text: "turkiyede yasayan ogrencilerin pek cogu bu durumu farketmedi bile"
    },
    {
      label: "Social & Slang",
      text: "slm cnm çok tşk ederim saolll valla bişey dicem herkez yanlız kalmış"
    },
    {
      label: "Shorthand & Vowel-Drop",
      text: "mrblar arkdslr nslsnz, blmyrm yrn glmycm glb cok tskler ltfn cvplar mısınız?"
    },
    {
      label: "Splits, Merges & Units",
      text: "hiç bir zaman 16gb ram yetmiyo, diğerleride youtubeda takılıyo"
    },
    {
      label: "TDK Apostrophes",
      text: "ankaralı arkadasım 2024te türkçe'ye ve ingilizce'ye cok calıstı"
    },
    {
      label: "Clean Prose (FPR Test)",
      text: "Bu sabah kahvaltıdan sonra sahilde uzun ve sakin bir yürüyüş yaptık."
    }
  ];

  const DEASC_PRESETS = [
    {
      label: "Minimal Pairs & â/î/û",
      text: "Bu yil sirketin net kar orani rekor kirdi ama disarida yogun kar yagisi hala suruyor ve ruzgar sert esiyor."
    },
    {
      label: "Context Homographs",
      text: "Bahcedeki beyaz guller acinca yuzumuz guldu, kirlarda kosan atlar su birikintisini asti."
    },
    {
      label: "Şişli/sisli & Aydın/aydın",
      text: "Bu sabah vadi cok sisli gorunuyor ama Sisli meydaninda hava acik; aydin insanlar Aydin'da bulustu."
    },
    {
      label: "Tech Brands & URLs",
      text: "Google ve YouTube uzerinden Apple etkinligi canli yayinlandi; detaylar https://zemberek.dev adresinde paylasildi."
    },
    {
      label: "Partial-ASCII Pin Safety",
      text: "Öğrencilerin bir kismi sinav sonuçlarını ogrenmek icin sabah erkenden fakulteye uğradı."
    },
    {
      label: "OOV & Numerals (3'üncü, 6'şar)",
      text: "Saat 14.30'da 3'uncu katta 6'sar kisilik gruplarla toplancaz, kullanici adi cimcime50 olarak kaldi."
    },
    {
      label: "ALL-CAPS Headline",
      text: "ISTANBUL'DA YENI METRO HATTI ACILDI, OGRENCILER ICIN UCRETSIZ ULASIM BASLADI!"
    },
    {
      label: "Classic News",
      text: "Turkiye'de yasayan milyonlarca ogrenci bu hafta ders basi yaparken egitim teknolojileri hizla gelisiyor."
    }
  ];

  const ASCII_FOLD_MAP = {
    "ç": "c", "Ç": "C",
    "ğ": "g", "Ğ": "G",
    "ı": "i", "İ": "I",
    "ö": "o", "Ö": "O",
    "ş": "s", "Ş": "S",
    "ü": "u", "Ü": "U",
    "â": "a", "Â": "A",
    "î": "i", "Î": "I",
    "û": "u", "Û": "U"
  };

  const CIRCUMFLEX_SET = new Set(["â", "Â", "î", "Î", "û", "Û"]);
  const TR_PIN_SET = new Set(["ç", "Ç", "ğ", "Ğ", "ı", "İ", "ö", "Ö", "ş", "Ş", "ü", "Ü", "â", "Â", "î", "Î", "û", "Û"]);

  const state = {
    activeTab: "normalization",
    engineMetaById: {},
    deascMetaById: {},
    lastResponse: null,
    lastDeascResponse: null,
    selectedLineIdx: 0,
    selectedTokenIdx: null,
    selectedDeascTokenIdx: null,
    normAbort: null,
    deascAbort: null,
    morphAbort: null,
    tokAbort: null
  };

  // Shared / Topbar DOM
  const serverStatusPill = document.getElementById("serverStatusPill");
  const themeToggleBtn = document.getElementById("themeToggleBtn");

  // Normalization DOM
  const engineSelect = document.getElementById("engineSelect");
  const presetButtons = document.getElementById("presetButtons");

  const normInput = document.getElementById("normInput");
  const normOutput = document.getElementById("normOutput");
  const inputCounter = document.getElementById("inputCounter");
  const latencyBadge = document.getElementById("latencyBadge");
  const editCountBadge = document.getElementById("editCountBadge");
  const highlightToggle = document.getElementById("highlightToggle");
  const compareAllToggle = document.getElementById("compareAllToggle");
  const clearInputBtn = document.getElementById("clearInputBtn");
  const copyOutputBtn = document.getElementById("copyOutputBtn");
  const sendToMorphBtn = document.getElementById("sendToMorphBtn");

  const comparisonPanel = document.getElementById("comparisonPanel");
  const comparisonBody = document.getElementById("comparisonBody");
  const inspectorPanel = document.getElementById("inspectorPanel");
  const latticeStrip = document.getElementById("latticeStrip");
  const gateDetailBody = document.getElementById("gateDetailBody");
  const rankDetailBody = document.getElementById("rankDetailBody");

  // Deasciifiers DOM
  const deascEngineSelect = document.getElementById("deascEngineSelect");
  const deascStatRam = document.getElementById("deascStatRam");
  const deascStatSent = document.getElementById("deascStatSent");
  const deascStatWord = document.getElementById("deascStatWord");
  const deascStatPin = document.getElementById("deascStatPin");
  const deascModeNote = document.getElementById("deascModeNote");
  const deascPresetButtons = document.getElementById("deascPresetButtons");

  const deascInput = document.getElementById("deascInput");
  const deascOutput = document.getElementById("deascOutput");
  const deascInputCounter = document.getElementById("deascInputCounter");
  const deascLatencyBadge = document.getElementById("deascLatencyBadge");
  const deascChangesBadge = document.getElementById("deascChangesBadge");
  const deascCircBadge = document.getElementById("deascCircBadge");
  const deascHighlightToggle = document.getElementById("deascHighlightToggle");
  const deascCompareToggle = document.getElementById("deascCompareToggle");
  const asciifyInputBtn = document.getElementById("asciifyInputBtn");
  const clearDeascBtn = document.getElementById("clearDeascBtn");
  const copyDeascBtn = document.getElementById("copyDeascBtn");
  const deascToNormBtn = document.getElementById("deascToNormBtn");
  const deascToMorphBtn = document.getElementById("deascToMorphBtn");

  const deascComparisonPanel = document.getElementById("deascComparisonPanel");
  const deascComparisonBody = document.getElementById("deascComparisonBody");
  const deascDisagreeBadge = document.getElementById("deascDisagreeBadge");
  const deascInspectorPanel = document.getElementById("deascInspectorPanel");
  const deascChangedOnlyToggle = document.getElementById("deascChangedOnlyToggle");
  const deascTokenStrip = document.getElementById("deascTokenStrip");
  const deascCharDetailBody = document.getElementById("deascCharDetailBody");
  const deascVariantDetailBody = document.getElementById("deascVariantDetailBody");

  // Morphology & Tokenizer DOM
  const morphInput = document.getElementById("morphInput");
  const morphResults = document.getElementById("morphResults");
  const morphLatencyBadge = document.getElementById("morphLatencyBadge");

  const tokInput = document.getElementById("tokInput");
  const tokSentences = document.getElementById("tokSentences");
  const tokResults = document.getElementById("tokResults");
  const tokLatencyBadge = document.getElementById("tokLatencyBadge");

  // Theme initialization
  const prefersDark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
  const savedTheme = localStorage.getItem("zemberek_demo_theme") || (prefersDark ? "dark" : "light");
  document.documentElement.setAttribute("data-theme", savedTheme);

  themeToggleBtn.addEventListener("click", () => {
    const cur = document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
    document.documentElement.setAttribute("data-theme", cur);
    localStorage.setItem("zemberek_demo_theme", cur);
  });

  // Module Tab Switching & URL Sync
  document.querySelectorAll(".tab-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      switchTab(btn.dataset.tab, true);
    });
  });

  function resolveInitialTab() {
    const path = (window.location.pathname || "/").replace(/\/+$/, "").toLowerCase();
    const hash = (window.location.hash || "").replace(/^#/, "").toLowerCase();
    const candidate = hash || path.replace(/^\//, "");
    if (candidate === "deasciifiers" || candidate === "deasciifier") return "deasciifiers";
    if (candidate === "morphology") return "morphology";
    if (candidate === "tokenization" || candidate === "tokenizer") return "tokenization";
    return "normalization";
  }

  function switchTab(tabName, updateUrl = false) {
    state.activeTab = tabName;
    document.querySelectorAll(".tab-btn").forEach((b) => {
      const active = b.dataset.tab === tabName;
      b.classList.toggle("active", active);
      b.setAttribute("aria-selected", String(active));
    });
    document.querySelectorAll(".tab-panel").forEach((p) => {
      p.classList.toggle("active", p.id === `tab-${tabName}`);
    });
    if (updateUrl && window.history && window.history.replaceState) {
      const targetPath = tabName === "normalization" ? "/" : `/${tabName}`;
      window.history.replaceState(null, "", targetPath);
    }
    if (tabName === "normalization") {
      runNormalize();
    } else if (tabName === "deasciifiers") {
      runDeasciify();
    } else if (tabName === "morphology") {
      runMorphology();
    } else if (tabName === "tokenization") {
      runTokenize();
    }
  }

  // Normalization Preset Buttons
  PRESETS.forEach((p, idx) => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "preset-chip";
    btn.textContent = p.label;
    btn.addEventListener("click", () => {
      normInput.value = p.text;
      state.selectedTokenIdx = null;
      runNormalize();
      normInput.focus();
    });
    presetButtons.appendChild(btn);
    if (idx === 0) {
      normInput.value = p.text;
    }
  });

  // Deasciifiers Preset Buttons
  DEASC_PRESETS.forEach((p, idx) => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "preset-chip";
    btn.textContent = p.label;
    btn.addEventListener("click", () => {
      deascInput.value = p.text;
      state.selectedDeascTokenIdx = null;
      runDeasciify();
      deascInput.focus();
    });
    deascPresetButtons.appendChild(btn);
    if (idx === 0) {
      deascInput.value = p.text;
    }
  });

  // Load Server Info
  async function loadServerInfo() {
    try {
      const r = await fetch("/api/info");
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      const info = await r.json();
      (info.engines || []).forEach((e) => {
        state.engineMetaById[e.id] = e;
      });
      (info.deasciifiers || []).forEach((d) => {
        state.deascMetaById[d.id] = d;
      });
      updateDeascStatsBar();
      serverStatusPill.textContent = `Ready · ${info.version}`;
    } catch (err) {
      serverStatusPill.textContent = "Backend unreachable";
    }
  }

  function updateDeascStatsBar() {
    const meta = state.deascMetaById[deascEngineSelect.value];
    if (!meta) return;
    deascStatRam.textContent = meta.ramLabel;
    deascStatSent.textContent = meta.sentAccLabel;
    deascStatWord.textContent = meta.wordAccLabel;
    deascStatPin.textContent = meta.pinSafetyLabel;
    deascModeNote.textContent = meta.modeNote;
  }

  // ==================== TAB 1: NORMALIZATION ====================
  async function runNormalize() {
    const text = normInput.value;
    if (!text.trim()) {
      state.lastResponse = null;
      normOutput.textContent = "Normalized Turkish text will appear here as you type…";
      normOutput.classList.add("empty-placeholder");
      inputCounter.textContent = `${text.length} chars · 0 tokens`;
      latencyBadge.textContent = "0.00 ms";
      editCountBadge.textContent = "0 edits";
      comparisonPanel.classList.add("hidden");
      latticeStrip.innerHTML = `<span class="io-meta">Type a Turkish sentence above to inspect its candidate lattice.</span>`;
      gateDetailBody.innerHTML = `<span class="io-meta">No token selected.</span>`;
      rankDetailBody.innerHTML = `<span class="io-meta">No token selected.</span>`;
      return;
    }

    if (state.normAbort) {
      state.normAbort.abort();
    }
    const ctrl = new AbortController();
    state.normAbort = ctrl;

    try {
      const resp = await fetch("/api/normalize", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          text,
          engine: engineSelect.value,
          inspect: true,
          compareAll: compareAllToggle.checked
        }),
        signal: ctrl.signal
      });
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      const data = await resp.json();
      state.lastResponse = data;
      renderNormalizeResult(data);
    } catch (err) {
      if (err.name === "AbortError") return;
      serverStatusPill.textContent = "Request error";
    }
  }

  function renderNormalizeResult(data) {
    normOutput.classList.remove("empty-placeholder");
    const charLen = normInput.value.length;
    inputCounter.textContent = `${charLen} chars · ${data.totalTokens} tokens`;
    latencyBadge.textContent = `${data.latencyMs.toFixed(2)} ms`;
    editCountBadge.textContent = `${data.totalEditedSpans} ${data.totalEditedSpans === 1 ? "edit" : "edits"}`;
    serverStatusPill.textContent = `Live · ${data.latencyMs.toFixed(2)} ms`;

    if (!highlightToggle.checked || engineSelect.value === "zemberek_head") {
      normOutput.textContent = data.normalizedText;
    } else {
      normOutput.innerHTML = "";
      const lines = data.lines || [];
      lines.forEach((lt, lIdx) => {
        if (lIdx > 0) {
          normOutput.appendChild(document.createTextNode("\n"));
        }
        const spans = lt.decodedSpans || [];
        if (spans.length === 0) {
          normOutput.appendChild(document.createTextNode(lt.normalizedLine || ""));
          return;
        }
        spans.forEach((sp) => {
          const el = document.createElement("span");
          el.className = sp.changed ? `out-span changed kind-${sp.changeKind}` : "out-span";
          el.textContent = sp.normSurface;
          if (sp.changed) {
            el.title = `${sp.rawSurface} → ${sp.normSurface} (${(sp.sources || []).join(", ")})`;
            el.addEventListener("click", () => {
              state.selectedLineIdx = lIdx;
              state.selectedTokenIdx = sp.startIdx;
              renderInspector(data);
              inspectorPanel.scrollIntoView({ behavior: "smooth", block: "nearest" });
            });
          }
          normOutput.appendChild(el);
          if (sp.spaceAfter) {
            normOutput.appendChild(document.createTextNode(" "));
          }
        });
      });
    }

    if (compareAllToggle.checked && data.comparisons && data.comparisons.length > 0) {
      comparisonPanel.classList.remove("hidden");
      comparisonBody.innerHTML = "";
      data.comparisons.forEach((c) => {
        const row = document.createElement("div");
        row.className = "comp-row";
        row.innerHTML = `
          <div class="comp-engine">${escapeHtml(c.label)} <span class="comp-size">(${escapeHtml(c.sizeLabel)})</span></div>
          <div class="comp-text">${escapeHtml(c.normalizedText)}</div>
          <div class="comp-lat">${c.latencyMs.toFixed(2)} ms</div>
        `;
        comparisonBody.appendChild(row);
      });
    } else {
      comparisonPanel.classList.add("hidden");
    }

    renderInspector(data);
  }

  function renderInspector(data) {
    if (engineSelect.value === "zemberek_head") {
      latticeStrip.innerHTML = `<span class="io-meta">Select Span-Lattice Perceptron (v2.4) above to inspect the Multi-Span Candidate Lattice and Semi-Markov Viterbi scores.</span>`;
      gateDetailBody.innerHTML = `<span class="io-meta">Not applicable to Zemberek HEAD baseline.</span>`;
      rankDetailBody.innerHTML = `<span class="io-meta">Not applicable to Zemberek HEAD baseline.</span>`;
      return;
    }

    const nonEmptyLines = (data.lines || []).filter((l) => (l.decodedSpans || []).length > 0);
    if (nonEmptyLines.length === 0) {
      latticeStrip.innerHTML = `<span class="io-meta">No tokens to inspect.</span>`;
      return;
    }

    let activeLine = nonEmptyLines.find((l) => l.lineIndex === state.selectedLineIdx) || nonEmptyLines[0];
    state.selectedLineIdx = activeLine.lineIndex;

    const spans = activeLine.decodedSpans || [];
    const slots = activeLine.slots || [];

    const validStarts = new Set(spans.map((s) => s.startIdx));
    if (state.selectedTokenIdx === null || !validStarts.has(state.selectedTokenIdx)) {
      const firstEdited = spans.find((s) => s.changed);
      state.selectedTokenIdx = firstEdited ? firstEdited.startIdx : spans[0].startIdx;
    }

    latticeStrip.innerHTML = "";
    nonEmptyLines.forEach((lineObj) => {
      (lineObj.decodedSpans || []).forEach((sp) => {
        const chip = document.createElement("button");
        chip.type = "button";
        const isSel = lineObj.lineIndex === state.selectedLineIdx && sp.startIdx === state.selectedTokenIdx;
        chip.className = `span-chip kind-${sp.changeKind}${isSel ? " selected" : ""}`;

        const topHtml = sp.changed
          ? `<span class="span-chip-raw">${escapeHtml(sp.rawSurface)}</span><span>${escapeHtml(sp.normSurface)}</span>`
          : `<span>${escapeHtml(sp.normSurface)}</span>`;

        const spanRange = sp.endIdx - sp.startIdx > 1 ? `[${sp.startIdx}..${sp.endIdx}) merge` : `[${sp.startIdx}]`;
        const modeTag = sp.gateSkipped
          ? `Immutable`
          : `P=${(sp.winningProb * 100).toFixed(0)}%`;

        chip.innerHTML = `
          <div class="span-chip-top">${topHtml}</div>
          <div class="span-chip-sub">${spanRange} · ${modeTag}</div>
        `;
        chip.addEventListener("click", () => {
          state.selectedLineIdx = lineObj.lineIndex;
          state.selectedTokenIdx = sp.startIdx;
          renderInspector(data);
        });
        latticeStrip.appendChild(chip);
      });
    });

    const slot = slots.find((s) => s.tokenIdx === state.selectedTokenIdx) || slots[0];
    if (!slot) return;

    renderGateCard(slot);
    renderRankCard(slot);
  }

  function renderGateCard(slot) {
    const pct = Math.min(100, Math.max(0, slot.gateProb * 100));
    const thrPct = (slot.gateThreshold * 100).toFixed(1);
    const sig = slot.gateSignals;

    let signalsHtml = "";
    if (sig) {
      signalsHtml = `
        <div class="signal-grid">
          <div class="signal-item"><span>Length</span><strong>${sig.wordLen}</strong></div>
          <div class="signal-item"><span>Strict Morph</span><strong>${sig.strictMorph ? "Yes" : "No"}</strong></div>
          <div class="signal-item"><span>ASCII Ambig</span><strong>${sig.hasAsciiAmbiguous ? "Yes" : "No"}</strong></div>
          <div class="signal-item"><span>TR Diacritics</span><strong>${sig.hasTurkishDiacritics ? "Yes" : "No"}</strong></div>
          <div class="signal-item"><span>Clitic Hint</span><strong>${sig.cliticTailHint ? "Yes" : "No"}</strong></div>
          <div class="signal-item"><span>Merge Hint</span><strong>${sig.nextMergeHint ? "Yes" : "No"}</strong></div>
          <div class="signal-item"><span>Parallel Keep</span><strong>${(sig.parallelKeepProb * 100).toFixed(0)}%</strong></div>
          <div class="signal-item"><span>Char Phono</span><strong>${sig.charPhonotactics.toFixed(2)}</strong></div>
        </div>
      `;
    }

    gateDetailBody.innerHTML = `
      <div class="gate-meter-row">
        <span>Token <code>${escapeHtml(slot.rawSurface)}</code> P(needs_edit)</span>
        <span class="gate-prob-big">${pct.toFixed(1)}%</span>
      </div>
      <div class="gate-bar-track" title="Cutoff threshold: ${thrPct}%">
        <div class="gate-bar-fill" style="width: ${Math.max(pct, 1.2)}%"></div>
      </div>
      <div class="gate-note">${escapeHtml(slot.gateNote || "")} (edit log-odds: <code>${slot.gateLogit.toFixed(2)}</code>)</div>
      ${signalsHtml}
    `;
  }

  function renderRankCard(slot) {
    const cands = slot.candidates || [];
    if (cands.length === 0) {
      rankDetailBody.innerHTML = `<span class="io-meta">No candidate arcs at this position.</span>`;
      return;
    }

    const rowsHtml = cands
      .map((c) => {
        const pct = (c.memmProb * 100).toFixed(1);
        const srcHtml = (c.sources || [])
          .map((s) => `<span class="src-badge">${escapeHtml(s)}</span>`)
          .join("");
        const winBadge = c.winner ? "★ " : "";
        return `
          <tr class="${c.winner ? "winner-row" : ""}">
            <td class="cand-surf">${winBadge}${escapeHtml(c.normSurface)}</td>
            <td class="mono">[${c.startIdx},${c.endIdx}) ${escapeHtml(c.spanKind)}</td>
            <td class="prob-cell">
              <div class="prob-bar-wrap">
                <div class="prob-mini-bar"><div class="prob-mini-fill" style="width:${Math.max(2, c.memmProb * 100)}%"></div></div>
                <span>${pct}%</span>
              </div>
            </td>
            <td class="mono">${c.logit.toFixed(2)}</td>
            <td class="mono">${formatSigned(c.cleanSpanLlr)}</td>
            <td class="mono">${formatSigned(c.cliticSkipLlr)}</td>
            <td class="mono">${c.editCost.toFixed(2)}</td>
            <td>${srcHtml}</td>
          </tr>
        `;
      })
      .join("");

    rankDetailBody.innerHTML = `
      <div class="cand-table-wrap">
        <table class="cand-table">
          <thead>
            <tr>
              <th>Candidate</th>
              <th>Span</th>
              <th>Softmax P</th>
              <th>Arc Score</th>
              <th>3-Gram LLR</th>
              <th>Clitic Skip</th>
              <th>Edit Cost</th>
              <th>Generators</th>
            </tr>
          </thead>
          <tbody>${rowsHtml}</tbody>
        </table>
      </div>
    `;
  }

  // ==================== TAB 2: DEASCIIFIERS ====================
  async function runDeasciify() {
    const text = deascInput.value;
    if (!text.trim()) {
      state.lastDeascResponse = null;
      deascOutput.textContent = "Deasciified Turkish text will appear here as you type…";
      deascOutput.classList.add("empty-placeholder");
      deascInputCounter.textContent = `${text.length} chars · 0 tokens`;
      deascLatencyBadge.textContent = "0.00 ms";
      deascChangesBadge.textContent = "0 words · 0 chars";
      deascCircBadge.classList.add("hidden");
      deascComparisonPanel.classList.add("hidden");
      deascTokenStrip.innerHTML = `<span class="io-meta">Type or paste ASCII Turkish text above to inspect restored diacritics.</span>`;
      deascCharDetailBody.innerHTML = `<span class="io-meta">No token selected.</span>`;
      deascVariantDetailBody.innerHTML = `<span class="io-meta">No token selected.</span>`;
      return;
    }

    if (state.deascAbort) {
      state.deascAbort.abort();
    }
    const ctrl = new AbortController();
    state.deascAbort = ctrl;

    try {
      const resp = await fetch("/api/deasciify", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          text,
          engine: deascEngineSelect.value,
          compareAll: deascCompareToggle.checked
        }),
        signal: ctrl.signal
      });
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      const data = await resp.json();
      state.lastDeascResponse = data;
      renderDeasciifyResult(data);
    } catch (err) {
      if (err.name === "AbortError") return;
      serverStatusPill.textContent = "Request error";
    }
  }

  function renderDeasciifyResult(data) {
    deascOutput.classList.remove("empty-placeholder");
    const rawText = data.inputText || "";
    const outText = data.deasciifiedText || "";
    deascInputCounter.textContent = `${rawText.length} chars · ${data.totalTokens} tokens`;
    deascLatencyBadge.textContent = `${data.latencyMs.toFixed(2)} ms`;
    deascChangesBadge.textContent = `${data.changedWords} ${data.changedWords === 1 ? "word" : "words"} · ${data.changedChars} ${data.changedChars === 1 ? "char" : "chars"}`;

    if (data.circumflexCount > 0) {
      deascCircBadge.textContent = `${data.circumflexCount} â/î/û`;
      deascCircBadge.classList.remove("hidden");
    } else {
      deascCircBadge.classList.add("hidden");
    }
    serverStatusPill.textContent = `Live · ${data.latencyMs.toFixed(2)} ms`;

    // 1. Render Deasciified Output (with optional 1:1 character highlights)
    const tokens = data.tokens || [];
    if (!deascHighlightToggle.checked || !data.sameLength || tokens.length === 0) {
      deascOutput.textContent = outText;
    } else {
      deascOutput.innerHTML = "";
      let cursor = 0;
      tokens.forEach((tok) => {
        if (tok.start > cursor) {
          deascOutput.appendChild(document.createTextNode(outText.slice(cursor, tok.start)));
        }
        const span = document.createElement("span");
        const classes = ["deasc-word"];
        if (tok.changed) {
          classes.push("changed", `kind-${tok.changeKind}`);
        }
        if (tok.engineDisagreement) {
          classes.push("disagree");
        }
        span.className = classes.join(" ");

        if (tok.changed && tok.rawSurface.length === tok.restoredSurface.length) {
          span.innerHTML = buildCharHighlightedHtml(tok.rawSurface, tok.restoredSurface, rawText);
        } else {
          span.textContent = tok.restoredSurface;
        }

        if (tok.changed || tok.engineDisagreement) {
          const diffHint = tok.engineDisagreement ? " · Engines differ (click to inspect)" : "";
          span.title = `${tok.rawSurface} → ${tok.restoredSurface}${diffHint}`;
          span.addEventListener("click", () => {
            state.selectedDeascTokenIdx = tok.index;
            renderDeascInspector(data);
            deascInspectorPanel.scrollIntoView({ behavior: "smooth", block: "nearest" });
          });
        }
        deascOutput.appendChild(span);
        cursor = tok.end;
      });
      if (cursor < outText.length) {
        deascOutput.appendChild(document.createTextNode(outText.slice(cursor)));
      }
    }

    // 2. Render Side-by-Side 5-Deasciifiers Comparison Card
    if (deascCompareToggle.checked && data.comparisons && data.comparisons.length > 0) {
      deascComparisonPanel.classList.remove("hidden");
      deascDisagreeBadge.textContent = `${data.disagreementCount} ${data.disagreementCount === 1 ? "word disagreement" : "word disagreements"}`;
      deascComparisonBody.innerHTML = "";

      data.comparisons.forEach((comp) => {
        const isCurrent = comp.id === data.engine;
        const row = document.createElement("div");
        row.className = `deasc-comp-row${isCurrent ? " active-engine" : ""}`;
        row.title = isCurrent ? "Currently active deasciifier" : `Click to switch primary output to ${comp.shortLabel}`;

        const diffRenderedText = renderComparisonDiffHtml(comp.outputText, outText, tokens);
        const circNote = comp.circumflexCount > 0 ? ` · ${comp.circumflexCount}^` : "";
        const stripWarn = comp.strippedPinCount > 0 ? ` · ${comp.strippedPinCount} stripped!` : "";
        const activeBadge = isCurrent ? `<span class="active-tag">Active</span>` : "";

        row.innerHTML = `
          <div class="comp-engine">
            ${escapeHtml(comp.shortLabel)}${activeBadge}
            <div class="comp-size">${escapeHtml(comp.ramLabel)}</div>
          </div>
          <div class="comp-text">${diffRenderedText}</div>
          <div class="deasc-comp-meta">
            <div>${comp.latencyMs.toFixed(2)} ms</div>
            <div>${comp.changedWords}w / ${comp.changedChars}c${circNote}${stripWarn}</div>
          </div>
        `;
        row.addEventListener("click", () => {
          if (deascEngineSelect.value !== comp.id) {
            deascEngineSelect.value = comp.id;
            updateDeascStatsBar();
            runDeasciify();
          }
        });
        deascComparisonBody.appendChild(row);
      });
    } else {
      deascComparisonPanel.classList.add("hidden");
    }

    // 3. Render Token & Character Inspector
    renderDeascInspector(data);
  }

  function buildCharHighlightedHtml(rawWord, outWord, fullRawText) {
    const sentenceHasOtherTr = hasNonDotlessTr(fullRawText);
    let html = "";
    for (let i = 0; i < outWord.length; i++) {
      const r = rawWord[i];
      const o = outWord[i];
      if (r === o) {
        html += escapeHtml(o);
      } else if (TR_PIN_SET.has(r) && !TR_PIN_SET.has(o) && !(r === "ı" && o === "i" && !sentenceHasOtherTr)) {
        html += `<mark class="deasc-char stripped">${escapeHtml(o)}</mark>`;
      } else if (CIRCUMFLEX_SET.has(o) && !CIRCUMFLEX_SET.has(r)) {
        html += `<mark class="deasc-char circumflex">${escapeHtml(o)}</mark>`;
      } else {
        html += `<mark class="deasc-char">${escapeHtml(o)}</mark>`;
      }
    }
    return html;
  }

  function renderComparisonDiffHtml(compText, selectedText, tokens) {
    if (compText === selectedText || compText.length !== selectedText.length || tokens.length === 0) {
      return escapeHtml(compText);
    }
    let html = "";
    let cursor = 0;
    tokens.forEach((tok) => {
      if (tok.start > cursor) {
        html += escapeHtml(compText.slice(cursor, tok.start));
      }
      const cWord = compText.slice(tok.start, tok.end);
      const sWord = selectedText.slice(tok.start, tok.end);
      if (cWord !== sWord) {
        html += `<span class="comp-diff-word" title="Selected engine outputs: ${escapeHtml(sWord)}">${escapeHtml(cWord)}</span>`;
      } else {
        html += escapeHtml(cWord);
      }
      cursor = tok.end;
    });
    if (cursor < compText.length) {
      html += escapeHtml(compText.slice(cursor));
    }
    return html;
  }

  function renderDeascInspector(data) {
    const allTokens = data.tokens || [];
    if (allTokens.length === 0) {
      deascTokenStrip.innerHTML = `<span class="io-meta">No tokens to inspect.</span>`;
      deascCharDetailBody.innerHTML = `<span class="io-meta">No token selected.</span>`;
      deascVariantDetailBody.innerHTML = `<span class="io-meta">No token selected.</span>`;
      return;
    }

    const visibleTokens = deascChangedOnlyToggle.checked
      ? allTokens.filter((t) => t.changed || t.engineDisagreement)
      : allTokens;

    if (
      state.selectedDeascTokenIdx === null ||
      !allTokens.some((t) => t.index === state.selectedDeascTokenIdx)
    ) {
      const firstDisagree = allTokens.find((t) => t.engineDisagreement);
      const firstChanged = allTokens.find((t) => t.changed);
      const pick = firstDisagree || firstChanged || allTokens[0];
      state.selectedDeascTokenIdx = pick.index;
    }

    deascTokenStrip.innerHTML = "";
    if (visibleTokens.length === 0) {
      deascTokenStrip.innerHTML = `<span class="io-meta">No changed or disagreeing tokens on this input. Uncheck filter above to inspect all tokens.</span>`;
    } else {
      visibleTokens.forEach((tok) => {
        const chip = document.createElement("button");
        chip.type = "button";
        const isSel = tok.index === state.selectedDeascTokenIdx;
        const kindClass =
          tok.changeKind === "circumflex"
            ? "kind-apostrophe"
            : tok.changeKind === "diacritic"
            ? "kind-rewrite"
            : tok.changeKind === "stripped_pin"
            ? "kind-split"
            : "kind-keep";
        const disagreeClass = tok.engineDisagreement ? " disagree-ring" : "";
        chip.className = `span-chip ${kindClass}${disagreeClass}${isSel ? " selected" : ""}`;

        const topHtml = tok.changed
          ? `<span class="span-chip-raw">${escapeHtml(tok.rawSurface)}</span><span>${escapeHtml(tok.restoredSurface)}</span>`
          : `<span>${escapeHtml(tok.restoredSurface)}</span>`;

        const subTag = tok.engineDisagreement
          ? `${tok.charDiffCount}c · Differs`
          : tok.changed
          ? `${tok.charDiffCount} ${tok.charDiffCount === 1 ? "char" : "chars"}`
          : tok.immutable
          ? "Immutable"
          : "Kept";

        chip.innerHTML = `
          <div class="span-chip-top">${topHtml}</div>
          <div class="span-chip-sub">[${tok.index}] · ${subTag}</div>
        `;
        chip.addEventListener("click", () => {
          state.selectedDeascTokenIdx = tok.index;
          renderDeascInspector(data);
        });
        deascTokenStrip.appendChild(chip);
      });
    }

    const selectedTok =
      allTokens.find((t) => t.index === state.selectedDeascTokenIdx) || allTokens[0];
    renderDeascCharCard(selectedTok, data.inputText || "");
    renderDeascVariantCard(selectedTok);
  }

  function renderDeascCharCard(tok, fullRawText) {
    const raw = tok.rawSurface || "";
    const out = tok.restoredSurface || "";
    const sentenceHasOtherTr = hasNonDotlessTr(fullRawText);

    let cellsHtml = "";
    if (raw.length === out.length) {
      for (let i = 0; i < raw.length; i++) {
        const r = raw[i];
        const o = out[i];
        let cls = "char-cell";
        if (r !== o) {
          if (TR_PIN_SET.has(r) && !TR_PIN_SET.has(o) && !(r === "ı" && o === "i" && !sentenceHasOtherTr)) {
            cls += " diff-stripped";
          } else if (CIRCUMFLEX_SET.has(o) && !CIRCUMFLEX_SET.has(r)) {
            cls += " diff-circumflex";
          } else {
            cls += " diff-diacritic";
          }
        }
        cellsHtml += `
          <div class="${cls}" title="Offset ${tok.start + i}: '${escapeHtml(r)}' → '${escapeHtml(o)}'">
            <span class="char-raw">${escapeHtml(r)}</span>
            <span class="char-arrow">${r === o ? "·" : "↓"}</span>
            <span class="char-out">${escapeHtml(o)}</span>
          </div>
        `;
      }
    }

    deascCharDetailBody.innerHTML = `
      <div class="char-map-strip">${cellsHtml}</div>
      <div class="signal-grid">
        <div class="signal-item"><span>Span Offsets</span><strong>[${tok.start}..${tok.end})</strong></div>
        <div class="signal-item"><span>Changed Chars</span><strong>${tok.charDiffCount} / ${raw.length}</strong></div>
        <div class="signal-item"><span>Input Casing</span><strong>${escapeHtml(tok.casing)}</strong></div>
        <div class="signal-item"><span>Pinned TR Input</span><strong>${tok.hasPinnedTurkish ? "Yes" : "No"}</strong></div>
        <div class="signal-item"><span>Immutable Shield</span><strong>${tok.immutable ? "Yes (Protected)" : "No"}</strong></div>
        <div class="signal-item"><span>5-Mode Agreement</span><strong>${tok.engineDisagreement ? "Disagrees" : "Unanimous"}</strong></div>
      </div>
    `;
  }

  function renderDeascVariantCard(tok) {
    const latCands = tok.latticeCandidates || [];
    let latticeHtml = "";
    if (latCands.length > 0) {
      const latRows = latCands
        .map((c) => {
          const win = c.winner ? "★ " : "";
          return `
            <tr class="${c.winner ? "winner-row" : ""}">
              <td class="cand-surf">${win}${escapeHtml(c.surface)}</td>
              <td class="mono">${escapeHtml(c.lemma)}</td>
              <td class="mono">${escapeHtml(c.morphSummary)}</td>
              <td class="mono">${formatSigned(c.empPrior)}</td>
              <td class="mono">${formatSigned(c.charScore)}</td>
              <td class="mono">${formatSigned(c.unaryScore)}</td>
              <td class="mono"><strong>${formatSigned(c.seqScore)}</strong></td>
            </tr>
          `;
        })
        .join("");

      latticeHtml = `
        <div class="io-meta" style="margin-bottom:5px;">
          <strong>LatticeDeasciifier TokenLattice (${latCands.length} ${latCands.length === 1 ? "candidate" : "candidates"}):</strong>
        </div>
        <div class="cand-table-wrap" style="margin-bottom:12px;">
          <table class="cand-table">
            <thead>
              <tr>
                <th>Candidate</th>
                <th>Lemma</th>
                <th>Morph</th>
                <th>Emp LogP</th>
                <th>Char</th>
                <th>Unary</th>
                <th>Seq Total</th>
              </tr>
            </thead>
            <tbody>${latRows}</tbody>
          </table>
        </div>
      `;
    }

    const variants = tok.variants || [];
    if (variants.length === 0) {
      const parsesHtml =
        (tok.selectedMorphParses || []).length > 0
          ? tok.selectedMorphParses.map((p) => `<span class="morph-parse-pill">${escapeHtml(p)}</span>`).join("")
          : `<span class="io-meta">No dictionary parse (OOV / Foreign / Numeral)</span>`;
      deascVariantDetailBody.innerHTML = `
        ${latticeHtml}
        <div style="margin-bottom:8px;">Selected output: <strong>${escapeHtml(tok.restoredSurface)}</strong></div>
        <div>${parsesHtml}</div>
        <div class="io-meta" style="margin-top:8px;">Enable "Compare all 5" above to compare every deasciifier mode on this word.</div>
      `;
      return;
    }

    const rowsHtml = variants
      .map((v) => {
        const parses =
          (v.morphParses || []).length > 0
            ? v.morphParses.map((p) => `<span class="morph-parse-pill">${escapeHtml(p)}</span>`).join("")
            : `<span class="io-meta">OOV / Unanalyzed</span>`;
        const matchTag = v.matchesSelected ? "★ " : "";
        return `
          <tr class="${v.matchesSelected ? "winner-row" : ""}">
            <td class="mono">${escapeHtml(v.shortLabel)}</td>
            <td class="cand-surf">${matchTag}${escapeHtml(v.surface)}</td>
            <td>${parses}</td>
          </tr>
        `;
      })
      .join("");

    deascVariantDetailBody.innerHTML = `
      ${latticeHtml}
      <div class="io-meta" style="margin-bottom:5px;"><strong>5-Mode Winning Outputs &amp; Morphological Parses:</strong></div>
      <div class="cand-table-wrap">
        <table class="cand-table">
          <thead>
            <tr>
              <th>Deasciifier Mode</th>
              <th>Restored Token</th>
              <th>Zemberek Morphological Analyses</th>
            </tr>
          </thead>
          <tbody>${rowsHtml}</tbody>
        </table>
      </div>
    `;
  }

  function hasNonDotlessTr(s) {
    for (let i = 0; i < s.length; i++) {
      const c = s[i];
      if (c !== "ı" && TR_PIN_SET.has(c)) return true;
    }
    return false;
  }

  function foldTurkishToAscii(s) {
    let out = "";
    for (let i = 0; i < s.length; i++) {
      const c = s[i];
      out += ASCII_FOLD_MAP[c] || c;
    }
    return out;
  }

  // ==================== TAB 3: MORPHOLOGY ====================
  async function runMorphology() {
    const text = morphInput.value.trim();
    if (!text) {
      morphResults.innerHTML = "";
      morphLatencyBadge.textContent = "0.00 ms";
      return;
    }
    if (state.morphAbort) state.morphAbort.abort();
    const ctrl = new AbortController();
    state.morphAbort = ctrl;

    try {
      const resp = await fetch("/api/morphology", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text }),
        signal: ctrl.signal
      });
      if (!resp.ok) return;
      const data = await resp.json();
      morphLatencyBadge.textContent = `${data.latencyMs.toFixed(2)} ms`;
      morphResults.innerHTML = (data.words || [])
        .map((w) => {
          const alts = (w.candidates || [])
            .filter((c) => !c.selected)
            .map((c) => escapeHtml(c.formatted))
            .join("  ·  ");
          return `
            <div class="morph-card">
              <div class="morph-head">
                <span class="morph-word">${escapeHtml(w.surface)}</span>
                <span class="src-badge">${escapeHtml(w.bestPos)}</span>
                <span class="morph-best">★ ${escapeHtml(w.bestFormatted)}</span>
              </div>
              ${alts ? `<div class="morph-alts">Other parses: ${alts}</div>` : ""}
            </div>
          `;
        })
        .join("");
    } catch (e) {
      if (e.name !== "AbortError") console.error(e);
    }
  }

  // ==================== TAB 4: TOKENIZER ====================
  async function runTokenize() {
    const text = tokInput.value;
    if (!text.trim()) {
      tokSentences.innerHTML = "";
      tokResults.innerHTML = "";
      tokLatencyBadge.textContent = "0.00 ms";
      return;
    }
    if (state.tokAbort) state.tokAbort.abort();
    const ctrl = new AbortController();
    state.tokAbort = ctrl;

    try {
      const resp = await fetch("/api/tokenize", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text }),
        signal: ctrl.signal
      });
      if (!resp.ok) return;
      const data = await resp.json();
      tokLatencyBadge.textContent = `${data.latencyMs.toFixed(2)} ms`;
      tokSentences.innerHTML = (data.sentences || [])
        .map((s, idx) => `<div class="tok-sent-item"><strong>S${idx + 1}:</strong> ${escapeHtml(s)}</div>`)
        .join("");
      tokResults.innerHTML = (data.tokens || [])
        .map(
          (t) => `
            <div class="tok-pill" title="Span [${t.start}..${t.end}]">
              <span class="tok-pill-text">${escapeHtml(t.text)}</span>
              <span class="tok-pill-type">${escapeHtml(t.type)}</span>
            </div>
          `
        )
        .join("");
    } catch (e) {
      if (e.name !== "AbortError") console.error(e);
    }
  }

  function formatSigned(v) {
    if (Math.abs(v) < 0.005) return "0.00";
    return (v > 0 ? "+" : "") + v.toFixed(2);
  }

  function escapeHtml(s) {
    return String(s ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  // Normalization Event Listeners
  normInput.addEventListener("input", runNormalize);
  engineSelect.addEventListener("change", () => {
    runNormalize();
  });
  highlightToggle.addEventListener("change", () => {
    if (state.lastResponse) renderNormalizeResult(state.lastResponse);
  });
  compareAllToggle.addEventListener("change", runNormalize);

  clearInputBtn.addEventListener("click", () => {
    normInput.value = "";
    state.selectedTokenIdx = null;
    runNormalize();
    normInput.focus();
  });

  copyOutputBtn.addEventListener("click", async () => {
    const outText = state.lastResponse ? state.lastResponse.normalizedText : "";
    if (!outText) return;
    await navigator.clipboard.writeText(outText);
    const orig = copyOutputBtn.textContent;
    copyOutputBtn.textContent = "Copied!";
    setTimeout(() => {
      copyOutputBtn.textContent = orig;
    }, 1100);
  });

  sendToMorphBtn.addEventListener("click", () => {
    if (state.lastResponse && state.lastResponse.normalizedText) {
      morphInput.value = state.lastResponse.normalizedText;
    }
    switchTab("morphology", true);
  });

  // Deasciifiers Event Listeners
  deascInput.addEventListener("input", runDeasciify);
  deascEngineSelect.addEventListener("change", () => {
    updateDeascStatsBar();
    runDeasciify();
  });
  deascHighlightToggle.addEventListener("change", () => {
    if (state.lastDeascResponse) renderDeasciifyResult(state.lastDeascResponse);
  });
  deascCompareToggle.addEventListener("change", runDeasciify);
  deascChangedOnlyToggle.addEventListener("change", () => {
    if (state.lastDeascResponse) renderDeascInspector(state.lastDeascResponse);
  });

  asciifyInputBtn.addEventListener("click", () => {
    if (!deascInput.value) return;
    deascInput.value = foldTurkishToAscii(deascInput.value);
    state.selectedDeascTokenIdx = null;
    runDeasciify();
    deascInput.focus();
  });

  clearDeascBtn.addEventListener("click", () => {
    deascInput.value = "";
    state.selectedDeascTokenIdx = null;
    runDeasciify();
    deascInput.focus();
  });

  copyDeascBtn.addEventListener("click", async () => {
    const outText = state.lastDeascResponse ? state.lastDeascResponse.deasciifiedText : "";
    if (!outText) return;
    await navigator.clipboard.writeText(outText);
    const orig = copyDeascBtn.textContent;
    copyDeascBtn.textContent = "Copied!";
    setTimeout(() => {
      copyDeascBtn.textContent = orig;
    }, 1100);
  });

  deascToNormBtn.addEventListener("click", () => {
    if (deascInput.value.trim()) {
      normInput.value = deascInput.value;
      state.selectedTokenIdx = null;
    }
    switchTab("normalization", true);
  });

  deascToMorphBtn.addEventListener("click", () => {
    if (state.lastDeascResponse && state.lastDeascResponse.deasciifiedText) {
      morphInput.value = state.lastDeascResponse.deasciifiedText;
    }
    switchTab("morphology", true);
  });

  // Morphology & Tokenizer Event Listeners
  morphInput.addEventListener("input", runMorphology);
  tokInput.addEventListener("input", runTokenize);

  // Boot
  const initialTab = resolveInitialTab();
  loadServerInfo().then(() => {
    switchTab(initialTab, false);
  });
})();
