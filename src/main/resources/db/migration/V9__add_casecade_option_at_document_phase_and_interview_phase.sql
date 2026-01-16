DO $$
    DECLARE
        -- 루프를 돌면서 사용할 변수
        rec record;
        fk_name text;
        query_drop text;
        query_add text;
    BEGIN
        -- ⬇️ [설정 섹션] 여기에 작업할 테이블 정보를 나열합니다.
        -- 형식: SELECT '자식테이블명', 'FK컬럼명', '부모테이블명'
        FOR rec IN
            SELECT 'document_phases' AS t_name, 'applicant_id' AS fk_col, 'applicants' AS p_name
            UNION ALL
            SELECT 'interview_phases' AS t_name, 'applicant_id' AS fk_col, 'applicants' AS p_name
            UNION ALL
            SELECT 'memo' AS t_name, 'document_phase_id' AS fk_col, 'document_phases' AS p_name -- 추가된 부분
            LOOP
            -----------------------------------------------------------
            -- 1. 로직 시작
                RAISE NOTICE '---------------------------------------------------';
                RAISE NOTICE 'Checking Table: % (Parent: %)', rec.t_name, rec.p_name;

                -- 2. 현재 걸려있는 제약조건 이름 찾기
                SELECT constraint_name INTO fk_name
                FROM information_schema.key_column_usage
                WHERE table_schema = 'public'
                  AND table_name = rec.t_name
                  AND column_name = rec.fk_col
                  AND position_in_unique_constraint IS NOT NULL;

                -- 3. 제약조건 처리
                IF fk_name IS NOT NULL THEN
                    RAISE NOTICE 'Found Constraint: %', fk_name;

                    -- 3-1. 기존 제약조건 삭제
                    query_drop := format('ALTER TABLE public.%I DROP CONSTRAINT %I', rec.t_name, fk_name);
                    RAISE NOTICE 'Executing Drop...';
                    EXECUTE query_drop;

                    -- 3-2. CASCADE가 적용된 새 제약조건 추가
                    -- (새 이름은 식별하기 쉽게 _cascade를 붙임)
                    query_add := format(
                            'ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES public.%I (id) ON DELETE CASCADE',
                            rec.t_name,
                            fk_name || '_cascade',
                            rec.fk_col,
                            rec.p_name
                                 );
                    RAISE NOTICE 'Executing Add (CASCADE)...';
                    EXECUTE query_add;

                    RAISE NOTICE '✅ Success: % -> CASCADE applied.', rec.t_name;
                ELSE
                    RAISE NOTICE '⚠️ Warning: No Foreign Key found on table % (Skipping)', rec.t_name;
                END IF;

            END LOOP;

        RAISE NOTICE '---------------------------------------------------';
        RAISE NOTICE '🎉 All tasks completed successfully!';

    EXCEPTION WHEN OTHERS THEN
        -- 에러 발생 시 전체 롤백
        RAISE NOTICE '❌ Error occurred: %', SQLERRM;
        RAISE NOTICE 'ROLLBACK performed automatically.';
        RAISE EXCEPTION '%', SQLERRM;
    END $$;