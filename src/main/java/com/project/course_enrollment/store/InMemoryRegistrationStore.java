package com.project.course_enrollment.store;

import com.project.course_enrollment.domain.Registration;
import com.project.course_enrollment.domain.RegistrationStatus;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory {@link RegistrationStore} backed by a {@link ConcurrentHashMap}
 * keyed by learner id.
 *
 * <p>A single {@link Registration} per learner id is retained; its status reflects
 * the learner's current lifecycle state (ENROLLED / WAITLISTED / DROPPED). FIFO
 * ordering is derived from the immutable {@link Registration#sequence()} assigned
 * via {@link #nextSequence()}.
 *
 * <p>Individual map operations are thread-safe. Business-level atomicity across
 * multiple operations is provided by the service layer, not here.
 */
@Repository
public class InMemoryRegistrationStore implements RegistrationStore {

    private final ConcurrentHashMap<String, Registration> registrations =
            new ConcurrentHashMap<>();
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    @Override
    public Optional<Registration> findByLearnerId(String learnerId) {
        if (learnerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registrations.get(learnerId));
    }

    @Override
    public void save(Registration registration) {
        if (registration == null) {
            throw new IllegalArgumentException("registration must not be null");
        }
        registrations.put(registration.learnerId(), registration);
    }

    @Override
    public long nextSequence() {
        return sequenceGenerator.getAndIncrement();
    }

    @Override
    public int countByStatus(RegistrationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        return (int) registrations.values().stream()
                .filter(registration -> registration.status() == status)
                .count();
    }

    @Override
    public List<Registration> waitlistInFifoOrder() {
        return registrations.values().stream()
                .filter(Registration::isWaitlisted)
                .sorted(Comparator.comparingLong(Registration::sequence))
                .toList();
    }

    @Override
    public void clear() {
        registrations.clear();
        sequenceGenerator.set(0);
    }
}
