# Limitations and Risks

This document records the known limitations, operational behavior, and risks of the current implementation. The feature was intentionally kept minimal per the assessment scope.

## 1. In-memory storage

- **Non-durable.** `InMemoryRegistrationStore` keeps all registrations in a `ConcurrentHashMap` held in the JVM heap. There is no database and no file persistence.
- **Restart behavior.** All enrollment and waitlist state is **lost on application restart** (or crash). After a restart the course offering is empty: no enrolled learners, no waitlist, and the FIFO `sequence` counter resets to 0.
- **Single JVM only.** State lives in one process. Running multiple application instances would give each its own independent, inconsistent state — the implementation is **not** horizontally scalable as-is.
- **Unbounded growth.** Dropped registrations are retained (status `DROPPED`) rather than removed, and the map is never pruned. For this bounded, single-offering assessment the volume is trivial, but a long-lived deployment would need a retention/cleanup policy.

## 2. Concurrency assumptions

- **Strategy.** All compound state transitions (enroll; drop + promotion) are serialized by a single intrinsic lock owned by the singleton `DefaultEnrollmentService`. This guarantees the capacity and single-active-registration invariants and keeps a drop and its triggered promotion atomic.
- **Scope of guarantee.** These are **in-JVM** guarantees. They hold only because:
  1. the service is a singleton Spring bean (one shared lock), and
  2. every read and write of registration state goes through the service under that lock.
- **Not proven.** Database transactions, row-level locking, or distributed locking are not covered. There is no database. The concurrency tests (`DefaultEnrollmentServiceConcurrencyTest`) demonstrate only that the single-lock strategy prevents oversubscription and duplicate active registrations within one JVM.
- **Throughput.** The coarse-grained lock serializes all write operations. For a single offering with short critical sections this is appropriate; it would become a bottleneck at high scale, where a finer-grained or storage-level concurrency mechanism would be needed.

## 3. Eligibility model

- Eligibility is a **single boolean prerequisite flag** supplied per request and captured on the `Registration` at enroll time. There is no real prerequisite engine (explicitly out of scope).
- The public `enroll` API never waitlists an ineligible learner, so under normal flow every waitlisted entry is eligible. The promotion routine still **re-checks** eligibility defensively (per the assumption that eligibility is checked again at promotion). An ineligible waitlisted entry models a learner whose eligibility was revoked after waitlisting; because no public endpoint revokes eligibility, that skip-and-retain branch is exercised by arranging store state directly in tests.

## 4. API / behavioral notes

- **Single offering.** Only the configured `course.offering.id` is valid; any other ID returns 404. Multi-offering support is out of scope.
- **Drop semantics.** Dropping a non-enrolled learner (missing, waitlisted, or already dropped) returns `NOT_ENROLLED` with HTTP 200 and changes no state, rather than 404. This is deliberate, idempotent behavior documented in `docs/api.md`.
- **No authentication/authorization.** Endpoints are open; identity and access management are out of scope.

## 5. Tooling / dependency notes

- `@MockBean` (used in `EnrollmentControllerTest`) is deprecated in Spring Boot 3.4. It works on the current 3.3.5 but should be migrated to `@MockitoBean` if the Spring Boot version is upgraded.
- Dependencies are minimal: `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-test`. No database driver or unnecessary libraries are included.

## 6. Out-of-scope (not implemented, by design)

Frontend, course catalog, payments, authentication/identity, multi-term registration, policy engines, and database infrastructure are intentionally excluded per the assessment scope.
