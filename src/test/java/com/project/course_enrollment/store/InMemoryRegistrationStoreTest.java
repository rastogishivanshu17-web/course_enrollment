package com.project.course_enrollment.store;

import com.project.course_enrollment.domain.Registration;
import com.project.course_enrollment.domain.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRegistrationStoreTest {

    private InMemoryRegistrationStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryRegistrationStore();
    }

    @Test
    void savesAndFindsByLearnerId() {
        Registration registration = new Registration(
                "L1", store.nextSequence(), true, RegistrationStatus.ENROLLED);

        store.save(registration);

        assertThat(store.findByLearnerId("L1")).containsSame(registration);
        assertThat(store.findByLearnerId("missing")).isEmpty();
    }

    @Test
    void nextSequenceIncreasesMonotonically() {
        long first = store.nextSequence();
        long second = store.nextSequence();
        long third = store.nextSequence();

        assertThat(first).isLessThan(second);
        assertThat(second).isLessThan(third);
    }

    @Test
    void saveReplacesExistingRegistrationForSameLearner() {
        store.save(new Registration(
                "L1", 0, true, RegistrationStatus.WAITLISTED));
        store.save(new Registration(
                "L1", 0, true, RegistrationStatus.ENROLLED));

        assertThat(store.countByStatus(RegistrationStatus.WAITLISTED)).isZero();
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(1);
    }

    @Test
    void countsByStatus() {
        store.save(new Registration("L1", 0, true, RegistrationStatus.ENROLLED));
        store.save(new Registration("L2", 1, true, RegistrationStatus.WAITLISTED));
        store.save(new Registration("L3", 2, true, RegistrationStatus.WAITLISTED));
        store.save(new Registration("L4", 3, true, RegistrationStatus.DROPPED));

        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(1);
        assertThat(store.countByStatus(RegistrationStatus.WAITLISTED)).isEqualTo(2);
        assertThat(store.countByStatus(RegistrationStatus.DROPPED)).isEqualTo(1);
    }

    @Test
    void waitlistIsReturnedInFifoOrderRegardlessOfInsertionOrder() {
        // Insert out of sequence order to prove ordering is based on sequence.
        store.save(new Registration("L3", 2, true, RegistrationStatus.WAITLISTED));
        store.save(new Registration("L1", 0, true, RegistrationStatus.WAITLISTED));
        store.save(new Registration("L2", 1, true, RegistrationStatus.WAITLISTED));
        store.save(new Registration("L0", 3, true, RegistrationStatus.ENROLLED));

        List<Registration> waitlist = store.waitlistInFifoOrder();

        assertThat(waitlist)
                .extracting(Registration::learnerId)
                .containsExactly("L1", "L2", "L3");
    }

    @Test
    void clearResetsStateAndSequence() {
        store.save(new Registration(
                "L1", store.nextSequence(), true, RegistrationStatus.ENROLLED));

        store.clear();

        assertThat(store.findByLearnerId("L1")).isEmpty();
        assertThat(store.nextSequence()).isZero();
    }
}
