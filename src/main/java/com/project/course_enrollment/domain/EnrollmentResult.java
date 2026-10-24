package com.project.course_enrollment.domain;

import java.util.Objects;

/**
 * Immutable result of an enrollment or drop operation.
 *
 * @param outcome the business outcome
 * @param learnerId the learner the operation targeted
 * @param message a short, human-readable explanation
 */
public record EnrollmentResult(EnrollmentOutcome outcome, String learnerId, String message) {

    public EnrollmentResult {
        Objects.requireNonNull(outcome, "outcome must not be null");
        if (learnerId == null || learnerId.isBlank()) {
            throw new IllegalArgumentException("Learner id must not be blank");
        }
    }

    public static EnrollmentResult of(
            EnrollmentOutcome outcome, String learnerId, String message) {
        return new EnrollmentResult(outcome, learnerId, message);
    }
}
