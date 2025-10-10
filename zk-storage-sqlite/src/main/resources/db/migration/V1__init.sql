-- Schema v1 (SQLite). Foreign key *enforcement* is enabled per-connection in code.

CREATE TABLE IF NOT EXISTS note (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  title        TEXT NOT NULL,
  body         TEXT NOT NULL,
  created_at   TEXT NOT NULL,
  modified_at  TEXT NOT NULL,
  rating       INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS keyword (
  name TEXT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS note_keyword (
  note_id INTEGER NOT NULL,
  name    TEXT NOT NULL,
  PRIMARY KEY (note_id, name),
  FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE CASCADE,
  FOREIGN KEY (name)    REFERENCES keyword(name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS link (
  src_id INTEGER NOT NULL,
  dst_id INTEGER NOT NULL,
  PRIMARY KEY (src_id, dst_id),
  FOREIGN KEY (src_id) REFERENCES note(id) ON DELETE CASCADE,
  FOREIGN KEY (dst_id) REFERENCES note(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS seq_edge (
  parent_id INTEGER NOT NULL,
  child_id  INTEGER NOT NULL,
  ord       INTEGER NOT NULL,
  PRIMARY KEY (parent_id, child_id),
  FOREIGN KEY (parent_id) REFERENCES note(id) ON DELETE CASCADE,
  FOREIGN KEY (child_id)  REFERENCES note(id) ON DELETE CASCADE
);
