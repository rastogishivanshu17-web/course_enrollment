package com.project.course_enrollment.service;

import com.project.course_enrollment.domain.CourseOffering;
import com.project.course_enrollment.domain.EnrollmentOutcome;
import com.project.course_enrollment.domain.EnrollmentResult;
import com.project.course_enrollment.domain.Learner;
import com.project.course_enrollment.domain.Registration;
import com.project.course_enrollment.domain.RegistrationStatus;
import com.project.course_enrollment.store.InMemoryRegistrationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * State-based tests for DefaultEnrollmentService.
 * Tests assert the resulting registration state and capacity invariants.
 */
class DefaultEnrollmentServiceTest {

    private InMemoryRegistrationStore store;
    private DefaultEnrollmentService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryRegistrationStore();
        service = new DefaultEnrollmentService(
                new CourseOffering("CS101", 2), store);
    }

    @Test
    void enrollsEligibleLearnerWhenCapacityAvailable() {
        EnrollmentResult result = service.enroll(new Learner("L1", true));

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.ENROLLED);
        assertThat(store.findByLearnerId("L1").orElseThrow().status())
                .isEqualTo(RegistrationStatus.ENROLLED);
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(1);
    }

    @Test
    void waitlistsEligibleLearnerWhenFull() {
        service.enroll(new Learner("L1", true));
        service.enroll(new Learner("L2", true));

        EnrollmentResult result = service.enroll(new Learner("L3", true));

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.WAITLISTED);
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(2);
        assertThat(store.findByLearnerId("L3").orElseThrow().status())
                .isEqualTo(RegistrationStatus.WAITLISTED);
    }

    @Test
    void rejectsIneligibleLearnerAndChangesNoState() {
        EnrollmentResult result = service.enroll(new Learner("L1", false));

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.NOT_ELIGIBLE);
        assertThat(store.findByLearnerId("L1")).isEmpty();
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isZero();
        assertThat(store.countByStatus(RegistrationStatus.WAITLISTED)).isZero();
    }

    @Test
    void duplicateEnrollmentIsIdempotentAndFreesNoExtraSeat() {
        service.enroll(new Learner("L1", true));

        EnrollmentResult second = service.enroll(new Learner("L1", true));

        assertThat(second.outcome()).isEqualTo(EnrollmentOutcome.ALREADY_ENROLLED);
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(1);
    }

    @Test
    void duplicateWaitlistRequestPreservesFifoOrder() {
        service.enroll(new Learner("L1", true));
        service.enroll(new Learner("L2", true));
        service.enroll(new Learner("W1", true));
        service.enroll(new Learner("W2", true));

        long w1SequenceBefore = store.findByLearnerId("W1").orElseThrow().sequence();

        EnrollmentResult duplicate = service.enroll(new Learner("W1", true));

        assertThat(duplicate.outcome()).isEqualTo(EnrollmentOutcome.ALREADY_WAITLISTED);
        assertThat(store.findByLearnerId("W1").orElseThrow().sequence())
                .isEqualTo(w1SequenceBefore);
        assertThat(store.waitlistInFifoOrder())
                .extracting(Registration::learnerId)
                .containsExactly("W1", "W2");
        assertThat(store.countByStatus(RegistrationStatus.WAITLISTED)).isEqualTo(2);
    }

    @Test
    void capacityInvariantIsNeverExceeded() {
        service.enroll(new Learner("L1", true));
        service.enroll(new Learner("L2", true));
        service.enroll(new Learner("L3", true));
        service.enroll(new Learner("L4", true));

        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(2);
        assertThat(store.countByStatus(RegistrationStatus.WAITLISTED)).isEqualTo(2);
    }

    @Test
    void nullLearnerIsRejected() {
        assertThatThrownBy(() -> service.enroll(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankLearnerIdIsRejectedByDomain() {
        assertThatThrownBy(() -> service.enroll(new Learner(" ", true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void waitlistedLearnerRetainsCapturedEligibility() {
        service.enroll(new Learner("L1", true));
        service.enroll(new Learner("L2", true));
        service.enroll(new Learner("W1", true));

        assertThat(store.findByLearnerId("W1").orElseThrow().eligible()).isTrue();
    }
}
