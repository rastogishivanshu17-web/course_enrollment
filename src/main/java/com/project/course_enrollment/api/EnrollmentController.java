package com.project.course_enrollment.api;

import com.project.course_enrollment.api.dto.EnrollmentRequest;
import com.project.course_enrollment.api.dto.EnrollmentResponse;
import com.project.course_enrollment.domain.CourseOffering;
import com.project.course_enrollment.error.OfferingNotFoundException;
import com.project.course_enrollment.domain.EnrollmentOutcome;
import com.project.course_enrollment.domain.EnrollmentResult;
import com.project.course_enrollment.domain.Learner;
import com.project.course_enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin REST adapter over {@link EnrollmentService}. Controllers contain no business rules: they
 * validate the offering id, delegate to the service, and map the {@link EnrollmentOutcome} to a
 * consistent HTTP status.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code POST /api/offerings/{offeringId}/enrollments} – enroll or waitlist a learner.</li>
 *   <li>{@code DELETE /api/offerings/{offeringId}/enrollments/{learnerId}} – drop a learner.</li>
 * </ul>
 *
 * <h2>Status mapping</h2>
 * <ul>
 *   <li>{@code ENROLLED} → 201 Created</li>
 *   <li>{@code WAITLISTED} → 202 Accepted</li>
 *   <li>{@code ALREADY_ENROLLED / ALREADY_WAITLISTED / DROPPED / NOT_ENROLLED} → 200 OK</li>
 *   <li>{@code NOT_ELIGIBLE} → 422 Unprocessable Entity</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/offerings/{offeringId}/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final CourseOffering offering;

    public EnrollmentController(EnrollmentService enrollmentService, CourseOffering offering) {
        this.enrollmentService = enrollmentService;
        this.offering = offering;
    }

    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(
            @PathVariable String offeringId,
            @Valid @RequestBody EnrollmentRequest request) {
        requireKnownOffering(offeringId);
        EnrollmentResult result = enrollmentService.enroll(
                new Learner(request.learnerId(), request.eligible()));
        return toResponse(result);
    }

    @DeleteMapping("/{learnerId}")
    public ResponseEntity<EnrollmentResponse> drop(
            @PathVariable String offeringId,
            @PathVariable String learnerId) {
        requireKnownOffering(offeringId);
        EnrollmentResult result = enrollmentService.drop(learnerId);
        return toResponse(result);
    }

    private void requireKnownOffering(String offeringId) {
        if (!offering.id().equals(offeringId)) {
            throw new OfferingNotFoundException(offeringId);
        }
    }

    private static ResponseEntity<EnrollmentResponse> toResponse(EnrollmentResult result) {
        return ResponseEntity.status(statusFor(result.outcome()))
                .body(EnrollmentResponse.from(result));
    }

    private static HttpStatus statusFor(EnrollmentOutcome outcome) {
        return switch (outcome) {
            case ENROLLED -> HttpStatus.CREATED;
            case WAITLISTED -> HttpStatus.ACCEPTED;
            case ALREADY_ENROLLED, ALREADY_WAITLISTED, DROPPED, NOT_ENROLLED -> HttpStatus.OK;
            case NOT_ELIGIBLE -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
}
