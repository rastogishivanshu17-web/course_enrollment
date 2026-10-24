# AI Use Record

This document records how AI assistance was used throughout the project, as required by the engineering assessment. Each entry captures the actual prompt, the decision taken, the reason, the files affected, and the verification performed.

---

## Iteration 1 – Environment Verification and Project Planning

- **Prompt (summary):** "Execute Iteration 1 only. Inspect the workspace, verify Java and Maven versions, confirm whether the folder is empty, and propose a minimal Spring Boot Maven project (Java, JUnit 5, Mockito). Do not modify files."
- **AI response (summary):** Ran `java -version`, `mvn -version`, and `./mvnw.cmd -version`. Reported: JDK 11 on PATH, Maven 3.9.11 and wrapper 3.10.0 both running on JDK 17; Java 21 not installed. Folder not empty (Spring Initializr skeleton present). Flagged that the existing `pom.xml` used Spring Boot 4.0.9 while the prompt required Spring Boot 3.x. Proposed package structure, responsibilities, test classes, and documentation plan.
- **Decision:** Accepted. No files modified during Iteration 1.
- **Reason:** Planning-only iteration; stop condition required awaiting approval.
- **Affected files:** None.
- **Verification performed:** `java -version`, `mvn -version`, and `./mvnw.cmd -version` executed and results reported. Verified.

---

## Iteration 2 – Spring Boot Project Setup

- **Prompt (summary):** "Yes, downgrade it" — approval to downgrade from Spring Boot 4.0.9 to a stable Spring Boot 3.x and proceed with the minimal runnable project setup.
- **AI response (summary):** Edited `pom.xml` to use `spring-boot-starter-parent` 3.3.5, kept `java.version` at 17, replaced the bare `spring-boot-starter` with `spring-boot-starter-web`, and added `spring-boot-starter-validation`. `spring-boot-starter-test` (JUnit 5 + Mockito) retained. Created this AI-use record.
- **Decision:** Accepted (edited existing POM).
- **Reason:** Align with the master prompt's Spring Boot 3.x requirement and the available Java 17 toolchain; add only the dependencies needed for REST, validation, and testing.
- **Affected files:**
  - `pom.xml` (modified)
  - `docs/ai-use-record.md` (created)
- **Verification performed:** See build/test results recorded in the Iteration 2 summary.

### Iteration 2 – Package Structure Establishment

- **Prompt (summary):** "Execute Iteration 2 only... Add a main application class, create the initial package structure, add a minimal application context test, run the build and tests."
- **AI response (summary):** Confirmed the existing `CourseEnrollmentApplication` main class and the `CourseEnrollmentApplicationTests` context-load test. Established the layered package structure via documented `package-info.java` placeholders (no business logic): `domain`, `service`, `store`, `api`, `api.dto`, `error`.
- **Decision:** Accepted.
- **Reason:** Make the layered architecture visible without pre-building future iterations.
- **Affected files:**
  - `src/main/java/.../domain/package-info.java` (created)
  - `src/main/java/.../service/package-info.java` (created)
  - `src/main/java/.../store/package-info.java` (created)
  - `src/main/java/.../api/package-info.java` (created)
  - `src/main/java/.../api/dto/package-info.java` (created)
  - `src/main/java/.../error/package-info.java` (created)
- **Verification performed:** `./mvnw.cmd clean test` — see Iteration 2 summary.

---

## Iteration 3 – Domain Model and Design Specification

- **Prompt (summary):** "Execute Iteration 3 only. Design the minimal domain model and service contract for one course offering using the approved assumptions (fixed positive capacity, simple prerequisite eligibility, unique active registration, FIFO waitlist, ineligible waitlisted learners skipped but retained, no promotion if none eligible). Inspect the project first; implement only the domain model, registration representation, and minimal service/repository abstractions. Create `docs/design-specification.md`. Add focused tests. Do not implement the full enroll/drop workflow."
- **AI response (summary):** Inspected the project (Spring Boot 3.3.5, Java 17, empty layered packages). Implemented domain types `RegistrationStatus`, `Learner`, `CourseOffering`, `Registration`, `EnrollmentOutcome`, `EnrollmentResult`; a `RegistrationStore` interface with an `InMemoryRegistrationStore` (`ConcurrentHashMap` + `AtomicLong`; FIFO via sequence); and an `EnrollmentService` contract (interface only, no workflow). Added focused tests for domain validation, status transitions, and FIFO/counting store behavior. Authored `docs/design-specification.md` covering the service boundary, model, rules, invariants, eligibility policy, and concurrency approach.
- **Decision:** Accepted.
- **Reason:** Establishes a tested foundation and a clear contract for Iterations 4–6 without pre-building workflow logic.
- **Affected files:**
  - `domain/RegistrationStatus.java`, `domain/Learner.java`, `domain/CourseOffering.java`, `domain/Registration.java`, `domain/EnrollmentOutcome.java`, `domain/EnrollmentResult.java` (created)
  - `store/RegistrationStore.java`, `store/InMemoryRegistrationStore.java` (created)
  - `service/EnrollmentService.java` (created)
  - `test/.../domain/CourseOfferingTest.java`, `RegistrationTest.java`, `LearnerTest.java` (created)
  - `test/.../store/InMemoryRegistrationStoreTest.java` (created)
  - `docs/design-specification.md` (created)
  - `docs/ai-use-record.md` (updated)
