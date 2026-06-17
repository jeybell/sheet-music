import re
import io
from contextlib import asynccontextmanager
from typing import Optional

import easyocr
import numpy as np
from fastapi import FastAPI, File, UploadFile, HTTPException
from PIL import Image

reader: Optional[easyocr.Reader] = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global reader
    reader = easyocr.Reader(['ko', 'en'], gpu=False)
    yield

app = FastAPI(lifespan=lifespan)

# "Key: G", "키: G", "Key of G" 등 명시적 키 레이블
_KEY_EXPLICIT = re.compile(
    r'(?:Key|키)\s*(?:of\s*)?[:\s]\s*([A-G][#b]?(?:m|M)?)',
    re.IGNORECASE,
)
# 코드 심볼 패턴 (Am, Gmaj7, C/E, Bm7 등)
_CHORD = re.compile(
    r'(?<![A-Za-z])([A-G][#b]?(?:m(?:aj)?|M|dim|aug|sus)?(?:2|4|5|6|7|9|11|13)?(?:maj[79])?(?:/[A-G][#b]?)?)(?![A-Za-z0-9])'
)
# 단독 알파벳 한 글자는 코드로 보지 않음
_SINGLE_LETTER = re.compile(r'^[A-G]$')


def _parse_key(text: str) -> Optional[str]:
    m = _KEY_EXPLICIT.search(text)
    return m.group(1) if m else None


def _parse_chords(text: str) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for m in _CHORD.finditer(text):
        chord = m.group(1)
        if chord not in seen and not _SINGLE_LETTER.match(chord):
            seen.add(chord)
            result.append(chord)
    return result[:15]


def _parse_title(results: list) -> Optional[str]:
    """이미지 최상단 텍스트를 제목 후보로 반환."""
    if not results:
        return None
    # bbox의 y좌표가 가장 작은(=화면 위쪽) 텍스트 순으로 정렬
    sorted_results = sorted(results, key=lambda r: min(p[1] for p in r[0]))
    for _, text, conf in sorted_results:
        text = text.strip()
        if text and conf > 0.5 and len(text) >= 2:
            return text
    return None


@app.post("/ocr")
async def ocr(file: UploadFile = File(...)):
    content_type = file.content_type or ""
    if not content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="image/* 파일만 지원합니다.")

    content = await file.read()
    try:
        image = Image.open(io.BytesIO(content)).convert("RGB")
    except Exception:
        raise HTTPException(status_code=400, detail="이미지를 열 수 없습니다.")

    results = reader.readtext(np.array(image))

    full_text = " ".join(text for _, text, conf in results if conf > 0.3)
    title = _parse_title([(b, t, c) for b, t, c in results if c > 0.5])
    key = _parse_key(full_text)
    chords = _parse_chords(full_text)

    return {
        "title": title,
        "key": key,
        "chords": chords,
        "rawText": full_text,
    }


@app.get("/health")
def health():
    return {"status": "ok"}
