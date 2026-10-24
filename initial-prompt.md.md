# Master Prompt: AI-Assisted Course Waitlist Engineering Assessment

## Role

Act as a senior Java backend engineer, Spring Boot developer, test engineer, and code reviewer.

Help me implement a bounded course enrollment and waitlist feature from an empty folder.

Teach me through small, reviewable iterations. Explain important engineering decisions so I can understand and defend the implementation.

---

# Technology Stack

- Java 21 (if installed and compatible)
- Spring Boot 3.x (use a compatible stable version)
- Maven
- JUnit 5
- Mockito
- Spring Boot Test and MockMvc where appropriate
- Jakarta Bean Validation where appropriate
- In-memory storage for the initial implementation unless a justified persistence requirement emerges

Use standard Spring Boot Maven project conventions.

Avoid unnecessary dependencies.

Do not introduce a database without a demonstrated requirement.

---

# Scope

Implement enrollment, dropping, and FIFO waitlist promotion for a single course offering.

## Required Behavior

### Enrollment

- Enroll an eligible learner when a seat is available.
- Add an eligible learner to the waitlist when the course is full.
- Prevent duplicate active registrations.
- Reject learners who fail prerequisites.

### Drop

- Drop a learner who is currently enrolled.
- Return a clear result when attempting to drop a learner who is not enrolled.
- Repeated drops must not free additional seats or trigger additional promotions.

### Waitlist Promotion

- Promote the earliest eligible learner when a seat opens.
- Skip ineligible learners during promotion while retaining their waitlist entries.
- Preserve FIFO waitlist order.

### Error Handling

Return clear results for:

- Duplicate enrollment requests
- Duplicate waitlist requests
- Invalid requests
- Missing learners
- Repeated drops
- Prerequisite failures

### State Consistency

Preserve:

- Enrollment capacity
- Queue order
- Registration consistency

### Concurrency

Protect compound state changes against concurrent requests using an appropriate synchronization strategy.

---

# Business Assumptions

Use these assumptions unless a documented technical constraint requires a change.

## Course Offering

- One course offering exists.
- Capacity is fixed and positive.

## Eligibility

- Learner eligibility is represented by a simple prerequisite flag (or equivalent minimal model).
- Eligibility is checked at enrollment time.
- Eligibility is checked again during promotion.

## Enrollment

- Eligible learner is enrolled immediately if capacity exists.
- Eligible learner joins the waitlist if capacity is full.
- Ineligible learner cannot enroll.

## Registration Rules

- A learner can have only one active registration for the offering.
- Repeated enrollment requests must not create duplicate records.
- Repeated enrollment requests must not alter waitlist order.

## Drop Rules

- Dropping a non-enrolled learner changes no state.
- Repeated drops change no state.
- Repeated drops do not trigger additional promotions.

## Waitlist Rules

- Waitlist entries maintain stable FIFO order.
- Promotion skips ineligible learners.
- Skipped learners remain on the waitlist.
- Earliest eligible learner is promoted.
- If no eligible learner exists, the seat remains open.

Document and consistently apply these assumptions.

---

# Engineering Invariants

The implementation must enforce and test the following:

1. Enrolled count never exceeds capacity.
2. A learner is never both enrolled and waitlisted.
3. A learner has no duplicate active registrations.
4. Ineligible learners are never enrolled.
5. Ineligible learners are never promoted.
6. Failed operations do not partially modify state.
7. Promotion preserves relative order of remaining waitlisted learners.
8. Successful drop and promotion leave the state consistent.
9. Concurrent operations cannot oversubscribe capacity.

---

# Architecture

Use a simple layered architecture.

## Domain Layer

Represent:

- Course offering
- Learner
- Registration state
- Eligibility

## Service Layer

Implement:

- Enrollment rules
- Drop rules
- Promotion rules
- Validation of business constraints

Keep business logic here.

## API Layer

Provide:

- REST endpoints
- DTOs
- Request validation

Controllers should remain thin.

## Repository / Store

Use a lightweight in-memory store abstraction when useful.

Do not introduce persistence infrastructure unless required.

## Error Handling

Provide centralized and consistent error handling.

## Testing

Create:

- Focused unit tests
- Integration tests where architecture justifies them

Do not create unnecessary abstractions.

---

# Mandatory Iterative Workflow

Complete the assignment strictly in the following order.

## Iteration 1: Environment Verification and Project Planning

### Objective

Verify the development environment and establish the implementation plan.

### Tasks

1. Verify Java installation and version.
2. Verify Maven installation and version.
3. Check whether a Maven wrapper exists.
4. Determine whether the working folder is empty.
5. List current contents if not empty.
6. Propose the minimal project structure.
7. Identify assumptions, risks, and constraints.

### Deliverables

- Environment report
- Folder inventory
- Proposed structure
- Initial implementation plan

### Stop Condition

Stop and wait for approval before creating or modifying files.

---

## Iteration 2: Spring Boot Project Setup

### Objective

Create a minimal runnable Spring Boot project.

### Tasks

- Create Maven project structure.
- Configure Spring Boot.
- Add only necessary dependencies.
- Create application entry point.
- Verify application startup.

### Deliverables

- Initial project
- Build configuration
- Runnable application

### Additional Requirement

Create:

`docs/ai-use-record.md`

and begin maintaining it from this iteration onward.

### Stop Condition

Wait for approval.

---

## Iteration 3: Domain Model and Design Specification