- **Verification performed:** `./mvnw.cmd clean test` — results recorded in the Iteration 3 summary.

---

## Iteration 4 – Enrollment Operation and Tests

- **Prompt (summary):** "Execute Iteration 4 only. Implement the enrollment service operation: validate identifiers/entities, reject ineligible learners, enroll when capacity is available, waitlist when full, prevent duplicate active registrations, preserve waitlist order on duplicate requests, no partial state change on failure, maintain capacity/registration invariants. Write JUnit 5 tests (available capacity, full capacity, prerequisite failure, duplicate enrollment, invalid inputs). Use Mockito at collaborator boundaries; test real outcomes and state, not only mock interactions."
- **Pre-work clarification:** Before Iteration 4, explained that the `eligible` flag is a per-request boolean prerequisite checked at enrollment and again at promotion, and recommended persisting it on `Registration` so promotion can re-check it. User approved continuing.
- **AI response (summary):** Added an immutable `eligible` field to `Registration` (new constructor arity). Implemented `DefaultEnrollmentService` with a single-lock strategy guarding the compound check-duplicate → check-eligibility → check-capacity → persist sequence; `drop` throws `UnsupportedOperationException` (deferred to Iteration 5). Added `CourseOfferingConfig` bean providing the `CourseOffering` with properties `id=CS101`, `capacity=2`. Wrote state-based tests (`DefaultEnrollmentServiceTest`, 9) and a Mockito boundary test (`DefaultEnrollmentServiceMockTest`, 2) proving rejected requests write nothing. Updated two existing tests for the new `Registration` constructor.
- **Decision:** Accepted (with the agreed `Registration.eligible` addition).
- **Reason:** Implements the required enrollment behavior and invariants while preparing the waitlist to retain eligibility for Iteration 5 promotion.
- **Affected files:**
  - `domain/Registration.java` (modified — added `eligible`)
  - `service/DefaultEnrollmentService.java` (created)
  - `config/CourseOfferingConfig.java` (created)
  - `resources/application.properties` (modified — course offering properties)
  - `test/.../service/DefaultEnrollmentServiceTest.java` (created)
  - `test/.../service/DefaultEnrollmentServiceMockTest.java` (created)
  - `test/.../domain/RegistrationTest.java` and `test/.../store/InMemoryRegistrationStoreTest.java` (modified — constructor arity)
  - `docs/ai-use-record.md` (updated)
- **Verification performed:** `./mvnw.cmd clean test` → **BUILD SUCCESS, 27 tests, 0 failures, 0 errors** (9 enrollment state tests, 2 Mockito boundary tests, plus domain/store/context tests).

---

## Iteration 5 – Drop Operation and FIFO Promotion

- **Prompt (summary):** "Execute Iteration 5 only. Implement dropping an enrolled learner and promoting the earliest eligible waitlisted learner: valid drop removes enrollment; non-enrolled drop returns a clear result and changes no state; repeated drops free no extra seats; after a valid drop, find and promote the earliest currently eligible waitlisted learner; preserve FIFO order; skip but retain ineligible waitlisted learners; leave the seat open if none eligible; preserve invariants. Keep drop+promotion atomic. Add JUnit 5 tests (FIFO promotion, ineligible retained, multiple ineligible, no eligible candidate, invalid drop, repeated drop, state consistency). Update the design spec if needed."
- **AI response (summary):** Implemented `drop(String)` in `DefaultEnrollmentService`: validates the ID, returns `NOT_ENROLLED` (no state change) when the learner is missing/waitlisted/already dropped, otherwise marks the registration dropped and calls private `promoteEarliestEligible()`. Drop and promotion run inside the existing single lock, so the operation is atomic within the in-memory implementation. `waitlistInFifoOrder()` is traversed and promotes the first eligible entry, skipping and retaining ineligible entries; relative order is preserved. Added `DefaultEnrollmentServiceDropTest` (10 tests). Updated the design specification with a justified reachability note explaining that ineligible waitlisted entries (revoked eligibility) are arranged directly in the store to exercise the defensive skip-and-retain branch.
- **Decision:** Accepted.
- **Reason:** Fulfills all drop/promotion rules and invariants with an atomic, single-lock boundary and realistic, state-based tests.
- **Affected files:**
  - `service/DefaultEnrollmentService.java` (modified — `drop` + `promoteEarliestEligible`, Javadoc)
  - `test/.../service/DefaultEnrollmentServiceDropTest.java` (created)
  - `docs/design-specification.md` (updated — eligibility reachability note; concurrency atomicity)
  - `docs/ai-use-record.md` (updated)
