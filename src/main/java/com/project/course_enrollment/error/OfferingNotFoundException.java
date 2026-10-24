package com.project.course_enrollment.error;

/**
 * Thrown when a request targets an offering id that does not match the single configured offering.
 * Mapped to HTTP 404 by the global exception handler.
 */
public class OfferingNotFoundException extends RuntimeException {

    public OfferingNotFoundException(String offeringId) {
        super("Unknown course offering: " + offeringId);
    }
}
