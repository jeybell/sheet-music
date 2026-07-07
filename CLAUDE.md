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
- **통합 등록/업로드**: 곡+악보 1단계 통합 등록, 일괄 업로드 화면(`/songs/bulk`, 드래그앤드롭 + 카드별 편집)
- **가사 관리**: songs.lyrics 컬럼, `PATCH /lyrics`, 곡 상세 가사 섹션(직접 편집)
- **태그**: song_tags 테이블(`@ElementCollection EAGER+SUBSELECT`), 태그 칩 입력/필터
- **링크**: song_links 테이블, 멀티 플랫폼(YouTube/Spotify/Melon 등) 자동 감지, YouTube 임베드 토글
- **통합 검색**: 제목·아티스트·가사 LIKE + 키 대소문자 무관 LIKE 필터 + 태그 필터
- **로딩 UX**: axios 인터셉터 전역 로딩 바 + 콘텐츠 딤 처리 (`useHttpLoading`)
- **사용자 인증**: Spring Security + JWT 자체 구현(`/api/auth/register`, `/api/auth/login`), 30일 만료 토큰. `/api/auth/**`, `GET /api/setlists/share/**`, `GET /api/song-files/*/view`·`/download`만 공개, 나머지 API는 인증 필요. 프론트엔드는 `authStore`(Pinia) + axios 인터셉터로 토큰 첨부, 라우터 가드로 미인증 시 `/login` 리다이렉트, 401 응답 시 자동 로그아웃
- **관리자 모드**(#156): `users.role`(USER/ADMIN) 역할 기반 권한. JWT 필터가 매 요청 시 DB에서 role 조회해 `ROLE_ADMIN`/`ROLE_USER` authority 주입(지정/해제 즉시 반영, 게스트는 관리자 불가). `/api/admin/**` → `hasRole('ADMIN')`. 관리 기능: 사용자 관리(목록/권한 지정·해제/삭제, 본인 대상 작업 차단), 콘텐츠 휴지통(삭제된 곡·콘티 조회·복구), 기능요청 상태변경·삭제. 프론트: `authStore.isAdmin`, `/admin` 라우트 가드 + 관리자에게만 보이는 진입 메뉴. 초기 관리자 시드: `jeybell`
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
| V10 | setlists.service_type 컬럼 제거 |
| V11 | users 테이블 (사용자 인증) |
| V15 | song_files.ocr_* 컬럼 제거 (OCR 기능 완전 제거, #142) |
| V16 | setlist_items 스테일 song_sheet_id 정리 (#154) |
| V17 | users.role 컬럼 (관리자 모드, #156) |

## 다음 작업 예정 이슈
> #80~#87(악보 목록 정렬·태그·페이지네이션·건수, 일괄 업로드 OCR 분리, 콘티 등록 UI·예배종류 제거), #85(콘티 목록 성능), #110(콘티 순서변경 터치 지원), #77(사용자 인증 도입)은 처리 완료.

| 번호 | 제목 |
|------|------|
| #106 | 콘티 곡 선택 모달에서 악보 수정 기능 추가 |
| #107 | 공유 링크 뷰어에서 앱 홈으로 이동하는 기능 제공 |
| #108 | 콘티 목록 캘린더 뷰 (날짜별 콘티 표시) |
| #109 | 악보 이미지에 스타일러스(애플펜슬) 필기 메모 (벡터 저장) |
| #75 | 콘티 아이템 키 전조 메모 및 표시 — 관련 #35 |
| #74 | 콘티 복사 (템플릿으로 재사용) — 관련 #37 |
| #73 | 콘티 즐겨찾기 / 최근 사용 콘티 빠른 접근 (#77 완료로 진행 가능) |
| #72 | 콘티 진행 모드 (프레젠테이션 뷰) — 관련 #33 |
| #71 | 콘티 QR코드 공유 |
| #70 | 콘티 공유 링크 뷰어 개선 (악보 포함 풀스크린 공유) |
| #38 | 셋리스트 연주 이력 기록 |

### 배포 정보
- 백엔드: https://worship-sheet.fly.dev (Fly.io, GitHub Actions로 `main` push 시 자동배포)
- 프론트엔드: https://worship-sheet.vercel.app (Vercel, Git 연동으로 `main` push 시 자동배포, Root Directory: `frontend`)
- DB: Supabase
- OCR 자동 추출 기능은 사용 빈도 대비 Fly.io 상시 가동 비용(24시간 4GB/2CPU) 부담이 커서 #142로 완전히 제거함 (`ocr-service` 앱도 폐기 대상 — Fly.io 대시보드에서 별도로 앱 삭제 필요).
- ⚠️ 사용자 인증(#77) 배포 후 Fly.io 백엔드에 `JWT_SECRET` 환경변수(32바이트 이상 랜덤 문자열)를 반드시 설정할 것. 미설정 시 기본값(dev-only)이 사용되어 보안상 위험함

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

## 로컬 백엔드 실행
- **기본**: `JAVA_HOME=<jdk17 경로> ./gradlew bootRun` → 저장소 루트에 `.env`가 없으면 `application.yml` 기본값(로컬 Postgres, `localhost:5432/sheet_music`) 사용.
- **운영(Supabase) DB에 붙여서 실행하고 싶을 때**: 저장소 루트 `.env.example`을 `.env`로 복사(gitignore 처리, 커밋 안 됨) → `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`를 Supabase 값으로 채움. `build.gradle`의 `bootRun` 태스크가 이 `.env`를 자동으로 읽어 프로세스 환경변수로 주입하므로 별도 플래그 없이 그냥 실행하면 됨:
  ```
  JAVA_HOME=<jdk17 경로> ./gradlew bootRun
  ```
  Docker Compose도 같은 `.env`를 쓰므로 파일 하나로 두 실행 방식 모두 커버됨.
  ⚠️ 운영 DB에 쓰기까지 그대로 반영됨(회원가입·곡 등록/삭제·콘티 편집 등). 순수 조회만 필요하면 대신 읽기전용 MCP(`.mcp.json`의 `supabase-db`, `SUPABASE_READONLY_URL`)를 사용할 것.