- **Verification performed:** `./mvnw.cmd clean test` → **BUILD SUCCESS, 37 tests, 0 failures, 0 errors** (10 new drop/promotion tests plus all prior suites).

---

## Iteration 6 – REST API, Validation, and Error Handling

- **Prompt (summary):** "Execute Iteration 6 only. Add the smallest suitable REST API for enroll and drop (`POST/DELETE` under `/api/offerings/{offeringId}/enrollments`). Use DTOs; validate requests; return consistent documented HTTP responses; handle invalid input, missing entities, duplicates, prerequisite failures; add centralized exception handling; use MockMvc + Mockito for controller tests; don't change unrelated logic. Update API docs and the AI-use record."
- **AI response (summary):** Added `EnrollmentController` (thin — validates offering ID, delegates to the service, maps `EnrollmentOutcome` to HTTP status: `ENROLLED` 201, `WAITLISTED` 202, `ALREADY_*`/`DROPPED`/`NOT_ENROLLED` 200, `NOT_ELIGIBLE` 422). Added DTOs `EnrollmentRequest` (`@NotBlank learnerId`, `@NotNull eligible`) and `EnrollmentResponse`. Added centralized `GlobalExceptionHandler` (`@RestControllerAdvice`) producing a consistent `ApiError` body for validation (400 + field errors), malformed JSON (400), `IllegalArgumentException` (400), and `OfferingNotFoundException` (404). Added `@WebMvcTest` controller tests (9, service mocked) and an `@SpringBootTest` + MockMvc integration test (3, real service/store). Wrote `docs/api.md`. No business logic changed.
- **Decision:** Accepted.
- **Reason:** Exposes the service over HTTP with consistent, validated, well-documented responses while keeping all rules in the service layer.
- **Affected files:**
  - `api/EnrollmentController.java` (created)
  - `api/dto/EnrollmentRequest.java`, `api/dto/EnrollmentResponse.java` (created)
  - `error/ApiError.java`, `error/OfferingNotFoundException.java`, `error/GlobalExceptionHandler.java` (created)
  - `test/.../api/EnrollmentControllerTest.java`, `test/.../api/EnrollmentApiIntegrationTest.java` (created)
  - `docs/api.md` (created)
  - `docs/ai-use-record.md` (updated)
- **Verification performed:** `./mvnw.cmd clean test` → **BUILD SUCCESS, 49 tests, 0 failures, 0 errors** (9 controller + 3 integration tests plus all prior suites).

---

## Iteration 7 – Independent Verification and Concurrency Review

- **Prompt (summary):** "Execute Iteration 7 only. Act as an independent reviewer. Inspect code and tests for capacity overflow, duplicate active registrations, prerequisite handling, FIFO violations, accidental removal of ineligible waitlist entries, invalid/repeated drops changing state, partial failure during drop+promotion, unsafe concurrency, inadequate/misleading tests, unnecessary dependencies/abstractions. Add missing tests, fix confirmed defects, select/explain a synchronization strategy, and don't claim unit tests prove transaction guarantees."
- **AI response (summary/findings):** Audited `DefaultEnrollmentService`, `InMemoryRegistrationStore`, and all tests. **No functional defects confirmed.**
  - **Capacity:** enrolled-count read and save occur atomically under one lock; no overflow.
  - **Duplicates:** one record per learner ID plus duplicate check under lock ensures one active registration.
  - **Prerequisites:** checked at enrollment and re-checked at promotion; duplicate check precedes eligibility so an already-enrolled learner is never evicted.
  - **FIFO:** `waitlistInFifoOrder()` sorts by immutable sequence; promotion mutates only the promoted learner, preserving order.
  - **Ineligible retention:** promotion skips, never removes, ineligible entries.
  - **Invalid/repeated drops:** return `NOT_ENROLLED` with no state change.
  - **Partial failure:** in-memory operations do not throw; the whole drop+promotion runs under one lock, so there is no partial-commit window in this implementation. A database-backed store would need a transaction (documented).
  - **Visibility:** plain `status` field is safe because all reads/writes occur under the same monitor (documented).
  - **Abstractions/dependencies:** `RegistrationStore`/`EnrollmentService` interfaces are justified boundaries; `save()` replacing in-place mutation is intentional so the service does not assume the store returns live references. Dependencies (web, validation, test) are used; no unnecessary dependency identified.
  - **Gaps found:** no concurrency tests and no re-enrollment-after-drop test.
