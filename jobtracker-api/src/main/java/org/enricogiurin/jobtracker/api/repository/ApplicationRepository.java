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

import static org.enricogiurin.jobtracker.api.jooq.Tables.APPLICATION;
import static org.enricogiurin.jobtracker.api.jooq.Tables.COMPANY;
import static org.jooq.Functions.nullOnAllNull;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.row;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.enricogiurin.jobtracker.api.jooq.enums.ApplicationStatus;
import org.enricogiurin.jobtracker.api.jooq.tables.Company;
import org.enricogiurin.jobtracker.api.jooq.tables.records.ApplicationRecord;
import org.enricogiurin.jobtracker.api.jooq.tables.records.CompanyRecord;
import org.enricogiurin.jobtracker.api.model.Application;
import org.enricogiurin.jobtracker.api.model.CompanyRef;
import org.jooq.DSLContext;
import org.jooq.Record6;
import org.jooq.SelectOnConditionStep;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ApplicationRepository {

    // SELECT column aliases, matching the Application record component names.
    private static final String ID = "id";
    private static final String EMPLOYER_ALIAS = "employer";
    private static final String ROLE = "role";
    private static final String POSTING_URL = "postingUrl";
    private static final String STATUS = "status";
    private static final String APPLIED_ON = "appliedOn";

    // alias company as the employer so the SELECT can join and embed it.
    private static final Company EMPLOYER = COMPANY.as(EMPLOYER_ALIAS);

    private final DSLContext dsl;

    public ApplicationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Application> findById(UUID id) {
        return getSelectApplicationSpec()
                .where(APPLICATION.ID.eq(id))
                .fetchOptional()
                .map(mapping(Application::new));
    }

    /**
     * List all applications owned by the given user, most recently applied
     * first (applications not yet submitted, with no {@code appliedOn}, last).
     *
     * @param ownerId the owning user
     * @return the owner's applications, possibly empty
     */
    public List<Application> findAllByOwner(UUID ownerId) {
        return getSelectApplicationSpec()
                .where(APPLICATION.USER_ID.eq(ownerId))
                .orderBy(APPLICATION.APPLIED_ON.desc().nullsLast())
                .fetch(mapping(Application::new));
    }

    /**
     * Create a new application owned by the given user.
     *
     * <p>The employer is resolved from {@code application.employer()}: when it
     * carries an {@code id} the existing company is used; otherwise a new company
     * is inserted from its {@code name}. Everything runs in a single transaction,
     * so a failure rolls back both the company and the application. The lifecycle
     * {@code status} defaults to {@code EXPLORING} when not supplied.
     *
     * @param ownerId the owning user (until auth is wired in, supplied by the caller)
     * @param application the application data to persist
     * @return the created application
     * @throws IllegalArgumentException if the employer references a missing company,
     *     or has neither an id nor a name
     */
    @Transactional(readOnly = false)
    public Application create(UUID ownerId, Application application) {
        UUID employerId = resolveEmployerId(application.employer());
        ApplicationRecord record = dsl.newRecord(APPLICATION);
        record.setUserId(ownerId);
        record.setEmployerId(employerId);
        record.setRole(application.role());
        record.setPostingUrl(application.postingUrl());
        if (application.status() != null) {
            record.setStatus(application.status());
        }
        record.setAppliedOn(application.appliedOn());
        record.insert();
        return findById(record.getId()).orElseThrow();
    }

    /**
     * Resolve the employer to a company id: reuse the existing company when an
     * id is supplied, otherwise insert a new one from the name.
     */
    private UUID resolveEmployerId(CompanyRef employer) {
        if (employer == null) {
            throw new IllegalArgumentException("Missing employer company");
        }
        if (employer.id() != null) {
            return requireCompanyId(employer);
        }
        if (employer.name() == null || employer.name().isBlank()) {
            throw new IllegalArgumentException("Missing employer company name");
        }
        return insertCompany(employer.name());
    }

    private UUID insertCompany(String name) {
        CompanyRecord company = dsl.newRecord(COMPANY);
        company.setName(name);
        company.insert();
        return company.getId();
    }

    private SelectOnConditionStep<Record6<UUID, CompanyRef, String, String,
            ApplicationStatus, LocalDate>> getSelectApplicationSpec() {
        return dsl.select(
                APPLICATION.ID.as(ID),
                row(EMPLOYER.ID, EMPLOYER.NAME)
                        .mapping(nullOnAllNull(CompanyRef::new)).as(EMPLOYER_ALIAS),
                APPLICATION.ROLE.as(ROLE),
                APPLICATION.POSTING_URL.as(POSTING_URL),
                APPLICATION.STATUS.as(STATUS),
                APPLICATION.APPLIED_ON.as(APPLIED_ON))
                .from(APPLICATION)
                .join(EMPLOYER).on(APPLICATION.EMPLOYER_ID.eq(EMPLOYER.ID));
    }

    private UUID requireCompanyId(CompanyRef company) {
        return dsl.select(COMPANY.ID)
                .from(COMPANY)
                .where(COMPANY.ID.eq(company.id()))
                .fetchOptional(COMPANY.ID).orElseThrow(
                        () -> new IllegalArgumentException(
                                "Company not found for employer: " + company.id()));
    }

}
