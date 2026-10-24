# API Reference — Course Enrollment

Base path: `/api/offerings/{offeringId}/enrollments`

There is a single configured offering. `{offeringId}` must match the configured offering ID (default: `CS101`, from `course.offering.id`). Any other ID returns **404 Not Found**.

All request and response bodies are JSON.

---

## Enroll a learner

`POST /api/offerings/{offeringId}/enrollments`

### Request body

```json
{
  "learnerId": "L1",
  "eligible": true
}
```

| Field | Type | Rules |
|---|---|---|
| `learnerId` | string | Required, non-blank |
| `eligible` | boolean | Required; must be explicitly provided |

### Responses

| Outcome | HTTP | Meaning |
|---|---|---|
| `ENROLLED` | `201 Created` | Seat available; learner enrolled |
| `WAITLISTED` | `202 Accepted` | Course full; eligible learner added to FIFO waitlist |
| `ALREADY_ENROLLED` | `200 OK` | Idempotent — learner already holds a seat |
| `ALREADY_WAITLISTED` | `200 OK` | Idempotent — learner already on the waitlist; order preserved |
| `NOT_ELIGIBLE` | `422 Unprocessable Entity` | Learner fails the prerequisite |

Success/outcome body:

```json
{
  "outcome": "ENROLLED",
  "learnerId": "L1",
  "message": "Learner enrolled"
}
```

---

## Drop a learner

`DELETE /api/offerings/{offeringId}/enrollments/{learnerId}`

Dropping an enrolled learner frees the seat and promotes the earliest **eligible** waitlisted learner. Ineligible waitlisted learners are skipped but retained. If no waitlisted learner is eligible, the seat stays open.

### Responses

| Outcome | HTTP | Meaning |
|---|---|---|
| `DROPPED` | `200 OK` | Learner was enrolled and has been dropped; promotion may have occurred |
| `NOT_ENROLLED` | `200 OK` | Learner is not currently enrolled (missing, waitlisted, or already dropped); no state change |

Response body:

```json
{
  "outcome": "DROPPED",
  "learnerId": "L1",
  "message": "Learner dropped"
}
```

---

## Error responses

Consistent error body produced by the centralized `@RestControllerAdvice`:

```json
{
  "timestamp": "2026-10-23T08:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "fieldErrors": {
    "learnerId": "learnerId must not be blank"
  }
}
```

`fieldErrors` is present only for validation failures.

| Condition | HTTP |
|---|---|
| Validation failure (blank `learnerId`, missing `eligible`) | `400 Bad Request` |
| Malformed or missing JSON body | `400 Bad Request` |
| Unknown `offeringId` | `404 Not Found` |

---

## Design notes

- Controllers are thin: they validate the offering ID, delegate to `EnrollmentService`, and map the business `EnrollmentOutcome` to an HTTP status. Business rules stay in the service layer.
- Expected business conditions (duplicates, ineligibility, dropping a non-enrolled learner) are returned as `2xx` or `422` outcomes rather than thrown exceptions. Invalid input and unknown offerings produce `4xx` errors.
