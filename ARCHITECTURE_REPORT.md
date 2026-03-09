# Architecture Report: acode-learn-backend

> Generated: 2026-03-09
> Reference project: fleetat-be (Spring Modulith reference)

---

## 1. Project Overview

| Aspect | Value |
|--------|-------|
| Framework | Spring Boot 4.0.2 |
| Architecture | Spring Modulith (Modular Monolith) |
| Java | 21 |
| Persistence | PostgreSQL + Liquibase |
| Auth | OAuth2 JWT (external auth server) |
| Modules | 4 (`course`, `resource`, `user`, `shared`) |

---

## 2. Module Structure

```
gr.alexc.acodelearn/
├── course/          → Course & CourseSection management
├── resource/        → Learning resources (links, files, code, guides, etc.)
├── user/            → User identity (OAuth2-backed)
└── shared/          → Cross-cutting: audit, security, CORS, exceptions
```

### Module Dependency Graph

```
shared ← course
shared ← resource
shared ← user
user   ← course   (via UserLookup interface)
resource ← course (via ResourceLookup interface)
```

No circular dependencies exist. All inter-module dependencies go through public interfaces (`UserLookup`, `ResourceLookup`), which is correct.

---

## 3. What is Well Done ✅

- **Module declarations**: Each module has `package-info.java` with `@ApplicationModule`
- **Internal encapsulation**: Repositories are hidden in `internal/` subpackage
- **Interface-based cross-module dependencies**: `UserLookup` and `ResourceLookup` interfaces used correctly
- **Modulith verification test**: `ModulithVerificationTest` exists
- **Domain events**: `Course` extends `AbstractAggregateRoot<Course>` and registers events
- **Audit fields**: Consistent `BaseEntity` with `createdAt`, `updatedAt`, `version` (optimistic locking)
- **Liquibase migrations**: Schema versioned and managed properly
- **Security**: Stateless JWT with method-level `@PreAuthorize`
- **Separation of concerns**: Command/Query split, separate handlers

---

## 4. Issues and Recommendations

### 4.1 Missing `allowedDependencies` in `@ApplicationModule` — HIGH

**Problem:** Module declarations exist but don't explicitly declare allowed inter-module dependencies:

```java
// current (all modules)
@org.springframework.modulith.ApplicationModule(displayName = "Course Module")
package gr.alexc.acodelearn.course;
```

**Fix:** Add `allowedDependencies` to make dependencies explicit and verifiable:

```java
// course module - depends on user and resource
@org.springframework.modulith.ApplicationModule(
    displayName = "Course Module",
    allowedDependencies = {"user", "resource", "shared"}
)
package gr.alexc.acodelearn.course;

// resource module - only shared
@org.springframework.modulith.ApplicationModule(
    displayName = "Resource Module",
    allowedDependencies = {"shared"}
)
package gr.alexc.acodelearn.resource;

// user module - only shared
@org.springframework.modulith.ApplicationModule(
    displayName = "User Module",
    allowedDependencies = {"shared"}
)
package gr.alexc.acodelearn.user;
```

This makes the dependency graph part of the code and is enforced by the Modulith verification test.

**Reference:** fleetat-be uses this pattern in all modules (`allowedDependencies = {"vehicle"}`).

---

### 4.2 Events Published but Never Consumed — HIGH

**Problem:** `CourseSectionCreatedEvent` and `CourseUpdatedEvent` are published by the `Course` aggregate but no `@ApplicationModuleListener` exists anywhere in the codebase. The `event_publication` table and Spring Modulith events-jpa infrastructure are set up and wasted.

**Decide one of:**
- **Option A — Remove events** if there's no planned use. Remove event classes, `AbstractAggregateRoot` extension, and the JPA event dependency.
- **Option B — Implement listeners** in modules that need to react to course changes. Example: if resources should be cleaned up when a course changes, implement in `resource` module:

```java
// resource/internal/CourseEventListener.java
@ApplicationModuleListener
void onCourseUpdated(CourseUpdatedEvent event) {
    // react to course changes
}
```

**Reference:** fleetat-be uses `@ApplicationModuleListener` in `driver/internal/VehicleEventListener.java` with a separate handler service for the business logic.

---

### 4.3 Cross-Module Entity References — MEDIUM

**Problem:** `Course` holds direct JPA `@ManyToMany` references to `User` entities and `Resource` entities via `CourseSection`. This couples the JPA persistence graphs across module boundaries.

