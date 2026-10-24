package com.project.course_enrollment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for enrolling a learner into the offering.
 *
 * @param learnerId non-blank learner identifier
 * @param eligible whether the learner satisfies the prerequisite (required so the caller must
 *                 state it explicitly rather than defaulting silently)
 */
public record EnrollmentRequest(
        @NotBlank(message = "learnerId must not be blank") String learnerId,
        @NotNull(message = "eligible must be provided") Boolean eligible) {
}
