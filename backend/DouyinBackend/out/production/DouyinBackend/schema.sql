CREATE TABLE IF NOT EXISTS star_user (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    avatar_index INTEGER NOT NULL,
    verified INTEGER NOT NULL,
    followed INTEGER NOT NULL,
    douyin_id TEXT,
    special_follow INTEGER NOT NULL,
    remark TEXT
);

CREATE INDEX IF NOT EXISTS idx_star_user_name ON star_user(name);
