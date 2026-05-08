-- 외래키에 ON DELETE CASCADE 추가 --
ALTER TABLE daily_targets
DROP CONSTRAINT IF EXISTS daily_targets_routine_id_fkey;

ALTER TABLE daily_targets
    ADD CONSTRAINT daily_targets_routine_id_fkey
        FOREIGN KEY (routine_id)
            REFERENCES routines(id)
            ON DELETE CASCADE;