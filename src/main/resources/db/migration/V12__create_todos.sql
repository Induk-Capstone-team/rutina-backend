CREATE TABLE todos (
                       id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       todo_date DATE NOT NULL,
                       todo_time TIME,
                       content VARCHAR(30) NOT NULL,
                       completed BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                       CONSTRAINT fk_todos_user
                           FOREIGN KEY (user_id)
                               REFERENCES users(id)
                               ON DELETE CASCADE
);

CREATE INDEX idx_todos_user_date ON todos(user_id, todo_date);