# Rubric Assessment

## Status: PENDING — rubric file not available

As of this iteration, the rubric file **`Course-Registration-Rubric-Score.md` was not found** in the workspace. A recursive search for any file matching `*rubric*` returned no results.

Per the assessment instructions, when the rubric is unavailable the assessment must **not** invent criteria or scores. Therefore:

- No rubric criteria are assumed.
- No scores are assigned.
- This assessment is **pending** until the real rubric file is provided in the repository.

When `Course-Registration-Rubric-Score.md` (or the actual rubric) is added, re-run this assessment to score the implementation against its real criteria, citing concrete evidence (code locations and test names).

---

## Evidence available for scoring (reference only, not a score)

The following verifiable facts can be mapped to rubric criteria once the rubric exists. This section is descriptive evidence, not a self-assigned grade.

### Required behavior → evidence

| Required behavior | Where implemented | Test evidence |
|---|---|---|
| Enroll eligible learner when seat available | `DefaultEnrollmentService.enroll` | `DefaultEnrollmentServiceTest.enrollsEligibleLearnerWhenCapacityAvailable` |
| Waitlist eligible learner when full | `DefaultEnrollmentService.enroll` | `...waitlistsEligibleLearnerWhenFull` |
| Prevent duplicate active registrations / duplicate check under lock | `DefaultEnrollmentService` | `...duplicateEnrollmentIsIdempotentAndFreesNoExtraSeat`, `...duplicateWaitlistRequestPreservesFifoOrder` |
| Reject prerequisite failures | Eligibility check | `...rejectsIneligibleLearnerAndChangesNoState`, controller 422 test |
| Drop enrolled learner | `DefaultEnrollmentService.drop` | `DefaultEnrollmentServiceTest.validDropRemovesFromEnrolled...` |
| Clear result for non-enrolled drop | `NOT_ENROLLED` | `...dropMissingLearnerReturnsNotEnrolled...`, `...dropWaitlistedLearnerReturnsNotEnrolled...` |
| Repeated drops free no extra seats | `drop` no-op path | `...repeatedDropDoesNotFreeExtraSeatOrPromoteAgain` |
| Promote earliest eligible (FIFO) | `promoteEarliestEligible` | `...dropPromotesEarliestEligibleWaitlistedLearnerInFifoOrder` |
| Skip ineligible, retain them | `promoteEarliestEligible` | `...promotionSkipsSingleIneligibleAndRetainsIt`, `...promotionSkipsMultipleIneligibleBeforeEligible` |
| No eligible candidate → seat open | `promoteEarliestEligible` | `...noEligibleWaitlistedLearnerLeavesSeatOpen` |
| Centralized error handling | `GlobalExceptionHandler` | `EnrollmentControllerTest` 400/404 tests |
| Concurrency protection | Single intrinsic lock | `DefaultEnrollmentServiceConcurrencyTest` (in-JVM only) |

### Engineering invariants → evidence

| Invariant | Evidence |
|---|---|
| 1. Enrolled count never exceeds capacity | `...capacityInvariantIsNeverExceeded`; concurrency test asserts `ENROLLED == capacity` |
| 2. Never both enrolled and waitlisted | Single record per learner id; `...dropAndPromotionLeaveConsistentState` |
| 3. No duplicate active registrations | `...concurrentDuplicateEnrollSameLearnerProducesExactlyOneActiveRegistration` |
| 4. Ineligible never enrolled | `...rejectsIneligibleLearnerAndChangesNoState` |
| 5. Ineligible never promoted | Promotion skip tests |
| 6. Failed ops don't partially modify state | Mockito `never().save()` tests; single-lock atomicity |
| 7. Promotion preserves remaining order | `...promotionSkipsMultipleIneligibleBeforeEligible` asserts `containsExactly("W1", "W2", "W4")` |
| 8. Drop + promotion leave state consistent | `...dropAndPromotionLeaveConsistentState` |
| 9. Concurrent ops cannot oversubscribe | `...concurrentEnrollmentsNeverOversubscribeCapacity` (in-JVM) |

### Verified test result

Command: `.\mvnw.cmd clean test` → Tests run: 52, Failures: 0, Errors: 0, Skipped: 0 — **BUILD SUCCESS**.
