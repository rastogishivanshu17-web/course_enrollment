package com.project.course_enrollment.service;

import com.project.course_enrollment.domain.CourseOffering;
import com.project.course_enrollment.domain.EnrollmentOutcome;
import com.project.course_enrollment.domain.EnrollmentResult;
import com.project.course_enrollment.domain.Learner;
import com.project.course_enrollment.domain.Registration;
import com.project.course_enrollment.domain.RegistrationStatus;
import com.project.course_enrollment.store.RegistrationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mockito-based tests that isolate RegistrationStore.
 * Rejected requests must not write to the store or consume a sequence number.
 */
@ExtendWith(MockitoExtension.class)
class DefaultEnrollmentServiceMockTest {

    @Mock
    private RegistrationStore store;

    private DefaultEnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new DefaultEnrollmentService(
                new CourseOffering("CS101", 2), store);
    }

    @Test
    void ineligibleLearnerWritesNothingToStore() {
        when(store.findByLearnerId("L1")).thenReturn(Optional.empty());

        EnrollmentResult result = service.enroll(new Learner("L1", false));

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.NOT_ELIGIBLE);
        verify(store, never()).save(any(Registration.class));
        verify(store, never()).nextSequence();
    }

    @Test
    void duplicateEnrolledLearnerWritesNothingToStore() {
        Registration existing = new Registration(
                "L1", 0, true, RegistrationStatus.ENROLLED);
        when(store.findByLearnerId("L1")).thenReturn(Optional.of(existing));

        EnrollmentResult result = service.enroll(new Learner("L1", true));

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.ALREADY_ENROLLED);
        verify(store, never()).save(any(Registration.class));
        verify(store, never()).nextSequence();
    }
}
