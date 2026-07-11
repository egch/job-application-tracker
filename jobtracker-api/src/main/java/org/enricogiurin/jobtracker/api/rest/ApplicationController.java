package org.enricogiurin.jobtracker.api.rest;

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

import java.util.List;
import java.util.UUID;

import org.enricogiurin.jobtracker.api.model.Application;
import org.enricogiurin.jobtracker.api.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for job applications.
 *
 * <p>Ownership is carried by the {@code X-User-Id} header until authentication
 * is wired in; once Keycloak lands it will come from the security context and
 * the header will be dropped.
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    static final String USER_ID_HEADER = "X-User-Id";

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Application>> list(
            @RequestHeader(USER_ID_HEADER) UUID userId) {
        return ResponseEntity.ok(service.findAllByOwner(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> findById(@PathVariable UUID id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Application> create(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestBody Application application) {
        Application created = service.create(userId, application);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
