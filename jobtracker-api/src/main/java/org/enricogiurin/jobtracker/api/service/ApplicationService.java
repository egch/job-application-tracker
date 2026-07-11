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

import java.util.Optional;
import java.util.UUID;

import org.enricogiurin.jobtracker.api.model.Application;
import org.enricogiurin.jobtracker.api.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application use cases, owning the transaction boundary and the ownership
 * concern so the controller stays about HTTP and the repository stays about
 * data access.
 *
 * <p>Until authentication is wired in, the owning user is supplied by the caller
 * (see {@code ownerId}); once Keycloak lands it will be resolved from the
 * security context here rather than passed through.
 */
@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    /**
     * Look up a single application by id.
     *
     * @param id the application id
     * @return the application, or empty when none exists
     */
    public Optional<Application> findById(UUID id) {
        return repository.findById(id);
    }

    /**
     * Create a new application owned by the given user.
     *
     * @param ownerId the owning user (until auth is wired in, supplied by the caller)
     * @param application the application data to persist
     * @return the created application
     */
    @Transactional(readOnly = false)
    public Application create(UUID ownerId, Application application) {
        return repository.create(ownerId, application);
    }
}
