CREATE TABLE IF NOT EXISTS nutrition_plan (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 60),
    name_key TEXT NOT NULL UNIQUE,
    effective_from TEXT NOT NULL CHECK (
        effective_from GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
    ),
    status TEXT NOT NULL CHECK (
        status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'NEEDS_REVIEW')
    )
);
