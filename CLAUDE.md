# 악보 정리 앱 (sheet-music)

## 프로젝트 개요
예배/공연용 악보를 곡 단위로 관리하고, 셋리스트(콘티)를 구성하는 앱.

## 작업 원칙
- 이슈 발생 시 별도 세션으로 분리해서 진행

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
- **셋리스트**: setlists·setlist_items CRUD, 콘티 화면, 셋리스트 PDF 다운로드(jsPDF)
- **OCR 자동 추출**: EasyOCR Python 마이크로서비스 → Spring Boot 연동. 제목/코드/아티스트/가사/rawText 추출, `@Async` 비동기 처리 후 상세 화면 15초 폴링 반영
- **통합 등록/업로드**: 곡+악보 1단계 통합 등록(OCR 자동입력), 일괄 업로드 화면(`/songs/bulk`, 드래그앤드롭 + 카드별 편집)
- **가사 관리**: songs.lyrics 컬럼, `PATCH /lyrics`, 곡 상세 가사 섹션(OCR 적용 + 직접 편집)
- **통합 검색**: 제목·아티스트·가사 LIKE + 코드 대소문자 무관 LIKE 필터
- **UI**: shadcn-vue 스타일 + Tailwind v4 + 다크/라이트 테마 + 반응형, 악보 이미지 슬라이드 뷰어
- **인프라**: CI/CD 자동배포(GitHub Actions → Fly.io, Vercel Git 연동), Cloudflare R2 연동(STORAGE_TYPE 전환, 현재 local 모드)

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
