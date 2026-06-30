#!/usr/bin/env python3
"""
악보 이미지 → Gemini Vision → CSV (제목/아티스트/키/가사)

사용법:
  python3 extract_lyrics.py
  python3 extract_lyrics.py --output result.csv
  python3 extract_lyrics.py --start 50   # 50번째 파일부터 재시작
"""

import os
import re
import sys
import csv
import time
import base64
import unicodedata
from pathlib import Path

import requests

SHEET_DIR  = Path(__file__).parent.parent / "악보파일"
GEMINI_KEY = os.environ.get("GEMINI_API_KEY", "")
API_URL    = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key={GEMINI_KEY}"
OUTPUT     = Path(__file__).parent / "lyrics_result.csv"
SKIP_EXTS  = {".gif", ".bmp", ".tif", ".pdf"}
BATCH_SIZE = 5    # 한 번에 보낼 이미지 수
DELAY      = 10.0  # 요청 간 딜레이(초) — 무료 플랜 분당 6회 제한

# ── 파일명 파싱 (bulk_upload.py 동일 로직) ─────────────────
NUM_RE   = re.compile(r'^\d+_')
PAGE_RE  = re.compile(r'-(\d+)$')
KEY_RE   = re.compile(r'\(([^)]+)\)')
VALID_KEY_RE = re.compile(r'^\.?[A-Ga-g][#b♭♯]?$')

def normalize_key(raw):
    raw = raw.strip()
    if not raw: return None
    if raw.startswith('.'): return raw[1:] + 'b'
    raw = re.sub(r'^\d+장\s*[.·]?\s*', '', raw).strip()
    raw = re.sub(r'\.?코드', '', raw).strip()
    raw = raw.replace('.', '').strip()
    return raw if VALID_KEY_RE.match(raw) else None

def parse_filename(name):
    name = unicodedata.normalize('NFC', name)
    if name.startswith('.'): return None
    stem = Path(name).stem
    if '복사본' in stem: return None
    stem = NUM_RE.sub('', stem)
    page_match = PAGE_RE.search(stem)
    page = int(page_match.group(1)) if page_match else 1
    if page_match: stem = stem[:page_match.start()]
    key_matches = KEY_RE.findall(stem)
    key = None
    if key_matches:
        candidate = normalize_key(key_matches[-1])
        if candidate:
            key = candidate
            stem = stem[:stem.rfind('(')].strip()
    title = stem.strip()
    return (title, key, page) if title else None


def collect_representatives():
    """곡별 첫 번째 페이지 이미지 1장씩만 수집"""
    seen = {}   # (title, key) → path
    for f in sorted(SHEET_DIR.iterdir()):
        if not f.is_file(): continue
        if f.suffix.lower() in SKIP_EXTS: continue
        result = parse_filename(f.name)
        if not result: continue
        title, key, page = result
        k = (title, key)
        if k not in seen:
            seen[k] = (title, key, f)
    return list(seen.values())


def image_to_b64(path: Path) -> tuple[str, str]:
    ext = path.suffix.lower().lstrip('.')
    mime = {'jpg': 'image/jpeg', 'jpeg': 'image/jpeg', 'png': 'image/png'}.get(ext, 'image/jpeg')
    data = base64.b64encode(path.read_bytes()).decode()
    return mime, data


PROMPT = """이 악보 이미지들을 보고 각각에 대해 아래 정보를 추출해주세요.
이미지 순서대로 JSON 배열로 응답하세요.

[
  {{
    "index": 0,
    "title": "곡 제목 (악보에서 추출, 없으면 null)",
    "artist": "아티스트/작곡가 (없으면 null)",
    "lyrics": "가사 전체 (없으면 null, 있으면 줄바꿈 포함해서 전부)"
  }},
  ...
]

주의:
- 코드(C, G, Am 등)는 가사에 포함하지 마세요
- 가사가 없는 악보(코드보만)는 lyrics를 null로
- 반드시 이미지 개수만큼 JSON 배열 항목이 있어야 합니다
- JSON만 응답하세요 (마크다운 코드블록 없이)"""


def call_gemini(batch: list[tuple]) -> list[dict]:
    """batch: [(title, key, path), ...]"""
    parts = [{"text": PROMPT}]
    for _, _, path in batch:
        mime, data = image_to_b64(path)
        parts.append({"inline_data": {"mime_type": mime, "data": data}})

    body = {"contents": [{"parts": parts}]}
    r = requests.post(API_URL, json=body, timeout=120)
    r.raise_for_status()

    raw = r.json()["candidates"][0]["content"]["parts"][0]["text"].strip()
    # 마크다운 코드블록 제거
    raw = re.sub(r'^```(?:json)?\n?', '', raw).rstrip('`').strip()
    return __import__('json').loads(raw)


def main():
    start_idx = 0
    output = OUTPUT
    for i, arg in enumerate(sys.argv[1:]):
        if arg == '--start' and i + 2 <= len(sys.argv) - 1:
            start_idx = int(sys.argv[i + 2])
        if arg == '--output' and i + 2 <= len(sys.argv) - 1:
            output = Path(sys.argv[i + 2])

    items = collect_representatives()
    total = len(items)
    print(f"총 {total}개 곡 대표 이미지 수집 완료")
    print(f"배치 크기 {BATCH_SIZE}개씩 처리 → 약 {(total // BATCH_SIZE + 1)}회 요청\n")

    # 기존 결과 로드 (재시작 지원)
    existing = {}
    if output.exists():
        with open(output, newline='', encoding='utf-8') as f:
            for row in csv.DictReader(f):
                existing[(row['title'], row['key'])] = row

    results = list(existing.values())
    processed = set(existing.keys())

    remaining = [(t, k, p) for t, k, p in items if (t, k or '') not in processed]
    remaining = remaining[start_idx:]
    print(f"처리할 항목: {len(remaining)}개 (이미 처리: {len(processed)}개)\n")

    ok = err = 0
    for i in range(0, len(remaining), BATCH_SIZE):
        batch = remaining[i:i + BATCH_SIZE]
        batch_num = i // BATCH_SIZE + 1
        total_batches = (len(remaining) + BATCH_SIZE - 1) // BATCH_SIZE
        print(f"[{batch_num}/{total_batches}] {', '.join(t for t, _, _ in batch)}", end=' ... ', flush=True)

        try:
            ocr_results = call_gemini(batch)
            for j, (title, key, _) in enumerate(batch):
                ocr = ocr_results[j] if j < len(ocr_results) else {}
                results.append({
                    'title':   title,
                    'key':     key or '',
                    'ocr_title':  ocr.get('title') or '',
                    'artist':  ocr.get('artist') or '',
                    'lyrics':  ocr.get('lyrics') or '',
                })
            print(f"✅ {len(batch)}개")
            ok += len(batch)
        except Exception as e:
            print(f"❌ {e}")
            for title, key, _ in batch:
                results.append({'title': title, 'key': key or '', 'ocr_title': '', 'artist': '', 'lyrics': ''})
            err += len(batch)

        # 중간 저장
        with open(output, 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=['title', 'key', 'ocr_title', 'artist', 'lyrics'])
            writer.writeheader()
            writer.writerows(results)

        time.sleep(DELAY)

    print(f"\n완료: 성공 {ok}개, 실패 {err}개")
    print(f"결과 저장: {output}")


if __name__ == '__main__':
    main()