- **Changes made:** Added `DefaultEnrollmentServiceConcurrencyTest` (2 tests: 200 concurrent enrollments never oversubscribe capacity 5; 50 concurrent duplicate enrollments of the same learner yield exactly one active registration / one `ENROLLED` outcome). Added `droppedLearnerCanReenrollIntoAFreedSeat` to the drop test. Expanded the design spec's concurrency section (visibility, partial-failure window, honest note).
- **Decision:** Accepted (tests + docs only; no production code changed).
- **Reason:** The review value was verification plus closing the concurrency/re-enrollment test gaps.
- **Affected files:**
  - `test/.../service/DefaultEnrollmentServiceConcurrencyTest.java` (created)
  - `test/.../service/DefaultEnrollmentServiceDropTest.java` (modified — re-enrollment test)
  - `docs/design-specification.md` (updated — concurrency details)
  - `docs/ai-use-record.md` (updated)
- **Verification performed:** `./mvnw.cmd clean test` → **BUILD SUCCESS, 52 tests, 0 failures, 0 errors**. Concurrency tests are deterministic in their final-state assertions and demonstrate only in-JVM thread safety, not database transaction guarantees.

---

## Iteration 8 – Final Documentation and Assessment

- **Prompt (summary):** "Execute Iteration 8 only. Prepare final submission docs based on actual code: create/update design specification, AI-use record, setup-and-testing, limitations-and-risks, and rubric assessment. Document setup, Java/Maven requirements, startup, test commands (only appropriate ones), real test results, in-memory limitations, restart behavior, concurrency assumptions. Locate and read the real `Course-Registration-Rubric-Score.md`; if missing, do not invent criteria/scores — mark pending. Review repo for stray files, secrets, unneeded dependencies, placeholders. Run the final suite; report actual results, file inventory, limitations, outstanding issues."
- **AI response (summary):** Searched the workspace for any rubric file (`*rubric*`) — **none found**; marked rubric assessment as pending without inventing criteria. Reviewed the repository: no stray files, no secrets in `application.properties`, minimal dependencies (web, validation, test), and no source-code placeholders (the only `UnsupportedOperationException`/`placeholder` historical mentions are inside `ai-use-record.md`, not code). Created `docs/setup-and-testing.md`, `docs/limitations-and-risks.md`, `docs/rubric-assessment.md`; updated `docs/design-specification.md`'s implementation status section. Ran the full suite.
- **Decision:** Accepted (documentation only; no code changed).
- **Reason:** Finalize submission documentation grounded in the actual implementation and verified test results, without fabricating a rubric.
- **Affected files:**
  - `docs/setup-and-testing.md` (created)
  - `docs/limitations-and-risks.md` (created)
  - `docs/rubric-assessment.md` (created)
  - `docs/design-specification.md` (updated — implementation status)
  - `docs/ai-use-record.md` (updated)
- **Verification performed:** `.\mvnw.cmd clean test` → **BUILD SUCCESS, 52 tests, 0 failures, 0 errors, 0 skipped** (actual output captured). Rubric file confirmed absent via workspace search.

---

## Post-submission – Package-info Cleanup

- **Prompt (summary):** "Are package-info needed?" — followed by "Yes, delete," then "Check all files and update which are needed that got affected."
- **AI response (summary):** Deleted the six documentation-only `package-info.java` files. They carried no package-level annotations and were not used for published Javadoc; their planned Iteration 6 wording had become stale now that each package contains its real classes. Removing them reduces clutter with zero behavioral impact.
- **Decision:** Deleted (edit).
- **Reason:** The files added no technical value and had drifted out of date.
- **Affected files:**
  - Deleted: `api/package-info.java`, `api/dto/package-info.java`, `domain/package-info.java`, `error/package-info.java`, `service/package-info.java`, `store/package-info.java`
  - `docs/ai-use-record.md` (updated — this entry)
- **Verification performed:** `.\mvnw.cmd clean test` → **BUILD SUCCESS**, 52 tests, 0 failures, 0 errors, 0 skipped; 18 source files compiled.

**Note:** The Iteration 2 and 7 entries above intentionally retain their original wording as a historical audit trail; this entry records the later removal rather than rewriting that history.

<!-- Future iterations appended below -->
