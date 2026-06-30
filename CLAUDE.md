# 악보 정리 앱 (sheet-music)

## 프로젝트 개요
예배/공연용 악보를 곡 단위로 관리하고, 셋리스트(콘티)를 구성하는 앱.

## 작업 원칙 (직렬 워크플로우, 한 번에 한 이슈)
- **이슈 = 세션 = 브랜치 1:1.** 이슈마다 별도 세션으로 분리하고, git도 별도 브랜치로 분리한다.
- ⚠️ 세션을 새로 열어도 워킹 트리(파일·diff)는 디스크에 그대로 남는다. **세션 분리 ≠ 작업 격리.** 격리는 브랜치로 보장한다.
- **한 번에 한 이슈만** 작업한다. 다음 이슈로 넘어가기 전에 현재 이슈를 커밋/PR로 마무리하거나 stash한다 (diff가 섞이지 않게).
- **main 직접 커밋 금지.** `feat/{이슈번호}-{설명}` / `fix/{이슈번호}-{설명}` 브랜치만 사용.
- main push = 자동배포이므로 **PR 머지로만 main에 반영**한다. 커밋 메시지에 `#이슈번호`를 넣어 이슈와 연결.
- 이슈 시작 → `git switch -c feat/#N-...` → 작업·커밋 → push → PR → merge → `git switch main && git pull` → 다음 이슈.

## 기술 스택
- **백엔드**: Java, Spring Boot, Gradle
- **프론트엔드**: Vue 3, TypeScript
- **DB**: PostgreSQL
- **인프라**: Docker Compose / Fly.io (백엔드) + Vercel (프론트엔드)
- **스토리지**: Cloudflare R2 (파일 업로드)

## 도메인 구조
```
songs (곡)
  └── song_sheets (악보 버전, 키별)
        └── song_files (악보 파일, PDF 등)

setlists (셋리스트/콘티)
  └── setlist_items (곡 순서, 어떤 악보 버전 쓸지 포함)
```

## 주요 설계 결정
- 삭제는 모두 **soft delete** (`deleted_at` 기록 방식)
- 파일 저장 경로: `./uploads/sheets/{songSheetId}/`
- 파일명은 UUID로 저장 (원본명 별도 보관)
- `sheet_key`, `version_name`은 선택사항, 같은 곡 안에서 동일 키 중복 허용

## API 구조 (README 기준)
| 도메인 | 경로 |
|--------|------|
| Songs | `/api/songs` |
| Song Sheets | `/api/songs/{songId}/sheets`, `/api/song-sheets/{id}` |
| Song Files | `/api/song-sheets/{id}/files`, `/api/song-files/{id}/view`, `/api/song-files/{id}/download` |
| Setlists | `/api/setlists` (구현 예정) |

## 구현된 주요 기능
- **곡/악보/파일**: songs·song_sheets·song_files CRUD, 곡 검색, soft delete
- **셋리스트**: setlists·setlist_items CRUD, 콘티 화면, 드래그앤드롭 순서 변경, 셋리스트 PDF 다운로드(jsPDF)
- **공유 링크**: UUID 토큰 기반 콘티 공유 (`/share/:token`), 공개 뷰
- **OCR 자동 추출**: EasyOCR Python 마이크로서비스 → Spring Boot 연동. 제목/코드/아티스트/가사/rawText 추출, `@Async` 비동기 처리 후 상세 화면 15초 폴링 반영
- **통합 등록/업로드**: 곡+악보 1단계 통합 등록(OCR 자동입력), 일괄 업로드 화면(`/songs/bulk`, 드래그앤드롭 + 카드별 편집)
- **가사 관리**: songs.lyrics 컬럼, `PATCH /lyrics`, 곡 상세 가사 섹션(OCR 적용 + 직접 편집)
- **태그**: song_tags 테이블(`@ElementCollection EAGER+SUBSELECT`), 태그 칩 입력/필터
- **링크**: song_links 테이블, 멀티 플랫폼(YouTube/Spotify/Melon 등) 자동 감지, YouTube 임베드 토글
- **통합 검색**: 제목·아티스트·가사 LIKE + 키 대소문자 무관 LIKE 필터 + 태그 필터
- **로딩 UX**: axios 인터셉터 전역 로딩 바 + 콘텐츠 딤 처리 (`useHttpLoading`)
- **UI**: shadcn-vue 스타일 + Tailwind v4 + 다크/라이트 테마 + 반응형, 악보 이미지 슬라이드 뷰어
- **인프라**: CI/CD 자동배포(GitHub Actions → Fly.io, Vercel Git 연동), Cloudflare R2 연동(STORAGE_TYPE 전환, 현재 local 모드)

## DB 마이그레이션 현황
| 버전 | 내용 |
|------|------|
| V1 | 초기 스키마 (songs, song_sheets, song_files, setlists, setlist_items) |
| V2 | feature_requests 테이블 |
| V3 | OCR 관련 컬럼 |
| V4 | songs.lyrics 컬럼 |
| V5 | feature_requests 추가 |
| V6 | songs.youtube_url 컬럼 |
| V7 | setlists.share_token 컬럼 |
| V8 | song_tags 테이블 |
| V9 | song_links 테이블 (기존 youtube_url 데이터 이전) |

## 다음 작업 예정 이슈
| 번호 | 제목 |
|------|------|
| #59 | 링크 UX 개선 (추가 시 URL만, 더보기 아코디언) |
| #60 | 콘티 등록 시 곡 추가까지 1단계로 |
| #61 | 등록 기능 간소화 및 기본값 설정 |
| #62 | 악보 목록 카드에서 키 있으면 음표 대신 키 배지 표시 |
| #63 | 용어 변경: 코드 → 키 (UI 전체) |

### 배포 정보
- 백엔드: https://worship-sheet.fly.dev (Fly.io, GitHub Actions로 `main` push 시 자동배포)
- 프론트엔드: https://worship-sheet.vercel.app (Vercel, Git 연동으로 `main` push 시 자동배포, Root Directory: `frontend`)
- OCR 서비스: https://worship-sheet-ocr.fly.dev (Fly.io, 4GB 메모리·CPU 2코어 / `auto_stop=false` / `min_machines_running=1` / `OCR_SERVICE_URL` 백엔드 환경변수에 등록됨)
- DB: Supabase

## 다음 세션 시작 가이드
- 새 세션 시작 시 **가장 먼저** `git pull origin main`으로 최신 코드를 받을 것.
- 진행 상황·결정사항이 바뀌면 CLAUDE.md를 즉시 업데이트하고 커밋·푸시할 것.

## 프론트엔드 주요 설계 (v24 이후)
- **테마**: 다크 기본, CSS 변수 기반 토큰 시스템 (`--background`, `--foreground`, `--card` 등)
- **FOUC 방지**: `index.html` 인라인 스크립트로 `dark` 클래스 선 적용
- **Tailwind v4**: `@custom-variant dark (&:where(.dark, .dark *))` + `@theme inline`
- **반응형**: `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`
- **곡 상세**: 악보 이미지 슬라이드 캐러셀 (sheets × files 플랫화), 키보드 ←/→ 네비게이션
- **로컬 개발**: `frontend/.env.local` → `VITE_API_BASE_URL=https://worship-sheet.fly.dev`
