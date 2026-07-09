package org.enricogiurin.jobtracker.api.model;

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

import java.time.LocalDate;
import java.util.UUID;

import org.enricogiurin.jobtracker.api.jooq.enums.ApplicationStatus;

/**
 * A job application: one opportunity followed through its lifecycle.
 *
 * <p>The read view embeds the {@link #employer()} as a {@link CompanyRef}. The
 * owning user is intentionally not exposed here; ownership is supplied
 * out-of-band on write (see {@code ApplicationRepository.create}) until
 * authentication is wired in.
 *
 * <p>On write, {@code null} components mean "leave unchanged" (partial update);
 * the {@link #employer()} id is the only strictly required field on create.
 */
public record Application(
        UUID id,
        CompanyRef employer,
        String role,
        String postingUrl,
        ApplicationStatus status,
        LocalDate appliedOn) {
}
