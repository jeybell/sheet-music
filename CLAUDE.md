# 악보 정리 앱 (sheet-music)

## 프로젝트 개요
예배/공연용 악보를 곡 단위로 관리하고, 셋리스트(콘티)를 구성하는 앱.

## 개발자 프로필
- 12년차 풀스택 개발자 (SI, SM 경력 다수 / 정부부처, 연구원 프로젝트 참여)
- 목표: PM / 아키텍처로 성장
- 프리랜서 1인 사업 준비 중
- 바이브 코딩 방식으로 작업 (Claude Code 활용)
- 주력 스택: Java/Spring, JavaScript, Angular / Vue(입문)
- 개인 OS: Mac / 회사 OS: Windows

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

## 현재 진행 상황

### ✅ 완료
- ERD 설계
- API 설계 (README 문서화)
- songs, song_sheets, song_files 기본 CRUD 구현
- #2 `song_files` 삭제 처리 구현 보강
- #3 곡 검색 API 구현
- #4 GlobalExceptionHandler 정리
- #11 setlists CRUD 구현
- #12 setlist_items 구현
- #14 환경변수 정리
- #15 Docker Compose 정리
- #16 Cloudflare R2 연동 (STORAGE_TYPE 환경변수로 전환, 현재 local 모드)
- #17 배포 완료 (백엔드: Fly.io / 프론트엔드: Vercel / DB: Supabase)
- #6 공통 Layout 구성 (헤더 네비게이션, 글로벌 스타일)
- #9 곡 상세 화면 구현 (수정/삭제, 악보 버전/파일 관리)
- #13 콘티 화면 구현 (셋리스트 목록/상세, 곡 추가/삭제)

### ✅ 추가 완료
- #20 전체 디자인 리뉴얼 (shadcn-vue 스타일 + Tailwind CSS v4 + lucide 아이콘)
- #21 악보 뷰어 및 셋리스트 PDF 다운로드 (이미지 미리보기, 전체화면 뷰어 모달, jsPDF 다운로드)
- #22 악보 첨부파일 뷰어 파일 깨짐 현상 수정 (CORS 헤더 노출 추가, Fly 업로드 볼륨 마운트로 재배포 시 파일 유실 방지, `/view`·`/download` 엔드포인트 분리)
- CI/CD 자동배포 구성 (GitHub Actions → Fly.io 자동배포, Vercel Git 연동 → 프론트 자동배포). `main` push만 하면 백엔드/프론트 모두 자동 반영됨
- #24 UI 벤치마킹 기반 디자인 개선 (다크/라이트 테마, 반응형, 곡 검색, 악보 이미지 슬라이드 뷰어)
- #26 OCR 자동 메타데이터 추출 (EasyOCR Python 마이크로서비스 → Spring Boot 연동, 제목/코드/키 자동 세팅 / Fly.io `worship-sheet-ocr` 배포 완료)
- #30 곡+악보 1단계 통합 등록 (이미지 선택 → OCR ~20초 분석 → 제목·코드·아티스트 자동입력 → 곡·악보·파일 한 번에 저장)
- #29 곡 등록 유효성 검사 (제목 중복 체크 → 409 / 공백 제거 후 비교 / 프론트 에러 메시지 처리 포함)
- OCR 비동기 처리 (파일 업로드 즉시 응답, `@Async` 백그라운드 OCR 실행 → `song_files` DB 저장 / 상세 화면 15초 폴링으로 자동 반영)
- OCR 서비스 안정화 (메모리 4GB·CPU 2코어 / `auto_stop_machines=false` / `min_machines_running=1` / readTimeout 10분)
- OCR 추출 항목: 제목 / 코드(첫 코드 자동추론) / 아티스트(한글+영어 혼합) / 가사 / rawText (`song_files`에 저장)
- UI 정리: 작곡가 필드 제거(아티스트로 통합) / '키' 레이블 → '코드'로 전체 변경

### 🔄 남은 작업 (이슈)
- #25 데이터 일괄 적재 (기존 악보 이미지 등록)
- #27 가사 관리 (OCR 추출 + 직접 입력 + 가사 검색)
- #28 통합 검색 (제목·아티스트·코드·가사 통합 검색, #3/#26/#27 연계)

### 배포 정보
- 백엔드: https://worship-sheet.fly.dev (Fly.io, GitHub Actions로 `main` push 시 자동배포)
- 프론트엔드: https://worship-sheet.vercel.app (Vercel, Git 연동으로 `main` push 시 자동배포, Root Directory: `frontend`)
- OCR 서비스: https://worship-sheet-ocr.fly.dev (Fly.io, 4GB 메모리·CPU 2코어 / `auto_stop=false` / `min_machines_running=1` / `OCR_SERVICE_URL` 백엔드 환경변수에 등록됨)
- DB: Supabase

## 다음 세션 시작 가이드
새 세션을 시작하면 **반드시 가장 먼저** `git pull origin main`을 실행해 최신 코드를 받아온 뒤 작업을 시작할 것.

이슈 등록·완료·결정사항 등 진행 상황이 바뀔 때마다 CLAUDE.md를 즉시 업데이트하고 커밋·푸시할 것.

새 세션 시작 시 Claude에게 전달할 것:
1. 이 파일 내용
2. 작업할 이슈 번호
3. 현재 코드 상태 (필요시 관련 파일 첨부)

## 프론트엔드 주요 설계 (v24 이후)
- **테마**: 다크 기본, CSS 변수 기반 토큰 시스템 (`--background`, `--foreground`, `--card` 등)
- **FOUC 방지**: `index.html` 인라인 스크립트로 `dark` 클래스 선 적용
- **Tailwind v4**: `@custom-variant dark (&:where(.dark, .dark *))` + `@theme inline`
- **반응형**: `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`
- **곡 상세**: 악보 이미지 슬라이드 캐러셀 (sheets × files 플랫화), 키보드 ←/→ 네비게이션
- **로컬 개발**: `frontend/.env.local` → `VITE_API_BASE_URL=https://worship-sheet.fly.dev`

## 작업 우선순위 (권장)
1. #25 데이터 일괄 적재
2. #27 가사 관리
3. #28 통합 검색
