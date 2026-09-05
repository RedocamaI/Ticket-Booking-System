# Ticket Booking System

A microservices-based ticket booking system inspired by IRCTC, built to demonstrate
system design skills using Spring Boot, PostgreSQL, and Spring Cloud Gateway.

## Architecture

```
Client
  │
  ▼
API Gateway (port 8080)
  ├── /api/auth/**     → Auth Service (port 8081)
  ├── /api/trains/**   → Train Service (port 8082)
  ├── /api/admin/**    → Train Service (port 8082)
  ├── /api/schedules/**→ Train Service (port 8082)
  └── /api/bookings/** → Booking Service (port 8083)
```

### Services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| API Gateway | 8080 | None | JWT validation, routing |
| Auth Service | 8081 | auth_db | Registration, login, JWT issuance |
| Train Service | 8082 | train_db | Trains, schedules, seat inventory |
| Booking Service | 8083 | booking_db | Bookings, payments, cancellations |

## Key Design Decisions

### Database per Service
Each service owns its database exclusively. No cross-database joins.
Foreign keys across services are plain UUID columns — consistency is
enforced at the service layer, not the DB layer.

### Optimistic Locking
The `seats` table has a `@Version` column. Concurrent booking attempts
on the same seat result in one success and one `OptimisticLockingFailureException`
→ 423 LOCKED. This prevents double-booking without pessimistic DB locks.

### JWT at Gateway
JWT validation happens only in the API Gateway. Downstream services trust
the `X-User-Id` header injected by the gateway — no JWT code in any service.

### Seat Locking Flow
```
POST /api/bookings
  → Gateway validates JWT, injects X-User-Id
  → Booking Service calls Train Service /internal/seats/{id}/lock
  → Seat status set to LOCKED
  → Booking created in PENDING state with 10-minute expiry
  → @Scheduled job cancels expired PENDING bookings every 60 seconds
```

### Internal Endpoints
Train Service exposes `/internal/**` endpoints only for inter-service
communication. The gateway blocks all `/internal/**` routes from external clients.

## Tech Stack

| Component | Technology |
|---|---|
| Backend | Spring Boot 4.1.0, Java 25 |
| Database | PostgreSQL 18 |
| Migrations | Flyway |
| Gateway | Spring Cloud Gateway MVC |
| Auth | JWT (JJWT 0.12.x), BCrypt |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build | Maven (multi-module) |

## Running Locally

### Prerequisites
- Java 25
- Maven 3.9+
- PostgreSQL 18
- Docker Desktop (for tests)

### Setup

1. Clone the repository:
```bash
git clone https://github.com/RedocamaI/Ticket-Booking-System.git
cd ticket-booking-system
```

2. Create three databases in PostgreSQL:
```sql
CREATE DATABASE auth_db;
CREATE DATABASE train_db;
CREATE DATABASE booking_db;
```

3. Copy and fill in credentials for each service:
```bash
cp auth-service/src/main/resources/application.properties.example \
   auth-service/src/main/resources/application-local.properties
# repeat for train-service and booking-service
```

4. Start all services:
```bash
mvn spring-boot:run -pl auth-service
mvn spring-boot:run -pl train-service
mvn spring-boot:run -pl booking-service
mvn spring-boot:run -pl api-gateway
```

### Running Tests
```bash
mvn test -pl auth-service
mvn test -pl train-service
mvn test -pl booking-service
```

## API Endpoints

### Auth Service (via gateway: localhost:8080)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/auth/register | No | Register new user |
| POST | /api/auth/login | No | Login, returns JWT |

### Train Service (via gateway: localhost:8080)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | /api/trains | Yes | List all trains |
| GET | /api/trains/{id} | Yes | Get train by ID |
| GET | /api/trains/search | Yes | Search by source, destination, date |
| GET | /api/schedules/{id} | Yes | Get schedule by ID |
| GET | /api/schedules/{id}/seats | Yes | List seats for a schedule |
| POST | /api/admin/trains | Admin | Create a train |
| POST | /api/admin/schedules | Admin | Create a schedule + auto-generate seats |

### Booking Service (via gateway: localhost:8080)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/bookings | Yes | Create booking, lock seat |
| POST | /api/bookings/{id}/pay | Yes | Process mock payment |
| GET | /api/bookings | Yes | Get user's bookings |
| GET | /api/bookings/{id} | Yes | Get booking by ID |
| PATCH | /api/bookings/{id}/cancel | Yes | Cancel and refund |

## Swagger UI

| Service | URL |
|---|---|
| Auth Service | http://localhost:8081/swagger-ui.html |
| Train Service | http://localhost:8082/swagger-ui.html |
| Booking Service | http://localhost:8083/swagger-ui.html |

## Phase 2 — Planned Improvements

| Feature | What it replaces |
|---|---|
| Redis seat locking with TTL | `@Scheduled` expiry job → Redis auto-eviction |
| Kafka event bus | Sync REST calls → async events (booking.confirmed, seat.released) |
| Notification Service | Extract from booking-service, consume Kafka events |
| Payment Service | Extract payments table, integrate real gateway (Razorpay) |
| Eureka service discovery | Hardcoded URLs → dynamic resolution |
| Resilience4j circuit breaker | Unhandled service failures → graceful degradation |
| Docker Compose | Manual service startup → single command |

## Phase 3 — Future

| Feature | Description |
|---|---|
| Multi-stop routes | Trains with intermediate stops, flexible search |
| Coach configuration | Replace 50/50 SLEEPER/AC split with per-train coach layout |

## GitHub

[github.com/RedocamaI](https://github.com/RedocamaI)