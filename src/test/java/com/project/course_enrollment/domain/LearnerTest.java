package com.project.course_enrollment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearnerTest {

    @Test
    void rejectsBlankId() {
        assertThatThrownBy(() -> new Learner(" ", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retainsEligibilityFlag() {
        assertThat(new Learner("L1", true).eligible()).isTrue();
        assertThat(new Learner("L2", false).eligible()).isFalse();
    }
}
