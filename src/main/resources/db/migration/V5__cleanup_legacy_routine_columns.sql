----------------------------------------
-- routines 테이블 레거시 컬럼 정리
-- V4에서 누락된 cron_expression, state 컬럼 제거
----------------------------------------

ALTER TABLE public.routines
DROP COLUMN IF EXISTS cron_expression,
    DROP COLUMN IF EXISTS state;