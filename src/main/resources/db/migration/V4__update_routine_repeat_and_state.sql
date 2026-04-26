----------------------------------------
-- routines 테이블 반복 방식 변경 및 state 컬럼 제거
----------------------------------------

-- 1. 기존 컬럼 제거
ALTER TABLE public.routines
    DROP COLUMN cron_expression,
    DROP COLUMN state;

-- 2. 반복 관련 컬럼 추가
ALTER TABLE public.routines
    ADD COLUMN repeat_type     varchar(10) NOT NULL DEFAULT 'NONE',
    ADD COLUMN repeat_interval integer,
    ADD COLUMN repeat_unit     varchar(10),
    ADD COLUMN repeat_days     varchar(50);

-- 3. start_at NOT NULL 변경
UPDATE public.routines SET start_at = CURRENT_DATE WHERE start_at IS NULL;
ALTER TABLE public.routines ALTER COLUMN start_at SET NOT NULL;

-- 4. category_id NOT NULL 변경
ALTER TABLE public.routines ALTER COLUMN category_id SET NOT NULL;
