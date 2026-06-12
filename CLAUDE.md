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
| Song Files | `/api/song-sheets/{id}/files`, `/api/song-files/{id}` |
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

### 🔄 남은 작업
- 없음 (모든 이슈 완료)

### 배포 정보
- 백엔드: https://worship-sheet.fly.dev
- 프론트엔드: https://worship-sheet.vercel.app
- DB: Supabase

## 다음 세션 시작 가이드
새 세션 시작 시 Claude에게 전달할 것:
1. 이 파일 내용
2. 작업할 이슈 번호
3. 현재 코드 상태 (필요시 관련 파일 첨부)

## 작업 우선순위 (권장)
백엔드 마무리 → 인프라 정리 → 프론트엔드 순서 권장
1. #2 → #3 → #4 (백엔드 마무리)
2. #11 → #12 (setlist 기능)
3. #14 → #15 → #16 → #17 (인프라)
4. #6 → #9 → #13 (프론트엔드)
