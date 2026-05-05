-- 카테고리 테이블에 hidden 필드 추가 --

ALTER TABLE categories
    ADD COLUMN hidden boolean NOT NULL DEFAULT false;