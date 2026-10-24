package com.project.course_enrollment.service;

import com.project.course_enrollment.domain.CourseOffering;
import com.project.course_enrollment.domain.EnrollmentOutcome;
import com.project.course_enrollment.domain.EnrollmentResult;
import com.project.course_enrollment.domain.Learner;
import com.project.course_enrollment.domain.Registration;
import com.project.course_enrollment.domain.RegistrationStatus;
import com.project.course_enrollment.store.RegistrationStore;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Default {@link EnrollmentService} implementation for the single course offering.
 *
 * <p><strong>Concurrency:</strong> every compound read-modify-write operation
 * (enroll and drop/promotion) runs inside a single intrinsic lock. This guarantees
 * the capacity and single-active-registration invariants under concurrent requests,
 * and keeps a drop and its triggered promotion atomic. These are in-JVM guarantees
 * only; no database or distributed locking is involved.
 */
@Service
public class DefaultEnrollmentService implements EnrollmentService {

    private final CourseOffering offering;
    private final RegistrationStore store;
    private final Object lock = new Object();

    public DefaultEnrollmentService(CourseOffering offering, RegistrationStore store) {
        this.offering = Objects.requireNonNull(offering, "offering must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    @Override
    public EnrollmentResult enroll(Learner learner) {
        if (learner == null) {
            throw new IllegalArgumentException("learner must not be null");
        }

        final String learnerId = learner.id(); // record guarantees non-blank

        synchronized (lock) {
            Optional<Registration> existing = store.findByLearnerId(learnerId);

            // 1. Duplicate active registration: idempotent no-op, waitlist order preserved.
            if (existing.isPresent() && existing.get().isActive()) {
                Registration reg = existing.get();
                if (reg.isEnrolled()) {
                    return EnrollmentResult.of(
                            EnrollmentOutcome.ALREADY_ENROLLED,
                            learnerId,
                            "Learner already enrolled");
                }
                return EnrollmentResult.of(
                        EnrollmentOutcome.ALREADY_WAITLISTED,
                        learnerId,
                        "Learner already on the waitlist");
            }

            // 2. Eligibility: ineligible learners are never enrolled or waitlisted.
            if (!learner.eligible()) {
                return EnrollmentResult.of(
                        EnrollmentOutcome.NOT_ELIGIBLE,
                        learnerId,
                        "Learner does not satisfy the prerequisite");
            }

            // 3. Capacity: assign a seat if available, otherwise join the FIFO waitlist.
            int enrolledCount = store.countByStatus(RegistrationStatus.ENROLLED);
            long sequence = store.nextSequence();

            if (offering.hasAvailableSeat(enrolledCount)) {
                store.save(new Registration(
                        learnerId, sequence, true, RegistrationStatus.ENROLLED));
                return EnrollmentResult.of(
                        EnrollmentOutcome.ENROLLED,
                        learnerId,
                        "Learner enrolled");
            }

            store.save(new Registration(
                    learnerId, sequence, true, RegistrationStatus.WAITLISTED));
            return EnrollmentResult.of(
                    EnrollmentOutcome.WAITLISTED,
                    learnerId,
                    "Course full; learner added to waitlist");
        }
    }

    @Override
    public EnrollmentResult drop(String learnerId) {
        if (learnerId == null || learnerId.isBlank()) {
            throw new IllegalArgumentException("learnerId must not be blank");
        }

        synchronized (lock) {
            Optional<Registration> existing = store.findByLearnerId(learnerId);

            // Dropping a non-enrolled learner (missing, waitlisted, or already dropped)
            // returns a clear result and changes no state. Repeated drops are no-ops.
            if (existing.isEmpty() || !existing.get().isEnrolled()) {
                return EnrollmentResult.of(
                        EnrollmentOutcome.NOT_ENROLLED,
                        learnerId,
                        "Learner is not currently enrolled; no state changed");
            }

            // 1. Free the seat.
            Registration dropped = existing.get();
            dropped.markDropped();
            store.save(dropped);

            // 2. Promote the earliest eligible learner on the waitlist, if any.
            promoteEarliestEligible();

            return EnrollmentResult.of(
                    EnrollmentOutcome.DROPPED,
                    learnerId,
                    "Learner dropped");
        }
    }

    /**
     * Promotes the first eligible learner in FIFO waitlist order, if any.
     * Must be called while holding {@link #lock}. Ineligible learners are skipped
     * and left on the waitlist; if no eligible learner exists, the freed seat remains open.
     */
    private void promoteEarliestEligible() {
        for (Registration candidate : store.waitlistInFifoOrder()) {
            if (candidate.eligible()) {
                candidate.markEnrolled();
                store.save(candidate);
                return;
            }
            // Ineligible: skip for this promotion attempt but retain the waitlist entry.
        }
    }
}
