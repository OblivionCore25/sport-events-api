# 🏆 Sport Events API

> A production-ready REST API for managing sport events — built with Spring Boot 3.

[![Java](https://img.shields.io/badge/Java-23-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9-blue?logo=apachemaven)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-33%20passing-success?logo=junit5)](https://junit.org/junit5/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 📋 Table of Contents

- [About the Project](#-about-the-project)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running the App](#running-the-app)
- [API Reference](#-api-reference)
  - [Create Event](#1-create-a-sport-event)
  - [List Events](#2-list-sport-events)
  - [Get Event by ID](#3-get-event-by-id)
  - [Change Event Status](#4-change-event-status)
  - [SSE Stream](#5-real-time-sse-stream)
  - [List Sport Types](#6-list-configured-sport-types)
- [Status Lifecycle](#-status-lifecycle)
- [Configuration](#-configuration)
- [Running Tests](#-running-tests)
- [Error Handling](#-error-handling)
- [AI Assistance Disclosure](#-ai-assistance-disclosure)
- [Contributing](#-contributing)

---

## 📖 About the Project

The **Sport Events API** provides a clean HTTP interface to create, manage, and monitor sport events in real time. It enforces a strict event lifecycle (`INACTIVE → ACTIVE → FINISHED`) with clear business rules and pushes live status updates to subscribers via **Server-Sent Events (SSE)**.

Storage is intentionally kept in-memory (`ConcurrentHashMap`) — no database setup required. The application is ready to run out of the box.

---

## ✨ Features

- ✅ Full **CRUD** for sport events
- ✅ **Status lifecycle enforcement** with descriptive error messages
- ✅ **Real-time push** via Server-Sent Events (SSE)
- ✅ **Config-file-driven sport types** — add new sports without changing code
- ✅ **Structured JSON error responses** for all error cases
- ✅ **33 automated tests** — unit + integration
- ✅ Zero external dependencies — runs entirely in memory

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 23 |
| Framework | Spring Boot 3.3.1 |
| Web | Spring MVC (REST + SSE) |
| Validation | Jakarta Bean Validation |
| Boilerplate | Lombok |
| Build | Apache Maven 3.9 |
| Testing | JUnit 5, Mockito, MockMvc |
| Storage | In-memory `ConcurrentHashMap` |

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/entain/sportevents/
│   │   ├── SportEventsApplication.java      # Entry point
│   │   ├── config/
│   │   │   └── SportTypesConfig.java        # Loads sport types from application.yml
│   │   ├── controller/
│   │   │   └── SportEventController.java    # REST endpoints
│   │   ├── dto/
│   │   │   ├── CreateEventRequest.java      # POST request body
│   │   │   ├── UpdateStatusRequest.java     # PATCH request body
│   │   │   └── EventResponse.java           # Outbound response DTO
│   │   ├── exception/
│   │   │   ├── EventNotFoundException.java
│   │   │   ├── InvalidStatusTransitionException.java
│   │   │   └── GlobalExceptionHandler.java  # Centralised error handling
│   │   ├── model/
│   │   │   ├── SportEvent.java              # Domain model
│   │   │   └── EventStatus.java            # Enum: INACTIVE | ACTIVE | FINISHED
│   │   ├── repository/
│   │   │   └── InMemoryEventRepository.java # Thread-safe in-memory store
│   │   ├── service/
│   │   │   └── SportEventService.java       # Business logic & transition rules
│   │   ├── sse/
│   │   │   └── SseEmitterRegistry.java      # Manages SSE subscriber connections
│   │   └── validation/
│   │       ├── ValidSportType.java          # Custom constraint annotation
│   │       └── SportTypeValidator.java      # Validates against configured list
│   └── resources/
│       └── application.yml                  # App config (port, sport types)
└── test/
    └── java/com/entain/sportevents/
        ├── controller/
        │   └── SportEventControllerTest.java  # 19 MockMvc integration tests
        └── service/
            └── SportEventServiceTest.java     # 14 Mockito unit tests
```

---

## 🚀 Getting Started

### Prerequisites

Make sure you have the following installed:

| Tool | Minimum Version | Check |
|------|----------------|-------|
| JDK | 21+ | `java --version` |
| Maven | 3.8+ | `mvn --version` |

> No database, no Docker, no environment variables required.

### Installation

```bash
# Clone the repository
git clone https://github.com/OblivionCore25/sport-events-api.git

# Navigate into the project directory
cd sport-events-api
```

### Running the App

```bash
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

To confirm it's up, hit the sports endpoint:

```bash
curl http://localhost:8080/api/sports
# → ["FOOTBALL","HOCKEY","TENNIS","BASKETBALL","BASEBALL","RUGBY","CRICKET"]
```

---

## 📡 API Reference

Base URL: `http://localhost:8080`

All request and response bodies use `Content-Type: application/json`.  
Dates follow **ISO 8601** format: `YYYY-MM-DDTHH:mm:ss`

---

### 1. Create a Sport Event

```
POST /api/events
```

**Request body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | `string` | ✅ | Event name (non-blank) |
| `sport` | `string` | ✅ | Sport type — see [configured values](#-configuration) (case-insensitive) |
| `startTime` | `string` | ✅ | Scheduled start — ISO 8601 datetime |

**Example:**

```bash
curl -s -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Champions League Final",
    "sport": "FOOTBALL",
    "startTime": "2027-05-30T20:45:00"
  }'
```

**Response `201 Created`:**

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Champions League Final",
  "sport": "FOOTBALL",
  "status": "INACTIVE",
  "startTime": "2027-05-30T20:45:00",
  "createdAt": "2026-06-25T12:00:00",
  "updatedAt": "2026-06-25T12:00:00"
}
```

> New events always start as **`INACTIVE`**.

**More examples:**

```json
{ "name": "Stanley Cup Game 7",   "sport": "HOCKEY",      "startTime": "2027-06-15T19:00:00" }
{ "name": "Wimbledon Mens Final",  "sport": "TENNIS",      "startTime": "2027-07-13T14:00:00" }
{ "name": "NBA Finals Game 6",     "sport": "BASKETBALL",  "startTime": "2027-06-20T21:00:00" }
{ "name": "Six Nations Match",     "sport": "RUGBY",       "startTime": "2027-03-15T15:30:00" }
```

---

### 2. List Sport Events

```
GET /api/events
```

Optional query parameters:

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | `string` | Filter by status: `INACTIVE`, `ACTIVE`, or `FINISHED` |
| `sport` | `string` | Filter by sport type (case-insensitive) |

Both filters can be combined (AND semantics). Omitting either means "no filter on that dimension".

**Examples:**

```bash
# All events
curl -s http://localhost:8080/api/events

# Only active events
curl -s "http://localhost:8080/api/events?status=ACTIVE"

# Only football events
curl -s "http://localhost:8080/api/events?sport=FOOTBALL"

# Active football events
curl -s "http://localhost:8080/api/events?status=ACTIVE&sport=FOOTBALL"
```

**Response `200 OK`:** Array of event objects.

---

### 3. Get Event by ID

```
GET /api/events/{id}
```

```bash
curl -s http://localhost:8080/api/events/3fa85f64-5717-4562-b3fc-2c963f66afa6
```

| Scenario | Status |
|----------|--------|
| Found | `200 OK` |
| Not found | `404 Not Found` |

---

### 4. Change Event Status

```
PATCH /api/events/{id}/status
```

**Request body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | `string` | ✅ | Target status: `ACTIVE` or `FINISHED` |

**Example — activate an event:**

```bash
curl -s -X PATCH http://localhost:8080/api/events/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "ACTIVE"}'
```

**Example — finish an active event:**

```bash
curl -s -X PATCH http://localhost:8080/api/events/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "FINISHED"}'
```

All transition rules are enforced — see [Status Lifecycle](#-status-lifecycle) for details.

| Scenario | Status |
|----------|--------|
| Valid transition | `200 OK` |
| Event not found | `404 Not Found` |
| Invalid transition | `422 Unprocessable Entity` |

---

### 5. Real-time SSE Stream

```
GET /api/events/stream
```

Subscribe to receive a **live push notification** every time any event's status changes.

```bash
# Keep this running in a separate terminal
curl -N http://localhost:8080/api/events/stream
```

When a status change occurs, all connected clients instantly receive:

```
event:event-update
data:{"id":"...","name":"Champions League Final","sport":"FOOTBALL","status":"ACTIVE",...}
```

> Multiple clients can connect simultaneously — all receive the same broadcast.

---

### 6. List Configured Sport Types

```
GET /api/sports
```

Returns the list of valid sport types as defined in `application.yml`.

```bash
curl -s http://localhost:8080/api/sports
```

**Response `200 OK`:**

```json
["FOOTBALL", "HOCKEY", "TENNIS", "BASKETBALL", "BASEBALL", "RUGBY", "CRICKET"]
```

---

## 🔄 Status Lifecycle

Events follow a strict one-way lifecycle:

```
INACTIVE ──────────────────► ACTIVE ──► FINISHED
    │                           │
    │   (only if startTime      │
    │    is NOT in the past)    │
    │                           │
    ✗ INACTIVE → FINISHED       ✗ ACTIVE → INACTIVE
    ✗ FINISHED → anything       ✗ Same → Same (no-op)
```

| Transition | Allowed | Condition |
|-----------|---------|-----------|
| `INACTIVE → ACTIVE` | ✅ | `startTime` must not be in the past |
| `ACTIVE → FINISHED` | ✅ | Always |
| `INACTIVE → FINISHED` | ❌ | Must activate first |
| `FINISHED → any` | ❌ | Terminal state — no exits |
| `ACTIVE → INACTIVE` | ❌ | No rollback |

Violations return `422 Unprocessable Entity` with a descriptive message.

---

## ⚙️ Configuration

All configuration lives in [`src/main/resources/application.yml`](src/main/resources/application.yml):

```yaml
server:
  port: 8080                  # Change port here

sport:
  types:                      # Add or remove sport types — no code changes needed
    - FOOTBALL
    - HOCKEY
    - TENNIS
    - BASKETBALL
    - BASEBALL
    - RUGBY
    - CRICKET
```

### Adding a new sport type

1. Open `application.yml`
2. Add the new entry under `sport.types`
3. Restart the application

That's it — no Java changes required.

---

## 🧪 Running Tests

```bash
mvn test
```

**Expected output:**
```
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Test breakdown

| Test class | Type | Count | What's covered |
|-----------|------|-------|---------------|
| `SportEventServiceTest` | Unit (Mockito) | 14 | All transition rules, create, read |
| `SportEventControllerTest` | Integration (MockMvc) | 19 | All endpoints, validation, filters |

Run only unit tests:
```bash
mvn test -Dtest=SportEventServiceTest
```

Run only integration tests:
```bash
mvn test -Dtest=SportEventControllerTest
```

---

## 🚨 Error Handling

All errors return a structured JSON body:

```json
{
  "timestamp": "2026-06-25T12:00:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Cannot activate event: start time (2020-01-01T10:00:00) is in the past."
}
```

Validation errors additionally include a `fieldErrors` map:

```json
{
  "timestamp": "2026-06-25T12:00:00",
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": {
    "sport": "Invalid sport type. Check GET /api/sports for the list of valid types."
  }
}
```

| HTTP Status | Cause |
|------------|-------|
| `400 Bad Request` | Missing/invalid request fields |
| `404 Not Found` | Event ID does not exist |
| `422 Unprocessable Entity` | Forbidden status transition |
| `500 Internal Server Error` | Unexpected server error |

---

## 🤝 Contributing

Contributions are welcome! To get started:

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature-name`
3. Commit your changes: `git commit -m 'feat: add your feature'`
4. Push to your branch: `git push origin feat/your-feature-name`
5. Open a Pull Request

Please make sure all existing tests pass before submitting a PR:
```bash
mvn test
```

---

## 📄 License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for more information.

---

## 🤖 AI Assistance Disclosure

This project was built with the assistance of **Antigravity** (an AI coding assistant by Google DeepMind), used as a pair-programming tool throughout the development session.

### How it was used

| Area | AI involvement | Developer involvement |
|------|---------------|----------------------|
| **Architecture** | Proposed the layered structure (controller → service → repository → model) | Reviewed, questioned, and approved each layer |
| **Sport type design** | Suggested both enum and config-file options with trade-off analysis | Decided to use `application.yml` + `@ConfigurationProperties` after evaluating the options |
| **Code generation** | Generated initial implementations for all classes | Reviewed all generated code, reformatted to personal style, removed unused imports, and refactored test structure |
| **Status transition rules** | Implemented the `validateTransition()` logic based on the spec | Verified the logic against each rule in the requirements |
| **SSE implementation** | Implemented `SseEmitterRegistry` with `CopyOnWriteArrayList` | Understood the design, asked follow-up questions on production trade-offs |
| **Tests** | Generated unit and integration test scaffolding | Ran the tests, reviewed coverage, and independently understood each assertion |
| **Pre-submission review** | Identified gaps (missing Maven wrapper, LICENSE, test isolation, filter inconsistency) | Applied and validated each fix |
| **README** | Drafted the documentation | Reviewed content for accuracy |

### What the developer directed

- All design decisions were made after discussion — the AI presented options, the developer chose
- The decision to use SSE over WebSocket was evaluated and consciously chosen for this use case
- Reformatting of test files and removal of unused imports were done independently
- The developer understands and can explain every class, method, and design decision in the codebase

### References and documentation consulted

- [Spring Boot 3.3 Reference Documentation](https://docs.spring.io/spring-boot/docs/3.3.1/reference/html/)
- [Spring MVC — SseEmitter](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html)
- [Jakarta Bean Validation](https://jakarta.ee/specifications/bean-validation/3.0/)
- [Lombok Project](https://projectlombok.org/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

---

## 📬 Contact

**OblivionCore25** — [GitHub Profile](https://github.com/OblivionCore25)

Project link: [https://github.com/OblivionCore25/sport-events-api](https://github.com/OblivionCore25/sport-events-api)
