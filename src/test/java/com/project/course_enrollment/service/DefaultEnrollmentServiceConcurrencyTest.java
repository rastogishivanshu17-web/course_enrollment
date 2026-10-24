package com.project.course_enrollment.service;

import com.project.course_enrollment.domain.CourseOffering;
import com.project.course_enrollment.domain.EnrollmentOutcome;
import com.project.course_enrollment.domain.EnrollmentResult;
import com.project.course_enrollment.domain.Learner;
import com.project.course_enrollment.domain.RegistrationStatus;
import com.project.course_enrollment.store.InMemoryRegistrationStore;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused concurrency tests for the single-lock synchronization strategy.
 *
 * These tests exercise in-JVM thread safety only. They demonstrate that the
 * service's intrinsic lock serializes compound state transitions so capacity
 * is never oversubscribed and no learner ends up with two active registrations.
 * They do not prove database transaction or distributed-locking guarantees;
 * the store is in-memory.
 */
class DefaultEnrollmentServiceConcurrencyTest {

    @Test
    void concurrentEnrollmentsNeverOversubscribeCapacity() throws InterruptedException {
        final int capacity = 5;
        final int learners = 200;

        InMemoryRegistrationStore store = new InMemoryRegistrationStore();
        DefaultEnrollmentService service =
                new DefaultEnrollmentService(new CourseOffering("CS101", capacity), store);

        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(learners);

        for (int i = 0; i < learners; i++) {
            final String id = "L" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    service.enroll(new Learner(id, true));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown(); // release all threads at once
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // Capacity invariant holds exactly: the rest are waitlisted; none lost.
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(capacity);
        assertThat(store.countByStatus(RegistrationStatus.WAITLISTED))
                .isEqualTo(learners - capacity);
    }

    @Test
    void concurrentDuplicateEnrollsSameLearnerProducesExactlyOneActiveRegistration()
            throws InterruptedException {
        final int threads = 50;

        InMemoryRegistrationStore store = new InMemoryRegistrationStore();
        DefaultEnrollmentService service =
                new DefaultEnrollmentService(new CourseOffering("CS101", 3), store);

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Queue<EnrollmentOutcome> outcomes = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    EnrollmentResult result = service.enroll(new Learner("SAME", true));
                    outcomes.add(result.outcome());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // Exactly one seat consumed; exactly one ENROLLED outcome, the rest idempotent.
        assertThat(store.countByStatus(RegistrationStatus.ENROLLED)).isEqualTo(1);
        assertThat(store.countByStatus(RegistrationStatus.WAITLISTED)).isZero();
        assertThat(outcomes.stream()
                .filter(outcome -> outcome == EnrollmentOutcome.ENROLLED)
                .count()).isEqualTo(1);
        assertThat(outcomes.stream()
                .filter(outcome -> outcome == EnrollmentOutcome.ALREADY_ENROLLED)
                .count()).isEqualTo(threads - 1);
    }
}
