package com.project.course_enrollment.domain;

import java.util.Objects;

/**
 * A learner's registration record against the course offering.
 *
 * <p>The {@code sequence} is a monotonically increasing value assigned when the registration is
 * first created. It establishes a stable, total ordering used to preserve FIFO waitlist order
 * independently of the mutable {@link RegistrationStatus}.</p>
 *
 * <p>Only the {@link RegistrationStatus} is mutable; the learner id, sequence, and eligibility are
 * immutable. State transitions are expressed through intention-revealing methods rather than a
 * public setter. The {@code eligible} flag captured at registration time is what promotion
 * re-evaluates later, since the originating request is no longer available by then.</p>
 */
public final class Registration {

    private final String learnerId;
    private final long sequence;
    private final boolean eligible;
    private RegistrationStatus status;

    public Registration(
            String learnerId,
            long sequence,
            boolean eligible,
            RegistrationStatus initialStatus) {
        if (learnerId == null || learnerId.isBlank()) {
            throw new IllegalArgumentException("Learner id must not be blank");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("Sequence must not be negative");
        }
        this.learnerId = learnerId;
        this.sequence = sequence;
        this.eligible = eligible;
        this.status = Objects.requireNonNull(initialStatus, "initialStatus must not be null");
    }

    public String learnerId() {
        return learnerId;
    }

    public long sequence() {
        return sequence;
    }

    /** @return the learner's eligibility as captured when this registration was created. */
    public boolean eligible() {
        return eligible;
    }

    public RegistrationStatus status() {
        return status;
    }

    /** @return {@code true} if the registration is ENROLLED or WAITLISTED. */
    public boolean isActive() {
        return status == RegistrationStatus.ENROLLED
                || status == RegistrationStatus.WAITLISTED;
    }

    public boolean isEnrolled() {
        return status == RegistrationStatus.ENROLLED;
    }

    public boolean isWaitlisted() {
        return status == RegistrationStatus.WAITLISTED;
    }

    public void markEnrolled() {
        this.status = RegistrationStatus.ENROLLED;
    }

    public void markWaitlisted() {
        this.status = RegistrationStatus.WAITLISTED;
    }

    public void markDropped() {
        this.status = RegistrationStatus.DROPPED;
    }
}
