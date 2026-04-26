----------------------------------------
-- routines 테이블에 start_at, end_at column 추가
----------------------------------------

ALTER TABLE public.routines
    ADD COLUMN start_at date,
    ADD COLUMN end_at date;