```java
// In Course.java - tight coupling to User entity
@ManyToMany
@JoinTable(name = "course_has_user", ...)
private List<User> studentsEnrolled;
```

**Better pattern (from fleetat-be):** Reference other aggregates by ID only, not by JPA entity reference:

```java
// Store only IDs
private List<Long> enrolledStudentIds;
private List<Long> instructorIds;
```

Cross-module joins should happen at the service/query layer using the public API (`UserLookup`, `ResourceLookup`) rather than through JPA relationships.

**Trade-off:** This is a significant refactor. If the current approach works and module boundaries are otherwise respected, it can stay — but document the decision. The main risk is that JPA will eagerly/lazily cross module boundaries in ways that are hard to control.

---

### 4.4 `ResourceLookup` Located in `resource` Module Root — MEDIUM

**Problem:** `ResourceLookup` (used by the `course` module) is a public interface in the `resource` module root — that part is fine. But `ResourceQueryHandler` (which implements it) is also in the `resource` module root (not in `internal/`), making the implementation visible.

```
resource/
├── ResourceLookup.java       ← public (correct)
├── ResourceQueryHandler.java ← should be in internal/
└── internal/
    └── ResourceRepository.java
```

**Fix:** Move `ResourceQueryHandler` to `resource/internal/`:

```
resource/
├── ResourceLookup.java           ← public interface (correct)
└── internal/
    ├── ResourceQueryHandler.java ← implementation hidden
    └── ResourceRepository.java
```

Same issue applies to `UserLookup` / `UserQueryHandler` in the `user` module.

---

### 4.5 Missing `ModuleDocumentation` Generation in Test — MEDIUM

**Problem:** `ModulithVerificationTest` only calls `verify()`. The fleetat-be reference project also calls `Documenter` to auto-generate PlantUML module diagrams.

**Fix:**

```java
@Test
void createModuleDocumentation() {
    ApplicationModules modules = ApplicationModules.of(AcodeLearnBackendApplication.class);
    new Documenter(modules)
        .writeDocumentation();
}
```

Add `spring-modulith-docs` to `build.gradle`:
```groovy
testImplementation 'org.springframework.modulith:spring-modulith-docs'
```

This generates visual architecture diagrams automatically, making the module structure visible to all team members.

---

### 4.6 File Upload Security — MEDIUM

**Problem:** File resources are stored as raw `BYTEA` in the database with minimal validation:

- Only check is `fileName.contains("..")` (path traversal)
- No file size limits configured
- No file type whitelist validation (trusts `Content-Type` header)
- Storing binary blobs in DB degrades query performance over time

**Recommendations:**
1. Add upload size limit in `application.yaml`:
   ```yaml
   spring:
     servlet:
       multipart:
         max-file-size: 10MB
         max-request-size: 10MB
   ```
2. Validate file extension against a whitelist in `ResourceCommandHandler`
3. Consider external file storage (S3/MinIO) instead of database `BYTEA` for production

---

### 4.7 Missing `OptimisticLockingFailureException` Handling — LOW

**Problem:** All entities have `@Version` for optimistic locking, but `GlobalExceptionHandler` doesn't handle `OptimisticLockingFailureException`. Concurrent updates will return a 500 Internal Server Error instead of a meaningful 409 Conflict.

**Fix:** Add to `GlobalExceptionHandler`:

```java
@ExceptionHandler(OptimisticLockingFailureException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public ErrorResponse handleOptimisticLocking(OptimisticLockingFailureException ex) {
    return ErrorResponse.of(409, "Resource was modified by another request. Please retry.");
}
```

---

### 4.8 Missing Pagination on List Endpoints — LOW

**Problem:** `getSections()`, `getUserCourses()`, `getCourseResources()` return unbounded `List<>` results. As data grows, these will become slow and memory-intensive.

**Fix:** Add `Pageable` parameter support:

```java
@GetMapping("/user-courses")
public Page<CourseSummaryView> getUserCourses(
    @AuthenticationPrincipal Jwt jwt,
    Pageable pageable) { ... }
```

---

### 4.9 `shared` Module Should Not Be a Modulith Module — LOW

**Problem:** `shared` is declared as `@ApplicationModule`, but it functions as infrastructure/cross-cutting concern — not as a business domain module. Treating it as a module makes dependency management awkward (every module must declare `shared` as an allowed dependency).

