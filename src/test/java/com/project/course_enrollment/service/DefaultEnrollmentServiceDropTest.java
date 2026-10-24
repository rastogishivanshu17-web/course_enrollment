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
 * State-based tests for DefaultEnrollmentService drop and FIFO promotion.
 *
 * Promotion re-checks eligibility. Ineligible waitlisted learners are skipped
 * but retained in the waitlist, preserving the relative order of remaining
 * waitlisted learners.
 */
class DefaultEnrollmentServiceDropTest {

    private static final int CAPACITY = 2;

    private InMemoryRegistrationStore store;
    private DefaultEnrollmentService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryRegistrationStore();
        service = new DefaultEnrollmentService(
                new CourseOffering("CS101", CAPACITY), store);
    }

    private void seedEnrolled(String learnerId) {
        store.save(new Registration(
                learnerId, store.nextSequence(), true, RegistrationStatus.ENROLLED));
    }

    private void seedWaitlisted(String learnerId, boolean eligible) {
        store.save(new Registration(
                learnerId, store.nextSequence(), eligible, RegistrationStatus.WAITLISTED));
    }

    private RegistrationStatus statusOf(String learnerId) {
        return store.findByLearnerId(learnerId).orElseThrow().status();
    }

    private void assertInvariants() {
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED))
                .as("enrolled count never exceeds capacity")
                .isLessThanOrEqualTo(CAPACITY);
    }

    // Rule 1: valid drop removes active enrollment.
    @Test
    void validDropRemovesEnrollmentAndLeavesSeatOpenWhenNoWaitlist() {
        service.enroll(new Learner("L1", true));

        EnrollmentResult result = service.drop("L1");

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.DROPPED);
        assertThat(statusOf("L1")).isEqualTo(RegistrationStatus.DROPPED);
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isZero();
        assertInvariants();
    }

    // Rules 4-7: promote the earliest eligible learner in FIFO order.
    @Test
    void dropPromotesEarliestEligibleWaitlistedLearnerInFifoOrder() {
        service.enroll(new Learner("L1", true));
        service.enroll(new Learner("L2", true)); // full
        service.enroll(new Learner("W1", true)); // waitlisted
        service.enroll(new Learner("W2", true)); // waitlisted

        EnrollmentResult result = service.drop("L1");

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.DROPPED);
        assertThat(statusOf("W1")).isEqualTo(RegistrationStatus.ENROLLED);
        assertThat(statusOf("W2")).isEqualTo(RegistrationStatus.WAITLISTED);
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(CAPACITY);
        assertInvariants();
    }

    // Rule 5: skip an ineligible learner, but retain the waitlist entry.
    @Test
    void promotionSkipsSingleIneligibleAndRetainsIt() {
        seedEnrolled("E1");
        seedEnrolled("E2"); // full
        seedWaitlisted("W1", false); // ineligible, earliest
        seedWaitlisted("W2", true);  // eligible

        service.drop("E1");

        assertThat(statusOf("W1")).isEqualTo(RegistrationStatus.WAITLISTED);
        assertThat(statusOf("W2")).isEqualTo(RegistrationStatus.ENROLLED);
        assertInvariants();
    }

    @Test
    void promotionSkipsMultipleIneligibleBeforeEligible() {
        seedEnrolled("E1");
        seedEnrolled("E2"); // full
        seedWaitlisted("W1", false);
        seedWaitlisted("W2", false);
        seedWaitlisted("W3", true); // first eligible
        seedWaitlisted("W4", true);

        service.drop("E1");

        assertThat(statusOf("W1")).isEqualTo(RegistrationStatus.WAITLISTED);
        assertThat(statusOf("W2")).isEqualTo(RegistrationStatus.WAITLISTED);
        assertThat(statusOf("W3")).isEqualTo(RegistrationStatus.ENROLLED);
        assertThat(statusOf("W4")).isEqualTo(RegistrationStatus.WAITLISTED);

        // Remaining waitlisted learners retain their relative FIFO order.
        assertThat(store.waitlistInFifoOrder())
                .extracting(Registration::learnerId)
                .containsExactly("W1", "W2", "W4");
        assertInvariants();
    }

    // Rule 8: if nobody is eligible, leave the seat open.
    @Test
    void noEligibleWaitlistedLearnerLeavesSeatOpen() {
        seedEnrolled("E1");
        seedEnrolled("E2"); // full
        seedWaitlisted("W1", false);
        seedWaitlisted("W2", false);

        service.drop("E1");

        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(1);
        assertThat(statusOf("W1")).isEqualTo(RegistrationStatus.WAITLISTED);
        assertThat(statusOf("W2")).isEqualTo(RegistrationStatus.WAITLISTED);
        assertInvariants();
    }

    // Rule 2: dropping a learner who is not enrolled changes no state.
    @Test
    void dropOfMissingLearnerReturnsNotEnrolledAndChangesNoState() {
        EnrollmentResult result = service.drop("ghost");

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.NOT_ENROLLED);
        assertThat(store.findByLearnerId("ghost")).isEmpty();
    }

    @Test
    void dropOfWaitlistedLearnerReturnsNotEnrolledAndDoesNotPromote() {
        service.enroll(new Learner("L1", true));
        service.enroll(new Learner("L2", true)); // full
        service.enroll(new Learner("W1", true)); // waitlisted

        EnrollmentResult result = service.drop("W1");

        assertThat(result.outcome()).isEqualTo(EnrollmentOutcome.NOT_ENROLLED);
        assertThat(statusOf("W1")).isEqualTo(RegistrationStatus.WAITLISTED);
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(CAPACITY);
    }

    // Rule 3: a repeated drop must not promote another learner.
    @Test
    void repeatedDropDoesNotFreeExtraSeatOrPromoteAgain() {
        service.enroll(new Learner("L1", true));
        service.enroll(new Learner("L2", true)); // full
        service.enroll(new Learner("W1", true)); // waitlisted
        service.enroll(new Learner("W2", true)); // waitlisted

        service.drop("L1"); // promotes W1
        EnrollmentResult second = service.drop("L1"); // repeated drop

        assertThat(second.outcome()).isEqualTo(EnrollmentOutcome.NOT_ENROLLED);
        assertThat(statusOf("W1")).isEqualTo(RegistrationStatus.ENROLLED);
        assertThat(statusOf("W2")).isEqualTo(RegistrationStatus.WAITLISTED);
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(CAPACITY);
        assertInvariants();
    }

    // Invalid drop input.
    @Test
    void nullOrBlankLearnerIdIsRejected() {
        assertThatThrownBy(() -> service.drop(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.drop(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // State consistency across a drop/promote cycle.
    @Test
    void dropAndPromotionLeaveConsistentState() {
        service.enroll(new Learner("L1", true));
        service.enroll(new Learner("L2", true)); // full
        service.enroll(new Learner("W1", true)); // waitlisted

        service.drop("L2"); // promotes W1

        assertThat(statusOf("L2")).isEqualTo(RegistrationStatus.DROPPED);
        assertThat(statusOf("W1")).isEqualTo(RegistrationStatus.ENROLLED);
        assertThat(statusOf("L1")).isEqualTo(RegistrationStatus.ENROLLED);
        assertThat(store.countByStatus(RegistrationStatus.WAITLISTED)).isZero();
        assertInvariants();
    }

    @Test
    void droppedLearnerCanReenrollIntoAFreeSeat() {
        service.enroll(new Learner("L1", true));
        service.drop("L1"); // seat free, no waitlist

        EnrollmentResult reenroll = service.enroll(new Learner("L1", true));

        assertThat(reenroll.outcome()).isEqualTo(EnrollmentOutcome.ENROLLED);
        assertThat(statusOf("L1")).isEqualTo(RegistrationStatus.ENROLLED);
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(1);
        assertInvariants();
    }
}
