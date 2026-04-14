----------------------------------------
-- varchar 길이 업데이트
-- refresh_tokens 테이블: device 컬럼 추가
----------------------------------------

-- users 테이블 컬럼 길이 수정
ALTER TABLE public.users
ALTER COLUMN email TYPE varchar(100),
    ALTER COLUMN password TYPE varchar(255),
    ALTER COLUMN nickname TYPE varchar(30),
    ALTER COLUMN age TYPE varchar(10),
    ALTER COLUMN role TYPE varchar(20),
    ALTER COLUMN provider TYPE varchar(20),
    ALTER COLUMN provider_id TYPE varchar(100);

-- categories 테이블 컬럼 길이 수정
ALTER TABLE public.categories
ALTER COLUMN name TYPE varchar(20),
    ALTER COLUMN color_code TYPE varchar(7),
    ALTER COLUMN rt_sum TYPE varchar(10);

-- routines 테이블 컬럼 길이 수정
ALTER TABLE public.routines
ALTER COLUMN title TYPE varchar(30),
    ALTER COLUMN cron_expression TYPE varchar(100);

-- refresh_tokens 테이블 수정
ALTER TABLE public.refresh_tokens
ALTER COLUMN token_value TYPE varchar(5000);

ALTER TABLE public.refresh_tokens
    ADD COLUMN device character varying;

-- ai_logs 테이블 컬럼 길이 수정
ALTER TABLE public.ai_logs
ALTER COLUMN request_type TYPE varchar(30),
    ALTER COLUMN prompt TYPE varchar(5000),
    ALTER COLUMN response TYPE varchar(5000);