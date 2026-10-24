package com.project.course_enrollment.domain;

/**
 * Lifecycle state of a learner's registration against the single course offering.
 *
 * <ul>
 *   <li>{@link #ENROLLED} — holds a seat (counts against capacity).</li>
 *   <li>{@link #WAITLISTED} — waiting for a seat, retained in FIFO order.</li>
 *   <li>{@link #DROPPED} — no longer active; frees a seat if previously enrolled.</li>
 * </ul>
 *
 * <p>{@link #ENROLLED} and {@link #WAITLISTED} are the two <em>active</em> states. A learner may
 * have at most one active registration (invariant enforced by the service/store).</p>
 */
public enum RegistrationStatus {
    ENROLLED,
    WAITLISTED,
    DROPPED
}