**Consider:** In Spring Modulith, infrastructure code at the root package level is automatically accessible to all modules without being a module. You could move `shared` contents up to a `infrastructure/` package directly under `gr.alexc.acodelearn` and remove the `@ApplicationModule` declaration. This is an alternative pattern — the current approach works but adds boilerplate.

---

### 4.10 `Resource.courseId` as Non-JPA FK — INFORMATIONAL

**Observation:** `Resource` stores `courseId` as a plain `Long` instead of a `@ManyToOne` JPA relationship. This is actually a good Spring Modulith pattern (referencing across modules by ID only), but it's inconsistently applied (Course holds full JPA references to User and Resource).

**Action:** Document this as an intentional design decision. If cross-module entity references (issue 4.3) are refactored, this will become the consistent pattern everywhere.

---

## 5. Summary Table

| # | Issue | Priority | Category | Action |
|---|-------|----------|----------|--------|
| 4.1 | Missing `allowedDependencies` | HIGH | Architecture | Add to all `package-info.java` |
| 4.2 | Unused domain events | HIGH | Architecture | Implement listeners or remove |
| 4.3 | Cross-module JPA entity refs | MEDIUM | Architecture | Refactor to ID-only references |
| 4.4 | Implementation classes not in `internal/` | MEDIUM | Encapsulation | Move QueryHandlers to `internal/` |
| 4.5 | No module documentation generation | MEDIUM | DX | Add `Documenter` test + dependency |
| 4.6 | File upload security gaps | MEDIUM | Security | Size limits + type validation |
| 4.7 | No optimistic lock error handling | LOW | Resilience | Add 409 handler |
| 4.8 | No pagination | LOW | Performance | Add `Pageable` to list endpoints |
| 4.9 | `shared` as formal module | LOW | Architecture | Consider moving to root infrastructure |
| 4.10 | `Resource.courseId` pattern inconsistency | INFO | Design | Document or make consistent |

---

## 6. Patterns to Adopt from fleetat-be

| Pattern | fleetat-be Location | Apply In |
|---------|---------------------|----------|
| `allowedDependencies` in `@ApplicationModule` | `package-info.java` of each module | All modules |
| `@ApplicationModuleListener` for cross-module events | `driver/internal/VehicleEventListener.java` | Any future event consumers |
| Separate listener → handler pattern | `VehicleEventListener` → `VehicleDecommissionedHandler` | Event consumers |
| `Documenter` in tests | `ModulithStructureTest.java` | `ModulithVerificationTest` |
| ID-only cross-module references | `Driver.assignedVehicleId` (UUID, not FK) | `Course` → `User`, `CourseSection` → `Resource` |
| Java records for DTOs | `VehicleDTO`, `DriverDTO` | Command/View objects (partial already) |
| Management interface as public API | `VehicleManagement` interface | `CourseService`, `ResourceService` extract interfaces |

---

---

## 7. Applied Changes Log

| Change | Status |
|--------|--------|
| `allowedDependencies` in all `package-info.java` | ✅ Done |
| `ResourceQueryHandler` + `ResourceSpecifications` → `resource/internal/` | ✅ Done |
| `UserQueryHandler` + `UserView` → `user/internal/` | ✅ Done |
| `OptimisticLockingFailureException` → 409 handler | ✅ Done |
| File upload size limits in `application.yaml` (20MB/25MB) | ✅ Done |
| `spring-modulith-docs` + `Documenter` in verification test | ✅ Done |
| `CourseSectionCreatedEvent` payload fixed (removed JPA entity) | ✅ Done |
| `Course` `@ManyToMany List<User>` → `@ElementCollection List<Long>` | ✅ Done |
| `CourseSection` `@ManyToMany List<Resource>` → `@ElementCollection List<Long>` | ✅ Done |
| `CourseService` passes `user.getId()` to aggregate methods | ✅ Done |
| `CourseRepository` derived queries updated for element collections | ✅ Done |

---

## 8. Quick Wins (Low effort, high value)

1. Add `allowedDependencies` to all `package-info.java` files → immediate architecture enforcement
2. Move `ResourceQueryHandler` and `UserQueryHandler` to `internal/` subpackages
3. Add `OptimisticLockingFailureException` handler to `GlobalExceptionHandler`
4. Add file size limits to `application.yaml`
5. Add `Documenter` call to `ModulithVerificationTest` and the docs dependency
