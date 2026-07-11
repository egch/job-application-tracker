package org.enricogiurin.jobtracker.api.repository;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.enricogiurin.jobtracker.api.jooq.Tables.COMPANY;

import java.time.LocalDate;
import java.util.UUID;

import org.enricogiurin.jobtracker.api.TestcontainersConfiguration;
import org.enricogiurin.jobtracker.api.jooq.enums.ApplicationStatus;
import org.enricogiurin.jobtracker.api.model.Application;
import org.enricogiurin.jobtracker.api.model.CompanyRef;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link ApplicationRepository} against a real Postgres
 * (via Testcontainers), since the repository's value is in its jOOQ queries and
 * the {@code employer -> company} resolution against database constraints. The
 * fixed ids below reference rows seeded by {@code test/db/migration/R__test_data.sql}.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ApplicationRepositoryTest {

    // Ids seeded by R__test_data.sql.
    private static final UUID OWNER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SEEDED_COMPANY_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SEEDED_APPLICATION_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private ApplicationRepository repository;

    @Autowired
    private DSLContext dsl;

    @Test
    void createInsertsNewCompanyWhenEmployerHasNameOnly() {
        Application toCreate = new Application(
                null,
                new CompanyRef(null, "Acme Corp"),
                "Backend Engineer",
                "https://jobs.example.com/acme/backend",
                null,
                LocalDate.of(2026, 7, 1));

        Application created = repository.create(OWNER_ID, toCreate);

        assertThat(created.id()).isNotNull();
        assertThat(created.employer().id()).isNotNull();
        assertThat(created.employer().name()).isEqualTo("Acme Corp");
        assertThat(created.role()).isEqualTo("Backend Engineer");
        assertThat(created.postingUrl()).isEqualTo("https://jobs.example.com/acme/backend");
        assertThat(created.appliedOn()).isEqualTo(LocalDate.of(2026, 7, 1));
        // status defaults to EXPLORING when not supplied.
        assertThat(created.status()).isEqualTo(ApplicationStatus.EXPLORING);
    }

    @Test
    void createReusesExistingCompanyWhenEmployerHasId() {
        UUID companyId = dsl.insertInto(COMPANY)
                .set(COMPANY.NAME, "Existing Ltd")
                .returning(COMPANY.ID)
                .fetchOne()
                .getId();
        int companiesBefore = dsl.fetchCount(COMPANY);

        Application created = repository.create(OWNER_ID,
                new Application(null, new CompanyRef(companyId, null),
                        "Data Engineer", null, ApplicationStatus.APPLIED,
                        LocalDate.of(2026, 6, 15)));

        assertThat(created.employer().id()).isEqualTo(companyId);
        assertThat(created.employer().name()).isEqualTo("Existing Ltd");
        // no duplicate company is created when an id is supplied.
        assertThat(dsl.fetchCount(COMPANY)).isEqualTo(companiesBefore);
    }

    @Test
    void createHonoursExplicitStatus() {
        Application created = repository.create(OWNER_ID,
                new Application(null, new CompanyRef(null, "Interviewing Inc"),
                        "SRE", null, ApplicationStatus.INTERVIEWING, null));

        assertThat(created.status()).isEqualTo(ApplicationStatus.INTERVIEWING);
        assertThat(created.appliedOn()).isNull();
    }

    @Test
    void createRejectsNullEmployer() {
        Application toCreate = new Application(null, null, "Role", null, null, null);

        assertThatThrownBy(() -> repository.create(OWNER_ID, toCreate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing employer company");
    }

    @Test
    void createRejectsEmployerWithNeitherIdNorName() {
        Application toCreate = new Application(
                null, new CompanyRef(null, "  "), "Role", null, null, null);

        assertThatThrownBy(() -> repository.create(OWNER_ID, toCreate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing employer company name");
    }

    @Test
    void createRejectsEmployerReferencingUnknownCompany() {
        UUID unknownCompanyId = UUID.randomUUID();
        Application toCreate = new Application(
                null, new CompanyRef(unknownCompanyId, null), "Role", null, null, null);

        assertThatThrownBy(() -> repository.create(OWNER_ID, toCreate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company not found");
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByIdReturnsCreatedApplication() {
        Application created = repository.create(OWNER_ID,
                new Application(null, new CompanyRef(null, "Findable Co"),
                        "QA Engineer", null, null, null));

        assertThat(repository.findById(created.id())).contains(created);
    }

    @Test
    void findByIdReadsExistingSeededApplication() {
        Application expected = new Application(
                SEEDED_APPLICATION_ID,
                new CompanyRef(SEEDED_COMPANY_ID, "Seeded Company"),
                "Staff Engineer",
                "https://jobs.example.com/seeded/staff",
                ApplicationStatus.INTERVIEWING,
                LocalDate.of(2026, 5, 20));

        assertThat(repository.findById(SEEDED_APPLICATION_ID)).contains(expected);
    }
}
