package com.project.course_enrollment.store;

import com.project.course_enrollment.domain.Registration;
import com.project.course_enrollment.domain.RegistrationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction over registration storage for the single course offering.
 *
 * <p>This is a lightweight persistence boundary (in-memory for this assessment).
 * It exposes only the queries and mutations the enrollment service needs, keeping
 * storage details replaceable without changing business logic.
 *
 * <p>Thread-safety note: implementations may offer per-operation safety, but
 * compound read-modify-write sequences (e.g. "check capacity then enroll") are
 * serialized by the service layer, which is the single owner of business invariants.
 */
public interface RegistrationStore {

    /** @return the current registration for the learner, if one exists (any status). */
    Optional<Registration> findByLearnerId(String learnerId);

    /** Inserts or updates the registration, keyed by learner id. */
    void save(Registration registration);

    /** @return the next monotonically increasing sequence value for FIFO ordering. */
    long nextSequence();

    /** @return the number of registrations currently in the given status. */
    int countByStatus(RegistrationStatus status);

    /** @return waitlisted registrations in ascending sequence (FIFO) order. */
    List<Registration> waitlistInFifoOrder();

    /** Removes all registrations and resets the sequence. Intended for tests. */
    void clear();
}
