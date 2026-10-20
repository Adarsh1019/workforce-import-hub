CREATE TABLE app_users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL
);

CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE import_jobs (
    id                 BIGSERIAL PRIMARY KEY,
    file_name          VARCHAR(255) NOT NULL,
    stored_path        VARCHAR(500) NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    total_records      INT          NOT NULL DEFAULT 0,
    processed_records  INT          NOT NULL DEFAULT 0,
    success_count      INT          NOT NULL DEFAULT 0,
    failure_count      INT          NOT NULL DEFAULT 0,
    created_by         VARCHAR(50)  NOT NULL,
    batch_execution_id BIGINT,
    error_message      TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    started_at         TIMESTAMPTZ,
    finished_at        TIMESTAMPTZ
);

CREATE TABLE employees (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120)   NOT NULL,
    email         VARCHAR(160)   NOT NULL,
    department_id BIGINT         NOT NULL REFERENCES departments(id),
    role          VARCHAR(120)   NOT NULL,
    salary        NUMERIC(12, 2) NOT NULL CHECK (salary >= 0),
    joining_date  DATE           NOT NULL,
    import_job_id BIGINT REFERENCES import_jobs(id) ON DELETE SET NULL
);
-- Duplicate prevention: one employee per email (case-insensitive)
CREATE UNIQUE INDEX ux_employees_email ON employees (lower(email));
CREATE INDEX ix_employees_department ON employees (department_id);
CREATE INDEX ix_employees_import_job ON employees (import_job_id);

CREATE TABLE import_errors (
    id            BIGSERIAL PRIMARY KEY,
    import_job_id BIGINT NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    line_number   INT    NOT NULL,
    raw_line      TEXT,
    reason        TEXT   NOT NULL,
    UNIQUE (import_job_id, line_number)
);
