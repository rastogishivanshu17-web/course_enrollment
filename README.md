# Course Enrollment API

A Spring Boot REST API for managing learner enrollment in a course
offering. It supports eligibility validation, capacity-aware enrollment,
FIFO waitlisting, idempotent repeat requests, and promotion of the next
eligible waitlisted learner when a seat is freed.

## Technology Stack

-   Java 17
-   Spring Boot 3.3.5
-   Maven Wrapper
-   Spring Web and Bean Validation
-   JUnit/Mockito-based tests (see `pom.xml` for the configured test
    dependencies)

## Prerequisites

-   JDK 17
-   Git (to clone the repository)
-   No separate Maven installation is required when using the included
    Maven Wrapper.

## Build and Test

Clone the repository and enter the project directory:

``` bash
git clone https://github.com/rastogishivanshu17-web/course_enrollment.git
cd course_enrollment
```

On Windows PowerShell:

``` powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

``` bash
chmod +x ./mvnw
./mvnw clean test
./mvnw spring-boot:run
```

The application uses the default Spring Boot port unless configured
otherwise. Check `src/main/resources/application.properties` for the
active configuration.

## API Overview

All request and response bodies are JSON. The default configured
offering ID is `CS101`; the requested `offeringId` must match the
configured ID.

### Enroll a learner

`POST /api/offerings/CS101/enrollments`

Request:

``` json
{
  "learnerId": "L1",
  "eligible": true
}
```

Example using `curl`:

``` bash
curl -i -X POST "http://localhost:8080/api/offerings/CS101/enrollments" \
  -H "Content-Type: application/json" \
  -d '{"learnerId":"L1","eligible":true}'
```

Possible outcomes include:

-   `201 Created` --- learner enrolled because a seat is available.
-   `202 Accepted` --- course is full and the eligible learner is
    waitlisted.
-   `200 OK` --- learner is already enrolled or already waitlisted.
-   `422 Unprocessable Entity` --- learner is not eligible.

### Drop a learner

`DELETE /api/offerings/CS101/enrollments/L1`

Example:

``` bash
curl -i -X DELETE "http://localhost:8080/api/offerings/CS101/enrollments/L1"
```

Dropping an enrolled learner frees a seat and promotes the earliest
eligible learner on the FIFO waitlist, if one exists.

### Error handling

Validation failures and malformed requests return `400 Bad Request`. An
unknown offering ID returns `404 Not Found`. Error responses use the
centralized API error format documented in the API reference.

## Design and Behavior

-   Business rules are handled in the service layer; controllers remain
    focused on HTTP request/response handling.
-   Enrollment requests validate the learner ID and require an explicit
    `eligible` value.
-   Repeated enrollment requests are handled idempotently.
-   When the course is full, eligible learners can be added to a FIFO
    waitlist.
-   When a seat is freed, the earliest eligible waitlisted learner is
    promoted; ineligible waitlisted learners are skipped and retained.
-   Review the design documentation for concurrency behavior and the
    limitations of the in-memory, single-process implementation.

## Project Documentation

-   [API Reference](docs/api.md)
-   [Design Specification](docs/design-specification.md)
-   [Setup and Testing](docs/setup-and-testing.md)
-   [Limitations and Risks](docs/limitations-and-risks.md)
-   [AI Use Record](docs/ai-use-record.md)
-   [Rubric Assessment](docs/rubric-assessment.md)
-   [Initial Prompt / Assessment Context](intial-prompt.md)

## Assessment Notes

The original `Course-Registration-Rubric-Score.md` referenced in the
assessment instructions was not available to the candidate. The
repository's rubric assessment documents this limitation rather than
inventing scores for an unavailable scorecard.

## Scope and Limitations

This project is scoped as an assessment implementation. In-memory state
is not persistent across application restarts, and concurrency
guarantees are limited to the running application instance. See
[Limitations and Risks](docs/limitations-and-risks.md) for details.
