-- #456: 한 사람은 한 폼에 임시 지원서를 하나만 가진다. DB에서도 중복 저장을 막는다.
-- 임시저장 중복 방지 코드(feat/#456)가 먼저 배포된 다음 릴리스에서만 적용한다 (AGENTS.md: 기존 데이터에 UNIQUE 추가는 릴리스 분리).
-- 기존 중복은 (form_id, user_email)마다 가장 최근에 저장한 행 하나만 남기고 지운다. 운영자 승인 완료.
-- 지워진 행의 임시 첨부는 MinIO 만료 규칙(temp-applicants/, 14일)이 정리한다.
DELETE FROM temp_applicants
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY form_id, user_email
                   ORDER BY updated_at DESC NULLS LAST, id DESC
               ) AS row_rank
        FROM temp_applicants
    ) ranked
    WHERE row_rank > 1
);

ALTER TABLE temp_applicants
    ADD CONSTRAINT uk_temp_applicants_form_user
    UNIQUE (form_id, user_email);
