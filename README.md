# acode-learn-backend

Backend for the **Acode Learn** platform — a course management system that supports instructors and students with courses, sections, and learning resources (code snippets, markdown documents, guides, links, files, repositories).

## Stack

| Concern | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.2 |
| Architecture | Spring Modulith 2.0.2 |
| Build | Gradle 9.3 |
| Database | PostgreSQL (production), H2 in PostgreSQL mode (dev) |
| Schema migrations | Liquibase |
| Security | Spring Security 7 — OAuth2 JWT Resource Server |
| Persistence | Spring Data JPA (Hibernate) |
| Mapping | MapStruct 1.6.3 |
| Utilities | Lombok |

## Requirements

- **JDK 21** or higher
- **PostgreSQL** (for production profile)
- An OAuth2 Authorization Server exposing a JWK Set URI (for JWT validation)

The Gradle Wrapper is included — no global Gradle installation required.

## Profiles

| Profile | Datasource | Notes |
|---|---|---|
| `dev` (default) | H2 in-memory/file (PostgreSQL mode) | H2 console available at `/h2-console` |
| `prod` | PostgreSQL | Requires env vars |

## Running locally (dev)

```bash
./gradlew bootRun
```

The server starts on port **8082**. H2 console: `http://localhost:8082/h2-console`.

For the JWT validation to work in dev, set the authorization server URL:
```bash
AUTH_SERVER_JWK_URI=http://localhost:8081/oauth2/jwks ./gradlew bootRun
```
Or leave it unset — the dev profile defaults to `http://localhost:8081/oauth2/jwks`.

## Running in production

```bash
./gradlew bootJar
java -jar build/libs/acode-learn-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Environment Variables

| Variable | Profile | Required | Description |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | dev | No | Override H2 URL (default: file-based H2) |
| `SPRING_DATASOURCE_USERNAME` | dev | No | Datasource username (default: `sa`) |
| `SPRING_DATASOURCE_PASSWORD` | dev | No | Datasource password (default: `pass`) |
| `AUTH_SERVER_JWK_URI` | dev | No | JWK Set URI (default: `http://localhost:8081/oauth2/jwks`) |
| `DATABASE_URL` | prod | Yes | PostgreSQL JDBC URL |
| `DB_USERNAME` | prod | Yes | PostgreSQL username |
| `DB_PASSWORD` | prod | Yes | PostgreSQL password |
| `AUTH_SERVER_JWK_URI` | prod | Yes | JWK Set URI for JWT validation |

## Gradle commands

```bash
./gradlew build          # Compile, test, package
./gradlew bootRun        # Run with dev profile
./gradlew test           # Run test suite
./gradlew clean          # Delete build directory
./gradlew bootJar        # Build executable JAR
```

Test reports: `build/reports/tests/test/index.html`

## Architecture

The application follows a **Spring Modulith** structure with CQRS per module. Modules communicate via domain events (Spring Modulith outbox pattern) and never share internal packages.

```
gr.alexc.acodelearn
│
├── shared/                     # Cross-cutting concerns (no business logic)
│   ├── persistence/            # BaseEntity (auditing, @Version), AuditConfig
│   ├── security/               # SecurityConfig (JWT resource server)
│   └── web/                    # CorsConfig, GlobalExceptionHandler, ErrorResponse
│
├── user/                       # User module
│   ├── User.java               # Entity
│   ├── UserController.java
│   ├── query/                  # UserQueryHandler, UserView
│   └── internal/               # UserRepository (package-private)
│
├── resource/                   # Learning resource module
│   ├── Resource.java           # Sealed base entity
│   ├── [6 resource subtypes]   # CodeSnippet, File, Guide, Link, Markdown, Repository
│   ├── ResourceType.java       # Enum: LINK, FILE, REPOSITORY, CODE_SNIPPET, MARKDOWN, GUIDE
│   ├── ResourceController.java
│   ├── command/                # CreateResourceCommand, UpdateResourceCommand, DeleteResourceCommand
│   ├── query/                  # ResourceQueryHandler, ResourceSpecifications, ResourceSummaryView
│   └── internal/               # ResourceRepository, FileResourceRepository (package-private)
│
└── course/                     # Course aggregate module
    ├── Course.java             # Aggregate root (AbstractAggregateRoot)
    ├── CourseSection.java      # Entity inside aggregate
    ├── CourseController.java
    ├── [Domain events]         # CourseSectionCreatedEvent, CourseUpdatedEvent
    ├── command/                # 7 command records + CourseSectionCommandHandler
    ├── query/                  # CourseQueryHandler, CourseSummaryView, CourseSectionView
    └── internal/               # CourseRepository, CourseSectionRepository (package-private)
```

## Domain model

```
User ──< course_has_user >── Course ──< course_section >── CourseSection
         (enrolled)          │            (ordered)               │
                             │                                    └──< section_resources >── Resource*
User ──< user_has_course >── Course
         (owns/instructs)    └──< course_resources >── Resource*

Resource (sealed):
  ├── CodeSnippetResource
  ├── FileResource
  ├── GuideResource
  ├── LinkResource
  ├── MarkdownDocumentResource
  └── RepositoryResource
```

## API overview

| Method | Path | Role | Description |
|---|---|---|---|
| `GET` | `/user` | any | Get current authenticated user |
| `GET` | `/user-courses` | any | Courses the user is enrolled in |
| `GET` | `/instructor/owned-courses` | TEACHER | Courses owned by the instructor |
| `PUT` | `/course/{id}` | TEACHER | Update course metadata |
| `GET` | `/course/{id}/resources` | enrolled/owner | List course resources (optional `?type=`) |
| `GET` | `/course/{id}/resource/{rid}` | enrolled/owner | Get a specific resource |
| `GET` | `/course/{id}/resource/{rid}/file` | enrolled/owner | Download file resource |
| `POST` | `/course/{id}/resource` | TEACHER | Create a resource |
| `POST` | `/course/{id}/resource/file` | TEACHER | Upload a file resource |
| `PUT` | `/course/{id}/resource/{rid}` | TEACHER | Update a resource |
| `DELETE` | `/course/{id}/resource/{rid}` | TEACHER | Delete a resource |
| `GET` | `/course/{id}/sections` | enrolled/owner | List course sections |
| `GET` | `/course/{id}/sections/{sid}` | enrolled/owner | Get a section |
| `POST` | `/course/{id}/sections` | TEACHER | Create a section |
| `PUT` | `/course/{id}/sections/{sid}` | TEACHER | Update a section |
| `PUT` | `/course/{id}/sections/order` | TEACHER | Reorder sections |
| `DELETE` | `/course/{id}/sections/{sid}` | TEACHER | Delete a section |
| `POST` | `/course/{id}/sections/{sid}/resources` | TEACHER | Add resources to section |
| `DELETE` | `/course/{id}/sections/{sid}/resources` | TEACHER | Remove resources from section |

## Database migrations

Liquibase manages the schema. Migration files live in `src/main/resources/db/changelog/migrations/`:

| File | Description |
|---|---|
| `000-initial-setup.yaml` | Empty baseline changeset |
| `001-create-schema.yaml` | Full initial schema (all tables + foreign keys) |

## License

TODO: Add license information.
