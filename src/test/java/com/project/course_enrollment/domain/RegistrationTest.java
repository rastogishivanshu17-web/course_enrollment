package com.project.course_enrollment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationTest {

    @Test
    void newWaitlistedRegistrationIsActiveAndWaitlisted() {
        Registration reg = new Registration(
                "L1", 0, true, RegistrationStatus.WAITLISTED);

        assertThat(reg.isActive()).isTrue();
        assertThat(reg.isWaitlisted()).isTrue();
        assertThat(reg.isEnrolled()).isFalse();
    }

    @Test
    void statusTransitionsUpdateActivity() {
        Registration reg = new Registration(
                "L1", 0, true, RegistrationStatus.WAITLISTED);

        reg.markEnrolled();
        assertThat(reg.isEnrolled()).isTrue();
        assertThat(reg.isActive()).isTrue();

        reg.markDropped();
        assertThat(reg.status()).isEqualTo(RegistrationStatus.DROPPED);
        assertThat(reg.isActive()).isFalse();
    }

    @Test
    void retainsEligibilityFlag() {
        assertThat(new Registration(
                "L1", 0, true, RegistrationStatus.WAITLISTED)
                .eligible()).isTrue();

        assertThat(new Registration(
                "L2", 1, false, RegistrationStatus.WAITLISTED)
                .eligible()).isFalse();
    }

    @Test
    void rejectsBlankLearnerIdAndNegativeSequence() {
        assertThatThrownBy(() -> new Registration(
                "", 0, true, RegistrationStatus.ENROLLED))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Registration(
                "L1", -1, true, RegistrationStatus.ENROLLED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
