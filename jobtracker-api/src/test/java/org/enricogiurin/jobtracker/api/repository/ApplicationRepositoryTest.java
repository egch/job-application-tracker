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
import static org.enricogiurin.jobtracker.api.jooq.Tables.APP_USER;
import static org.enricogiurin.jobtracker.api.jooq.Tables.COMPANY;

import java.time.LocalDate;
import java.util.UUID;

import org.enricogiurin.jobtracker.api.TestcontainersConfiguration;
import org.enricogiurin.jobtracker.api.jooq.enums.ApplicationStatus;
import org.enricogiurin.jobtracker.api.model.Application;
import org.enricogiurin.jobtracker.api.model.CompanyRef;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jooq.test.autoconfigure.JooqTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for {@link ApplicationRepository} against a real Postgres
 * (via Testcontainers), since the repository's value is in its jOOQ queries and
 * the {@code employer -> company} resolution against database constraints.
 *
 * <p>{@code @JooqTest} wraps each test in a transaction that rolls back, so the
 * schema stays clean between tests without manual cleanup.
 */
@JooqTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ApplicationRepository.class})
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository repository;

    @Autowired
    private DSLContext dsl;

    private UUID ownerId;

    @BeforeEach
    void seedOwner() {
        ownerId = dsl.insertInto(APP_USER)
                .set(APP_USER.EMAIL, "owner-" + UUID.randomUUID() + "@example.com")
                .returning(APP_USER.ID)
                .fetchOne()
                .getId();
    }

    @Test
    void createInsertsNewCompanyWhenEmployerHasNameOnly() {
        Application toCreate = new Application(
                null,
                new CompanyRef(null, "Acme Corp"),
                "Backend Engineer",
                "https://jobs.example.com/acme/backend",
                null,
                LocalDate.of(2026, 7, 1));

        Application created = repository.create(ownerId, toCreate);

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

        Application created = repository.create(ownerId,
                new Application(null, new CompanyRef(companyId, null),
                        "Data Engineer", null, ApplicationStatus.APPLIED,
                        LocalDate.of(2026, 6, 15)));

        assertThat(created.employer().id()).isEqualTo(companyId);
        assertThat(created.employer().name()).isEqualTo("Existing Ltd");
        // no duplicate company is created when an id is supplied.
        assertThat(dsl.fetchCount(COMPANY)).isEqualTo(1);
    }

    @Test
    void createHonoursExplicitStatus() {
        Application created = repository.create(ownerId,
                new Application(null, new CompanyRef(null, "Interviewing Inc"),
                        "SRE", null, ApplicationStatus.INTERVIEWING, null));

        assertThat(created.status()).isEqualTo(ApplicationStatus.INTERVIEWING);
        assertThat(created.appliedOn()).isNull();
    }

    @Test
    void createRejectsNullEmployer() {
        Application toCreate = new Application(null, null, "Role", null, null, null);

        assertThatThrownBy(() -> repository.create(ownerId, toCreate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing employer company");
    }

    @Test
    void createRejectsEmployerWithNeitherIdNorName() {
        Application toCreate = new Application(
                null, new CompanyRef(null, "  "), "Role", null, null, null);

        assertThatThrownBy(() -> repository.create(ownerId, toCreate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing employer company name");
    }

    @Test
    void createRejectsEmployerReferencingUnknownCompany() {
        UUID unknownCompanyId = UUID.randomUUID();
        Application toCreate = new Application(
                null, new CompanyRef(unknownCompanyId, null), "Role", null, null, null);

        assertThatThrownBy(() -> repository.create(ownerId, toCreate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company not found");
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByIdReturnsCreatedApplication() {
        Application created = repository.create(ownerId,
                new Application(null, new CompanyRef(null, "Findable Co"),
                        "QA Engineer", null, null, null));

        assertThat(repository.findById(created.id())).contains(created);
    }
}
