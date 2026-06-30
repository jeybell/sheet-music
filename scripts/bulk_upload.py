#!/usr/bin/env python3
"""
악보파일 폴더 → worship-sheet API 일괄 업로드 스크립트

파일명 규칙:
  {번호}_{제목}({키})-{페이지}.{ext}
  {번호}_{제목}({키}).{ext}
  {번호}_{제목}.{ext}

키 표기:
  (.A)  → Ab  (반음 내림)
  (A코드) → A
  (E.코드) → E
"""

import os
import re
import sys
import time
import unicodedata
from pathlib import Path
from collections import defaultdict

import requests

# ── 설정 ──────────────────────────────────────────────
SHEET_DIR = Path(__file__).parent.parent / "악보파일"
API_BASE  = os.environ.get("API_BASE", "https://worship-sheet.fly.dev")
DRY_RUN   = "--dry-run" in sys.argv   # python bulk_upload.py --dry-run
YES       = "--yes" in sys.argv       # python bulk_upload.py --yes
SKIP_EXTS = {".gif", ".bmp", ".tif"}  # 업로드 제외 확장자

# 곡명이 아닌 파일명 패턴 스킵
SKIP_NAME_RE = re.compile(r'^(NAVER_|temp_|ngulove_|사본-)', re.IGNORECASE)

# ── 파일명 파싱 ────────────────────────────────────────
KEY_RE   = re.compile(r'\(([^)]+)\)')
PAGE_RE  = re.compile(r'-(\d+)$')
NUM_RE   = re.compile(r'^\d+_')
# 유효한 음악 키: C, C#, Db, D, D#, Eb, E, F, F#, Gb, G, G#, Ab, A, A#, Bb, B
VALID_KEY_RE = re.compile(r'^\.?[A-Ga-g][#b♭♯]?$')

def normalize_key(raw):
    """키 표기 정규화"""
    raw = raw.strip()
    if not raw:
        return None
    # (.X) 형태 → Xb (반음 내림)
    if raw.startswith('.'):
        candidate = raw[1:] + 'b'
        return candidate
    # '장.' 같은 찬송가 번호 표기 제거: '112장. A' → 'A'
    raw = re.sub(r'^\d+장\s*[.·]?\s*', '', raw).strip()
    # '코드' 제거
    raw = re.sub(r'\.?코드', '', raw).strip()
    # 남은 점 제거
    raw = raw.replace('.', '').strip()
    # 유효한 키인지 확인
    if VALID_KEY_RE.match(raw):
        return raw
    # 유효하지 않으면 None (장르명, 아티스트명 등)
    return None

def parse_filename(name):
    """반환: (title, key, variant) 또는 None
    variant: None이면 기본, 숫자면 별도 버전 (-1, -2 등)
    """
    name = unicodedata.normalize('NFC', name)

    if name.startswith('.'):
        return None

    stem = Path(name).stem

    if SKIP_NAME_RE.match(stem):
        return None

    if '복사본' in stem:
        return None

    # 번호 prefix 제거 (예: 109_)
    stem = NUM_RE.sub('', stem)

    # 버전 번호 추출 (-1, -2 …) → 별도 악보 버전
    variant_match = PAGE_RE.search(stem)
    variant = int(variant_match.group(1)) if variant_match else None
    if variant_match:
        stem = stem[:variant_match.start()]

    # 마지막 괄호에서 키 추출 시도
    key_matches = KEY_RE.findall(stem)
    key = None
    if key_matches:
        raw_key = key_matches[-1]
        candidate = normalize_key(raw_key)
        if candidate is not None:
            key = candidate
            stem = stem[:stem.rfind('(')].strip()

    title = stem.strip().replace(' ', '')
    if not title:
        return None

    return title, key, variant


def group_files():
    """
    반환:
      songs: { title → { (key, variant) → path } }
      각 (key, variant) 조합이 별도 sheet 버전
    """
    songs = defaultdict(dict)
    skipped = []

    for f in sorted(SHEET_DIR.iterdir()):
        if not f.is_file():
            continue
        if f.suffix.lower() in SKIP_EXTS:
            skipped.append(f.name)
            continue

        result = parse_filename(f.name)
        if result is None:
            skipped.append(f.name)
            continue

        title, key, variant = result
        sheet_key = (key, variant)
        if sheet_key not in songs[title]:
            songs[title][sheet_key] = f
        # 중복 시 첫 번째 파일 우선

    return songs, skipped


