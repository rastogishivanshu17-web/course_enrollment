package com.project.course_enrollment.domain;

/**
 * A learner who may register for the course offering.
 *
 * <p>Eligibility is modelled by a single prerequisite flag, per the approved assumptions.
 * Eligibility is evaluated at enrollment time and again during waitlist promotion.</p>
 *
 * @param id stable, non-blank learner identifier
 * @param eligible {@code true} if the learner satisfies the prerequisite
 */
public record Learner(String id, boolean eligible) {

    public Learner {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Learner id must not be blank");
        }
    }
}
