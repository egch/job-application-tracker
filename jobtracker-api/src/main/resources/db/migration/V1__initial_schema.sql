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
-- This is a denormalised snapshot; the full history lives in application_event.
CREATE TYPE application_status AS ENUM (
    'EXPLORING',
    'APPLIED',
    'INTERVIEWING',
    'OFFER',
    'REJECTED',
    'WITHDRAWN'
);

-- Nature of the contract on offer.
CREATE TYPE employment_type AS ENUM (
    'PERMANENT',
    'FIXED_TERM',
    'FREELANCE',
    'INTERNSHIP',
    'TEMP'
);

-- Kind of entry in an application's personal timeline.
CREATE TYPE application_event_type AS ENUM (
    'APPLIED',
    'REPLY_RECEIVED',
    'INTERVIEW_SCHEDULED',
    'INTERVIEW',
    'ON_HOLD',
    'RESUMED',
    'OFFER',
    'REJECTED',
    'WITHDRAWN',
    'NOTE'
);

-- Where a user's uploaded files are physically stored. Resolved per user by
-- the StorageService layer; LOCAL is the default free backend.
CREATE TYPE storage_backend AS ENUM (
    'LOCAL',
    'AZURE',
    'GOOGLE_DRIVE'
);

-- ---------------------------------------------------------------------------
-- Tables (columns only — keys and indexes live in V2__constraints.sql).
-- ---------------------------------------------------------------------------

-- app_user — owner of every record. files_enabled is the per-user entitlement
-- gating whether this user may archive attachments at all; which backend the
-- bytes actually land in is a deployment-wide choice, not a per-user one.
CREATE TABLE app_user (
    id            UUID NOT NULL DEFAULT gen_random_uuid(),
    email         TEXT NOT NULL,
    full_name     TEXT,
    files_enabled BOOLEAN NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- company — any organisation involved in an application: the hiring employer
-- and/or a recruitment agency. The same row can act as either, depending on
-- which application references it.
CREATE TABLE company (
    id          UUID NOT NULL DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    address     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- application — the core record; one row per opportunity. Carries a current
-- status for a quick pipeline view; the blow-by-blow lives in
-- application_event.
CREATE TABLE application (
    id               UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL,

    -- the organisations involved
    employer_id      UUID NOT NULL,             -- the hiring company
    agency_id        UUID,                      -- intermediary; null = applied directly

    -- the position
    role             TEXT,
    job_location     TEXT,
    remote           BOOLEAN NOT NULL DEFAULT false,
    posting_url      TEXT,

    -- contact at the company
    contact_person   TEXT,
    contact_phone    TEXT,

    -- the offer
    employment_type  employment_type,
    workload_percent SMALLINT,                  -- null = unspecified, 100 = full-time

    -- lifecycle
    status           application_status NOT NULL DEFAULT 'EXPLORING',
    on_hold_since    TIMESTAMPTZ,               -- non-null = currently on hold (orthogonal to status)
    applied_on       DATE,                      -- null while EXPLORING; set once submitted

    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- application_event — the personal timeline of steps/interactions for an
-- application (applied, reply received, interview scheduled, on hold, ...).
-- Source of truth for history; application.status mirrors the latest state.
CREATE TABLE application_event (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL,
    event_type      application_event_type NOT NULL,
    event_date      DATE NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- attachment — a file (email, screenshot, image, PDF) evidencing an
-- application, optionally pinned to a specific timeline event. The bytes live
-- in the storage backend; only metadata + a lookup key are stored here.
CREATE TABLE attachment (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL,
    event_id        UUID,                        -- optional: the event this file belongs to
    filename        TEXT NOT NULL,
    content_type    TEXT,
    size_bytes      BIGINT,
    storage_backend storage_backend NOT NULL,     -- backend the bytes were written to
    storage_key     TEXT NOT NULL,                -- key/path within that backend
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
