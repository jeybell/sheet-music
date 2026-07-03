-- 사용자 역할(권한) 컬럼 추가 — 관리자 모드 (#156)
-- 기본값 USER, 관리 기능(/api/admin/**)은 ADMIN 만 접근한다.
alter table users
    add column role varchar(20) not null default 'USER';

-- 초기 관리자 시드: 소유자 계정(jeybell). 해당 계정이 아직 없으면 아무 행도 갱신되지 않으며,
-- 이후 가입/승격으로 관리자를 지정할 수 있다.
update users set role = 'ADMIN' where username = 'jeybell';
