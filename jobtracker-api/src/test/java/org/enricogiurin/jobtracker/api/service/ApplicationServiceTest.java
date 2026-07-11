package org.enricogiurin.jobtracker.api.service;

/*-
 * #%L
 * %%
 * Copyright (C) 2026 Enrico Giurin
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.enricogiurin.jobtracker.api.TestcontainersConfiguration;
import org.enricogiurin.jobtracker.api.jooq.enums.ApplicationStatus;
import org.enricogiurin.jobtracker.api.model.Application;
import org.enricogiurin.jobtracker.api.model.CompanyRef;
import org.enricogiurin.jobtracker.api.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link ApplicationService} against a real Postgres (via
 * Testcontainers), driving the real {@link ApplicationRepository} — no mocks, so
 * the service is exercised end-to-end down to SQL. The fixed ids below reference
 * rows seeded by {@code test/db/migration/R__test_data.sql}.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ApplicationServiceTest {

    // Ids seeded by R__test_data.sql.
    private static final UUID OWNER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SEEDED_COMPANY_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SEEDED_APPLICATION_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private ApplicationService service;

    @Test
    void findByIdReadsExistingSeededApplication() {
        Application expected = new Application(
                SEEDED_APPLICATION_ID,
                new CompanyRef(SEEDED_COMPANY_ID, "Seeded Company"),
                "Staff Engineer",
                "https://jobs.example.com/seeded/staff",
                ApplicationStatus.INTERVIEWING,
                LocalDate.of(2026, 5, 20));

        assertThat(service.findById(SEEDED_APPLICATION_ID)).contains(expected);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(service.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void createPersistsApplicationAndItIsRetrievableThroughTheService() {
        Application toCreate = new Application(
                null,
                new CompanyRef(null, "Acme Corp"),
                "Backend Engineer",
                "https://jobs.example.com/acme/backend",
                ApplicationStatus.APPLIED,
                LocalDate.of(2026, 7, 1));

        Application created = service.create(OWNER_ID, toCreate);

        assertThat(created.id()).isNotNull();
        assertThat(created.employer().id()).isNotNull();
        assertThat(created.employer().name()).isEqualTo("Acme Corp");
        assertThat(created.role()).isEqualTo("Backend Engineer");
        assertThat(created.status()).isEqualTo(ApplicationStatus.APPLIED);
        // round-trips: what create returned is what a later read returns.
        assertThat(service.findById(created.id())).contains(created);
    }

    @Test
    void createReusesTheSeededCompanyWhenItsIdIsSupplied() {
        Application created = service.create(OWNER_ID,
                new Application(null, new CompanyRef(SEEDED_COMPANY_ID, null),
                        "Data Engineer", null, ApplicationStatus.APPLIED,
                        LocalDate.of(2026, 6, 15)));

        assertThat(created.employer().id()).isEqualTo(SEEDED_COMPANY_ID);
        assertThat(created.employer().name()).isEqualTo("Seeded Company");
    }
}
