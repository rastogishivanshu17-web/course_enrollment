# Setup and Testing

## Requirements

| Tool | Version used / required | Notes |
|---|---|---|
| JDK | **Java 17** | The project targets Java 17 (`<java.version>17</java.version>`). Maven and the wrapper in this environment run on Zulu JDK 17. Java 21 is not installed; Java 17 is the compatible version used. |
| Maven | **3.9.11** (or the bundled wrapper **3.10.0**) | Prefer the wrapper for reproducibility. |
| Spring Boot | **3.3.5** | Declared via `spring-boot-starter-parent`. |

> Note: the `java` executable on the system `PATH` in the development environment is JDK 11, but Maven and the Maven wrapper execute the build/tests on JDK 17. Use the wrapper so the correct toolchain is used.

## Project layout

```text
com.project.course_enrollment
├── CourseEnrollmentApplication      # Spring Boot entry point
├── config/    CourseOfferingConfig  # builds the single CourseOffering bean from properties
├── domain/    CourseOffering, Learner, Registration, RegistrationStatus,
│              EnrollmentOutcome, EnrollmentResult
├── service/   EnrollmentService (contract), DefaultEnrollmentService (rules + locking)
├── store/     RegistrationStore (contract), InMemoryRegistrationStore
├── api/       EnrollmentController + dto (EnrollmentRequest, EnrollmentResponse)
└── error/     ApiError, OfferingNotFoundException, GlobalExceptionHandler
```

## Configuration

`src/main/resources/application.properties`:

```properties
spring.application.name=course_enrollment
course.offering.id=CS101
course.offering.capacity=2
```

- `course.offering.id` — the single offering ID; requests to any other ID return 404.
- `course.offering.capacity` — fixed, positive seat capacity.

Both have defaults in `CourseOfferingConfig` (`CS101`, `2`) if the properties are absent.

## Build and run

Run the application (Windows PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

On Unix-like shells:

```bash
./mvnw spring-boot:run
```

The application starts an embedded server on the default port `8080`.

## Example requests

```bash
# Enroll an eligible learner
curl -X POST http://localhost:8080/api/offerings/CS101/enrollments \
  -H "Content-Type: application/json" \
  -d '{"learnerId":"L1","eligible":true}'

# Drop a learner (promotes earliest eligible waitlisted learner if any)
curl -X DELETE http://localhost:8080/api/offerings/CS101/enrollments/L1
```

See `docs/api.md` for the full endpoint and status-code reference.

## Testing

Run the full test suite:

```powershell
.\mvnw.cmd test
```

or

```bash
./mvnw test
```

### Actual latest result (verified)

Command executed: `.\mvnw.cmd clean test`

```text
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Per-class breakdown (as reported by Surefire):

| Test class | Tests |
|---|---:|
| `domain.CourseOfferingTest` | 3 |
| `domain.LearnerTest` | 2 |
| `domain.RegistrationTest` | 4 |
| `store.InMemoryRegistrationStoreTest` | 6 |
| `service.DefaultEnrollmentServiceTest` | 9 |
| `service.DefaultEnrollmentServiceMockTest` | 2 |
| `service.DefaultEnrollmentServiceDropTest` | 11 |
| `service.DefaultEnrollmentServiceConcurrencyTest` | 2 |
| `api.EnrollmentControllerTest` | 9 |
| `api.EnrollmentApiIntegrationTest` | 3 |
| `CourseEnrollmentApplicationTests` | 1 |
| **Total** | **52** |

> The concurrency tests demonstrate in-JVM thread safety of the single-lock strategy only. They do not prove database transaction or distributed-locking behavior (there is no database).
