package com.project.course_enrollment.api.dto;

import com.project.course_enrollment.domain.EnrollmentResult;

/**
 * Response body describing the outcome of an enrollment or drop operation.
 *
 * @param outcome   the business outcome name (e.g. {@code ENROLLED}, {@code WAITLISTED})
 * @param learnerId the learner the operation targeted
 * @param message   a short, human-readable explanation
 */
public record EnrollmentResponse(String outcome, String learnerId, String message) {

    public static EnrollmentResponse from(EnrollmentResult result) {
        return new EnrollmentResponse(
                result.outcome().name(),
                result.learnerId(),
                result.message());
    }
}
