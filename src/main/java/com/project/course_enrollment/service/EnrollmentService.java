package com.project.course_enrollment.service;

import com.project.course_enrollment.domain.EnrollmentResult;
import com.project.course_enrollment.domain.Learner;

/**
 * Service boundary for enrollment operations against the single course offering.
 *
 * <p>This contract is intentionally small and outcome-oriented: each operation
 * returns an {@link EnrollmentResult} describing what happened, rather than
 * relying on exceptions for expected business conditions (duplicates,
 * ineligibility, dropping a non-enrolled learner).
 *
 * <p>Implementations must enforce the engineering invariants (capacity,
 * single active registration, eligibility, FIFO promotion) and serialize
 * compound state changes for thread safety.
 */
public interface EnrollmentService {

    /**
     * Enrolls the learner if a seat is available; otherwise waitlists an eligible learner.
     * Idempotent for learners who already hold an active registration.
     *
     * @param learner the learner and their current eligibility
     * @return the outcome of the enrollment request
     */
    EnrollmentResult enroll(Learner learner);

    /**
     * Drops the learner's seat if currently enrolled and promotes the earliest
     * eligible waitlisted learner. A no-op for learners who are not currently enrolled.
     *
     * @param learnerId the learner to drop
     * @return the outcome of the drop request
     */
    EnrollmentResult drop(String learnerId);
}
