---
-- #%L
-- %%
-- Copyright (C) 2026 Enrico Giurin
-- %%
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- #L%
---
-- ---------------------------------------------------------------------------
-- Enum types
-- ---------------------------------------------------------------------------

-- Current stage of an application in the pipeline. EXPLORING means found but
-- not submitted yet; the remaining states cover the lifecycle through to a
-- terminal outcome (they made an offer, they rejected, or you withdrew).
CREATE TYPE application_status AS ENUM (
    'EXPLORING',
    'APPLIED',
    'INTERVIEWING',
    'OFFER',
    'REJECTED',
    'WITHDRAWN'
);

-- Kind of step in an application's timeline. Repeatable: a first and second
-- interview are simply two INTERVIEW rows on different dates.
CREATE TYPE application_event_type AS ENUM (
    'APPLIED',
    'REPLY_RECEIVED',
    'INTERVIEW',
    'OFFER',
    'REJECTED',
    'WITHDRAWN',
    'NOTE'
);

-- Where a user's uploaded files are physically stored. Resolved by the
-- StorageService layer; LOCAL is the default free backend.
CREATE TYPE storage_backend AS ENUM (
    'LOCAL',
    'AZURE',
    'GOOGLE_DRIVE'
);

-- ---------------------------------------------------------------------------
-- Tables (columns only — keys and indexes live in V2__constraints.sql).
-- ---------------------------------------------------------------------------

-- app_user — owner of every record.
CREATE TABLE app_user (
    id          UUID NOT NULL DEFAULT gen_random_uuid(),
    email       TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- company — the hiring organisation an application is directed at.
CREATE TABLE company (
    id          UUID NOT NULL DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- application — the core record; one row per opportunity, carrying its current
-- status through the pipeline.
CREATE TABLE application (
    id           UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    employer_id  UUID NOT NULL,
    role         TEXT,
    posting_url  TEXT,
    status       application_status NOT NULL DEFAULT 'EXPLORING',
    applied_on   DATE,                     -- null while EXPLORING; set once submitted
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- application_event — the personal timeline of steps for an application
-- (applied, reply received, interview, ...). Source of truth for history;
-- application.status mirrors the latest state for quick pipeline views.
CREATE TABLE application_event (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL,
    event_type      application_event_type NOT NULL,
    event_date      DATE NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- attachment — a file (screenshot, PDF, email, ...) evidencing an application,
-- optionally pinned to a specific timeline event. The bytes live in the
-- storage backend; only metadata + a lookup key here.
CREATE TABLE attachment (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL,
    event_id        UUID,                          -- optional: the step this file belongs to
    filename        TEXT NOT NULL,
    content_type    TEXT,
    size_bytes      BIGINT,
    storage_backend storage_backend NOT NULL,      -- backend the bytes were written to
    storage_key     TEXT NOT NULL,                 -- key/path within that backend
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
