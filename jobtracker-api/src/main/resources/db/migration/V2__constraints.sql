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
-- Primary keys, unique keys, foreign keys, check constraints and indexes for
-- the tables created in V1__initial_schema.sql.
-- ---------------------------------------------------------------------------

-- Primary keys ---------------------------------------------------------------

ALTER TABLE app_user
    ADD CONSTRAINT pk_app_user PRIMARY KEY (id);

ALTER TABLE company
    ADD CONSTRAINT pk_company PRIMARY KEY (id);

ALTER TABLE application
    ADD CONSTRAINT pk_application PRIMARY KEY (id);

ALTER TABLE application_event
    ADD CONSTRAINT pk_application_event PRIMARY KEY (id);

ALTER TABLE attachment
    ADD CONSTRAINT pk_attachment PRIMARY KEY (id);

-- Unique keys ----------------------------------------------------------------

ALTER TABLE app_user
    ADD CONSTRAINT uq_app_user_email UNIQUE (email);

ALTER TABLE company
    ADD CONSTRAINT uq_company_name UNIQUE (name);

-- Foreign keys ---------------------------------------------------------------

ALTER TABLE application
    ADD CONSTRAINT fk_application_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE;

ALTER TABLE application
    ADD CONSTRAINT fk_application_employer
        FOREIGN KEY (employer_id) REFERENCES company (id);

ALTER TABLE application
    ADD CONSTRAINT fk_application_agency
        FOREIGN KEY (agency_id) REFERENCES company (id);

ALTER TABLE application_event
    ADD CONSTRAINT fk_application_event_application
        FOREIGN KEY (application_id) REFERENCES application (id) ON DELETE CASCADE;

ALTER TABLE attachment
    ADD CONSTRAINT fk_attachment_application
        FOREIGN KEY (application_id) REFERENCES application (id) ON DELETE CASCADE;

-- If the event a file was pinned to is deleted, keep the file attached to the
-- application; just detach it from the (now gone) event.
ALTER TABLE attachment
    ADD CONSTRAINT fk_attachment_event
        FOREIGN KEY (event_id) REFERENCES application_event (id) ON DELETE SET NULL;

-- Check constraints ----------------------------------------------------------

-- Workload, when given, is a percentage in 1..100.
ALTER TABLE application
    ADD CONSTRAINT ck_application_workload_percent
        CHECK (workload_percent IS NULL OR workload_percent BETWEEN 1 AND 100);

-- Indexes --------------------------------------------------------------------

-- Pipeline / Kanban view: a user's applications grouped by stage.
CREATE INDEX idx_application_user_status ON application (user_id, status);
-- Per-user listing ordered by application date.
CREATE INDEX idx_application_user_applied_on ON application (user_id, applied_on);
-- Lookups / joins by organisation.
CREATE INDEX idx_application_employer ON application (employer_id);
CREATE INDEX idx_application_agency ON application (agency_id);
-- Timeline of an application, ordered by date.
CREATE INDEX idx_application_event_application ON application_event (application_id, event_date);
-- Attachments of an application / of a specific event.
CREATE INDEX idx_attachment_application ON attachment (application_id);
CREATE INDEX idx_attachment_event ON attachment (event_id);
