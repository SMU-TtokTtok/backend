DO $$
    DECLARE
        -- 처리할 테이블 목록 (배열에 테이블 이름을 나열)
        target_tables text[] := ARRAY['document_phases', 'interview_phases'];

        -- 공통 설정
        target_column text := 'applicant_id';   -- 자식 테이블의 FK 컬럼명
        parent_table text := 'applicants';      -- 부모 테이블 이름
        parent_column text := 'id';             -- 부모 테이블의 PK 컬럼명

        -- 내부 변수
        t_name text;
        fk_name text;
        query_drop text;
        query_add text;
    BEGIN
        -- 테이블 목록을 순회하며 작업 수행
        FOREACH t_name IN ARRAY target_tables
            LOOP
                -- 1. 현재 걸려있는 제약조건 이름 찾기
                SELECT constraint_name INTO fk_name
                FROM information_schema.key_column_usage
                WHERE table_schema = 'public'
                  AND table_name = t_name
                  AND column_name = target_column
                  AND position_in_unique_constraint IS NOT NULL;

                RAISE NOTICE '---------------------------------------------------';
                RAISE NOTICE 'Checking Table: %', t_name;

                -- 2. 제약조건이 존재하는지 확인 및 실행
                IF fk_name IS NOT NULL THEN
                    RAISE NOTICE 'Found Constraint: %', fk_name;

                    -- 3. 기존 제약조건 삭제
                    query_drop := format('ALTER TABLE public.%I DROP CONSTRAINT %I', t_name, fk_name);
                    RAISE NOTICE 'Executing Drop...';
                    EXECUTE query_drop;

                    -- 4. CASCADE가 적용된 새 제약조건 추가 (이름 뒤에 _cascade 붙임)
                    query_add := format(
                            'ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES public.%I (%I) ON DELETE CASCADE',
                            t_name,
                            fk_name || '_cascade',
                            target_column,
                            parent_table,
                            parent_column
                                 );
                    RAISE NOTICE 'Executing Add (CASCADE)...';
                    EXECUTE query_add;

                    RAISE NOTICE '✅ Success: % -> CASCADE applied.', t_name;
                ELSE
                    RAISE NOTICE '⚠️ Warning: No Foreign Key found on table % (Skipping)', t_name;
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