# ── API 호출 ───────────────────────────────────────────
session = requests.Session()
session.headers.update({"Accept": "application/json"})

_song_cache = {}  # title → songId (기존 곡 캐시)

def load_existing_songs():
    r = session.get(f"{API_BASE}/api/songs")
    r.raise_for_status()
    for s in r.json():
        _song_cache[s["title"]] = s["songId"]
    print(f"기존 곡 {len(_song_cache)}개 로드 완료\n")

def create_song(title):
    r = session.post(f"{API_BASE}/api/songs", json={"title": title})
    if r.status_code == 409:
        # 이미 존재하는 곡 → 캐시에서 ID 반환
        if title in _song_cache:
            return _song_cache[title]
        raise Exception(f"409이지만 캐시에 없음: {title}")
    r.raise_for_status()
    song_id = r.json()["songId"]
    _song_cache[title] = song_id
    return song_id

def create_sheet(song_id, key):
    r = session.post(f"{API_BASE}/api/songs/{song_id}/sheets",
                     json={"sheetKey": key})
    r.raise_for_status()
    return r.json()["songSheetId"]

def make_filename(title, key, variant, ext):
    key_part = f"({key})" if key else ""
    variant_part = f"-{variant}" if variant else ""
    return f"{title}{key_part}{variant_part}{ext}"

def upload_file(sheet_id, path, title, key, variant):
    filename = make_filename(title, key, variant, path.suffix.lower())
    with open(path, "rb") as fh:
        r = session.post(
            f"{API_BASE}/api/song-sheets/{sheet_id}/files",
            files={"file": (filename, fh)},
        )
    r.raise_for_status()
    return r.json()


# ── 메인 ──────────────────────────────────────────────
def main():
    print(f"📂 폴더: {SHEET_DIR}")
    print(f"🌐 API:  {API_BASE}")
    print(f"{'🔍 DRY-RUN 모드 (실제 업로드 없음)' if DRY_RUN else '🚀 업로드 모드'}\n")

    songs, skipped = group_files()

    print(f"📊 곡 수: {len(songs)}")
    print(f"⏭️  스킵: {len(skipped)}개\n")

    if skipped:
        print("[ 스킵 목록 ]")
        for s in skipped[:20]:
            print(f"  {s}")
        if len(skipped) > 20:
            print(f"  ... 외 {len(skipped)-20}개")
        print()

    total_sheets = sum(len(sheets) for sheets in songs.values())
    print(f"[ 업로드 예정 ]")
    print(f"  곡:      {len(songs):4d}개")
    print(f"  악보버전: {total_sheets:4d}개\n")

    if DRY_RUN:
        print("[ 미리보기 (처음 10곡) ]")
        for title, sheets in list(songs.items())[:10]:
            print(f"  📄 {title}")
            for (key, variant), path in sheets.items():
                fname = make_filename(title, key, variant, path.suffix.lower())
                print(f"      키={key or '없음'} → {fname}")
        return

    # 실제 업로드
    if not YES:
        confirm = input("업로드를 시작할까요? (y/N): ").strip().lower()
        if confirm != 'y':
            print("취소됨.")
            return

    load_existing_songs()

    ok = err = 0
    for title, sheets in songs.items():
        try:
            existed = title in _song_cache
            song_id = create_song(title)
            if existed:
                print(f"🔗 기존 곡 사용: {title} (id={song_id})")
            else:
                print(f"✅ 곡 생성: {title} (id={song_id})")
        except Exception as e:
            print(f"❌ 곡 생성 실패: {title} → {e}")
            err += 1
            continue

        for (key, variant), path in sheets.items():
            try:
                sheet_id = create_sheet(song_id, key)
            except Exception as e:
                print(f"  ❌ 악보 버전 생성 실패: key={key} → {e}")
                err += 1
                continue

            try:
                upload_file(sheet_id, path, title, key, variant)
                print(f"  ⬆️  {make_filename(title, key, variant, path.suffix.lower())}")
                ok += 1
            except Exception as e:
                print(f"  ❌ 업로드 실패: {path.name} → {e}")
                err += 1

            time.sleep(0.1)  # rate limit 방지

    print(f"\n완료: 성공 {ok}개, 실패 {err}개")


if __name__ == "__main__":
    main()
