package com.project.course_enrollment.domain;

/**
 * A single course offering with a fixed, positive seat capacity.
 *
 * <p>The offering is intentionally stateless with respect to registrations: it only knows its
 * capacity and provides pure helper predicates. The current enrolled count is owned by the
 * registration store and passed in, so capacity decisions remain side-effect free and easy to test.</p>
 */
public final class CourseOffering {

    private final String id;
    private final int capacity;

    public CourseOffering(String id, int capacity) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Course offering id must not be blank");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be a positive integer");
        }
        this.id = id;
        this.capacity = capacity;
    }

    public String id() {
        return id;
    }

    public int capacity() {
        return capacity;
    }

    /** @return {@code true} if a seat is available given the current enrolled count. */
    public boolean hasAvailableSeat(int enrolledCount) {
        return enrolledCount < capacity;
    }

    /** @return {@code true} if the offering is at capacity given the current enrolled count. */
    public boolean isFull(int enrolledCount) {
        return enrolledCount >= capacity;
    }
}
