package com.project.course_enrollment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseOfferingTest {

    @Test
    void rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new CourseOffering("CS101", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        assertThatThrownBy(() -> new CourseOffering("CS101", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankId() {
        assertThatThrownBy(() -> new CourseOffering("", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportsAvailabilityRelativeToEnrolledCount() {
        CourseOffering offering = new CourseOffering("CS101", 2);

        assertThat(offering.hasAvailableSeat(0)).isTrue();
        assertThat(offering.hasAvailableSeat(1)).isTrue();
        assertThat(offering.hasAvailableSeat(2)).isFalse();

        assertThat(offering.isFull(1)).isFalse();
        assertThat(offering.isFull(2)).isTrue();
        assertThat(offering.isFull(3)).isTrue();
    }
}
