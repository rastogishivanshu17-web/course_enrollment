# Design Specification — Bounded Course Enrollment & Waitlist

**Status:** Iteration 3 (domain model + contracts). Workflow logic lands in iterations 4–6.

**Scope:** A single course offering with bounded capacity and a FIFO waitlist.

---

## 1. Service Boundary

The feature exposes two operations through the `EnrollmentService` contract:

| Operation | Signature | Responsibility |
|---|---|---|
| Enroll | `EnrollmentResult enroll(Learner learner)` | Seat the learner if capacity exists; otherwise waitlist an eligible learner. Idempotent for already-active learners. |
| Drop | `EnrollmentResult drop(String learnerId)` | Free the seat of an enrolled learner and promote the earliest eligible waitlisted learner. No-op if not enrolled. |

Design choices:

- **Outcome-oriented, not exception-driven.** Expected business conditions (duplicate enroll, duplicate waitlist, ineligibility, dropping a non-enrolled learner) are returned as `EnrollmentResult`/`EnrollmentOutcome` values. Exceptions are reserved for true invalid input / programming errors. This keeps normal flows readable and lets the API layer map outcomes to HTTP codes.
- **The service is the sole owner of invariants.** The store provides storage primitives only; all compound decisions happen in the service under a single lock.

---

## 2. Domain Model

| Type | Kind | Purpose |
|---|---|---|
| `CourseOffering` | class | Immutable `id` + fixed positive `capacity`; pure predicates `hasAvailableSeat(count)` / `isFull(count)`. Holds no registration state. |
| `Learner` | record | `id` + `eligible` prerequisite flag. |
| `Registration` | class | One per learner id. Immutable `learnerId`, `sequence`, and `eligible` (captured at enroll time for promotion re-check); mutable `status`. FIFO order derives from `sequence`, independent of status. |
| `RegistrationStatus` | enum | `ENROLLED`, `WAITLISTED`, `DROPPED`. `ENROLLED`/`WAITLISTED` are the two active states. |
| `EnrollmentOutcome` | enum | `ENROLLED`, `WAITLISTED`, `ALREADY_ENROLLED`, `ALREADY_WAITLISTED`, `NOT_ELIGIBLE`, `DROPPED`, `NOT_ENROLLED`. |
| `EnrollmentResult` | record | `(outcome, learnerId, message)` — immutable operation result. |

Design choices:

- **FIFO via sequence, not insertion/iteration order.** A monotonic sequence assigned at creation gives a stable total order that survives status changes and concurrent-map iteration. `waitlistInFifoOrder()` sorts by it.
- **Capacity state lives in the store, not the offering.** `CourseOffering` predicates take the current enrolled count as a parameter, keeping them side-effect free and trivially testable.
- **Minimal mutability.** Only `Registration.status` changes, through intention-revealing methods (`markEnrolled` / `markWaitlisted` / `markDropped`) rather than a public setter.

---

## 3. Storage Abstraction

`RegistrationStore` (interface) with `InMemoryRegistrationStore` (`ConcurrentHashMap` keyed by learner id + `AtomicLong` sequence). Methods: `findByLearnerId`, `save`, `nextSequence`, `countByStatus`, `waitlistInFifoOrder`, `clear`.

- One `Registration` per learner id enforces the **single active registration** rule structurally — a re-enrollment updates the same record rather than creating a duplicate.
- In-memory only; no database. Limitation (non-durable, single-JVM) documented in Iteration 8.

---

## 4. Business Rules (approved assumptions)

1. Capacity is fixed and positive.
2. Eligibility = a single prerequisite flag, checked at enrollment **and** again at promotion.
3. Eligible learner with a free seat → `ENROLLED`.
4. Eligible learner, course full → `WAITLISTED`.
5. Ineligible learner → `NOT_ELIGIBLE` (never enrolled or waitlisted).
6. A learner has at most one active registration; repeat requests are idempotent and never alter waitlist order.
7. Dropping a non-enrolled learner changes nothing; repeated drops change nothing and trigger no extra promotion.
8. Waitlist is strict FIFO: promotion skips ineligible learners but **retains** them; the earliest eligible learner is promoted; if none is eligible, the seat stays open.

