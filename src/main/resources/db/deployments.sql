-- The deployment history of the server mode. Times are ISO-8601 instants in UTC.
CREATE TABLE IF NOT EXISTS deployments (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    finished_at   TEXT    NOT NULL,
    outcome       TEXT    NOT NULL,
    trigger_name  TEXT    NOT NULL,
    attempts      INTEGER NOT NULL,
    source_commit TEXT,
    target_commit TEXT,
    cause         TEXT
);
