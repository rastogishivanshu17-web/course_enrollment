package com.project.course_enrollment.domain;

/**
 * Outcome of an enrollment or drop operation.
 *
 * <p>Modelling outcomes as an explicit enum (rather than throwing for every business condition)
 * keeps expected, non-exceptional results — such as duplicate requests — in the normal control
 * flow while still allowing the API layer to map each outcome to an appropriate HTTP response.</p>
 */
public enum EnrollmentOutcome {

    /** Learner was given a seat. */
    ENROLLED,

    /** Course was full; learner was added to the waitlist. */
    WAITLISTED,

    /** Learner already holds a seat; request was a no-op. */
    ALREADY_ENROLLED,

    /** Learner is already on the waitlist; request was a no-op (FIFO order preserved). */
    ALREADY_WAITLISTED,

    /** Learner failed the prerequisite; cannot enroll or be waitlisted. */
    NOT_ELIGIBLE,

    /** Enrolled learner was dropped, freeing a seat. */
    DROPPED,

    /** Drop requested for a learner with no active enrollment; no state changed. */
    NOT_ENROLLED
}