### Objective

Define the domain model and document design decisions.

### Tasks

Implement:

- CourseOffering
- Learner
- Registration state
- Enrollment result model
- Waitlist representation

Create:

`docs/design-specification.md`

Document:

- Assumptions
- Invariants
- Synchronization strategy
- Domain design choices

### Stop Condition

Wait for approval.

---

## Iteration 4: Enrollment Operation and Tests

### Objective

Implement learner enrollment.

### Required Coverage

- Enrollment with available capacity
- Enrollment when full
- Duplicate enrollment
- Duplicate waitlist request
- Prerequisite failure
- Registration invariants

### Deliverables

- Service implementation
- Unit tests

### Stop Condition

Wait for approval.

---

## Iteration 5: Drop Operation and FIFO Promotion

### Objective

Implement dropping learners and promotion behavior.

### Required Coverage

- FIFO promotion
- Skipping ineligible learners
- Retaining skipped learners
- Multiple ineligible learners before an eligible learner
- No eligible learner available
- Invalid drop
- Repeated drop

### Deliverables

- Service implementation
- Unit tests

### Stop Condition

Wait for approval.

---

## Iteration 6: REST API, Validation, and Error Handling

### Objective

Expose functionality through REST APIs.

### Tasks

Implement:

- Controllers
- Request DTOs
- Response DTOs
- Bean Validation
- Exception handling

### Required Coverage

- API validation
- Error responses
- Integration tests

### Stop Condition

Wait for approval.

---

## Iteration 7: Independent Verification and Concurrency Review

### Objective

Validate correctness and concurrency behavior.

### Tasks

Review:

- Capacity guarantees
- Concurrency protection
- State consistency
- Invariants

Add practical concurrency tests where feasible.

### Deliverables

- Verification evidence
- Concurrency review
- Additional tests if needed

### Stop Condition

Wait for approval.

---

## Iteration 8: Final Documentation and Assessment

### Objective

Finalize project documentation.

### Required Documents

Create:

- `docs/setup-and-testing.md`
- `docs/limitations-and-risks.md`
- `docs/rubric-assessment.md`

### Rubric Requirement

If `Course-Registration-Rubric-Score.md` exists:

- Assess against actual rubric criteria.
- Provide evidence-based scoring.

If the rubric file does not exist:

- State clearly that it is unavailable.
- Leave rubric assessment pending.

Do not invent rubric criteria or scores.

---

# After Every Iteration

Provide the following:

## 1. Objective

Explain the purpose of the iteration.

## 2. Files Created or Modified

List every file touched.

## 3. Implementation

Implement only the scoped work.

Do not pre-build future iterations.

## 4. Verification

Run relevant commands where possible.

Examples:

```bash
./mvnw test
mvn test
./mvnw spring-boot:run
mvn spring-boot:run
```

Only report actual command results.

Clearly distinguish:

- Verified results
- Expected results
- Not executed

## 5. Regression Review

Review changes and identify any regressions.

## 6. AI-Use Record Update

Update:

`docs/ai-use-record.md`

Include:

- Actual prompt used
- AI response excerpt or summary
- Decision (accepted, edited, rejected)
- Reason
- Affected files
- Verification performed
- Actual results

Do not fabricate evidence.

## 7. Summary

Provide:

- Decisions made
- Work completed
- Remaining risks
- Remaining work

## 8. Approval Gate

Stop and wait for approval before continuing.

---

# Testing Requirements

Minimum coverage must include:

- Enrollment with available capacity
- Enrollment when full
- Duplicate enrollment
- Duplicate waitlist request
- Prerequisite failure
- FIFO promotion
- Ineligible learner skipped but retained
- Multiple ineligible learners before an eligible learner
- No eligible learner to promote
- Invalid drop
- Repeated drop
- Capacity invariants
- Registration invariants
- Invalid inputs
- Missing entities
- Concurrency scenarios where practical

## Testing Guidance

- Use JUnit 5.
- Use Mockito only to isolate collaborators.
- Do not mock core business logic.
- Do not claim Mockito tests prove real transaction or database locking behavior.

---

# Verification and Quality Requirements

## Build Tool

Use Maven Wrapper if available.

Otherwise use Maven.

## Allowed Commands

```bash
./mvnw test
```

```bash
mvn test
```

```bash
./mvnw spring-boot:run
```

```bash
mvn spring-boot:run
```

Use only commands relevant to the generated project.

## Reporting Rules

Never claim a command succeeded unless it actually ran.

Always distinguish:

- Executed and verified
- Not executed
- Expected outcome

## Review Requirements

Perform independent review for:

- Edge cases
- Correctness
- Concurrency
- State consistency
- Maintainability
- Architectural simplicity

Identify unnecessary complexity and remove it when appropriate.

---

# Scope Restrictions

Do not build:

- Frontend
- Course catalog
- Payments
- Authentication integrations
- Identity management
- Multi-term registration
- Policy engines
- Database infrastructure
- Unrelated features

Do not introduce a database merely to appear production-ready.

Document in-memory storage limitations instead.

---

# Final Submission Requirements

Provide:

## Files Created or Modified

Complete inventory of all files.

## Implemented Behavior

Summary of all implemented functionality.

## Verification Evidence

Actual commands executed and actual outcomes.

## Design Decisions

Key architectural and implementation decisions.

## Known Limitations

Document remaining limitations and risks.

## Remaining Work

Document anything incomplete or deferred.

---
