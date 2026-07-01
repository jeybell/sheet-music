-- #86 콘티 등록/수정 폼에서 예배종류 필드 제거
-- 제목(title)과 역할이 겹쳐 service_type 컬럼을 제거한다. 기존 값은 폐기.
ALTER TABLE setlists DROP COLUMN service_type;
