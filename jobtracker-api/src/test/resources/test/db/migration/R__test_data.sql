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
-- Shared test data: a Flyway repeatable migration applied at test context
-- startup (this location is added to spring.flyway.locations for tests only).
-- One owner, one company and one application, all with fixed ids so tests can
-- look rows up by a known id and assert the full read view.

INSERT INTO app_user (id, email) VALUES
    ('11111111-1111-1111-1111-111111111111', 'seed-owner@example.com');

INSERT INTO company (id, name) VALUES
    ('22222222-2222-2222-2222-222222222222', 'Seeded Company');

INSERT INTO application (id, user_id, employer_id, role, posting_url, status, applied_on) VALUES
    ('33333333-3333-3333-3333-333333333333',
     '11111111-1111-1111-1111-111111111111',
     '22222222-2222-2222-2222-222222222222',
     'Staff Engineer',
     'https://jobs.example.com/seeded/staff',
     'INTERVIEWING',
     DATE '2026-05-20');