---

## 5. Engineering Invariants (to be enforced & tested)

1. Enrolled count never exceeds capacity.
2. A learner is never simultaneously enrolled and waitlisted.
3. No duplicate active registrations.
4. Ineligible learners are never enrolled.
5. Ineligible learners are never promoted.
6. Failed operations do not partially modify state.
7. Promotion preserves relative order of remaining waitlisted learners.
8. Successful drop + promotion leave state consistent.
9. Concurrent operations cannot oversubscribe capacity.

Invariants 1–4 are partially supported today by `CourseOffering` predicates, single-record-per-learner storage, and status modelling. Full enforcement arrives with the service implementation (Iterations 4–5) and is proven by tests there and in Iteration 7.

---

## 6. Eligibility Policy

Eligibility is a boolean prerequisite carried on `Learner`. It is evaluated:

- **At enrollment:** ineligible → `NOT_ELIGIBLE`, no state change.
- **At promotion:** the store returns the FIFO waitlist; the service promotes the first learner whose current eligibility holds, skipping (but keeping) ineligible entries. If no waitlisted learner is eligible, the freed seat remains open.

Because eligibility is supplied per request, promotion re-evaluation uses the eligibility captured on each `Registration` at enroll time (the `eligible` field), since the originating `Learner` request object is no longer available during a later drop-triggered promotion.

**Reachability note (justified):** The public `enroll` API only ever waitlists *eligible* learners, so under normal flow every waitlisted entry is eligible. The promotion routine nonetheless re-checks eligibility defensively, as the assumptions require (“eligibility is checked again during promotion”). An ineligible waitlisted entry models a learner whose eligibility was revoked after waitlisting. This skip-and-retain branch is exercised in tests by arranging such state directly in the store, since no public endpoint revokes eligibility (a policy engine is explicitly out of scope).

---

## 7. Concurrency Approach

- **Strategy:** a single intrinsic lock owned by the service implementation serializes every compound read-modify-write (check capacity → enroll/waitlist; drop → promote). This guarantees invariants 1–9 under concurrent requests.
- **Why coarse-grained:** the domain is a single offering with simple state; one lock is the simplest correct design and avoids subtle multi-lock ordering bugs. The critical sections are short (in-memory map operations).
- **Store role:** `ConcurrentHashMap` + `AtomicLong` make individual operations safe, but the store does **not** attempt to guarantee multi-step business atomicity — that is the service's job.
- **Memory visibility:** `Registration.status` is a plain field, which is safe because every read and write of registration state happens inside the same synchronized(lock) block (reads via `countByStatus` / `waitlistInFifoOrder`, writes via the service). The monitor establishes a happens-before relationship, so no additional `volatile` is required. This holds only while all access goes through the service under the lock, which is the case in production (the controller calls the service exclusively).
- **Partial-failure window:** with the in-memory store, the operations used in drop+promotion (`markDropped` / `markEnrolled` / `save` / `waitlistInFifoOrder`) do not throw, so there is no partial-commit window; the whole drop+promotion runs under one lock. A database-backed store would instead require a transaction spanning the drop and promotion to preserve invariant 6.
- **Honesty note:** these are JVM in-process guarantees. No database or distributed locking is involved, and unit/Mockito tests do not prove transactional or DB-level locking behavior. The concurrency tests (`DefaultEnrollmentServiceConcurrencyTest`) demonstrate only in-JVM thread safety of the single-lock strategy.

---

## 8. Implementation Status

All planned iterations are implemented and verified:

- Domain model + store + service contract (Iteration 3).
- `DefaultEnrollmentService` enrollment/waitlisting (Iteration 4).
- `drop` + FIFO promotion with skip-and-retain (Iteration 5).
- REST controllers, DTOs, Bean Validation, centralized error handling (Iteration 6) — see `docs/api.md`.
- Independent review + concurrency tests (Iteration 7).
- Final documentation (Iteration 8): `setup-and-testing.md`, `limitations-and-risks.md`, `rubric-assessment.md`.

Latest verified result: `mvnw.cmd clean test` → Tests run: 52, Failures: 0, Errors: 0 — BUILD SUCCESS.
