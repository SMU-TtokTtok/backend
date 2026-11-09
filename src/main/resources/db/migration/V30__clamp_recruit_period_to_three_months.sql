-- #456: 모집 기간은 최대 3개월이다 (마감일 <= 시작일 + 3개월).
-- 이 규칙보다 긴 기존 폼의 마감일을 시작일 + 3개월로 맞춘다. 운영자 승인 완료.
-- 맞춘 마감일이 이미 지났다면 그 폼은 바로 모집 마감 상태가 된다.
-- 데이터만 바꾸므로 이전 버전 코드와 함께 실행돼도 안전하다.
UPDATE applyforms
SET apply_end_date = (apply_start_date + INTERVAL '3 months')::date
WHERE apply_end_date > (apply_start_date + INTERVAL '3 months')::date;
