# Booking Service – XYZ Car Rental Company

The **Booking Service** is a Spring Boot microservice responsible for confirming car rental bookings and retrieving
booking details.
<br>It integrates with external systems (Driving License API, Pricing API), performs business validations, ensures
transactional inventory checks, and exposes secure HTTPS REST endpoints.

---

# 1. Features

- Confirm Car Rental Booking
- Retrieve Booking Details
- Validate license via external API
- Retrieve pricing via external API
- Async processing using CompletableFuture
- Resilience4j (CircuitBreaker, Retry, TimeLimiter, Fallbacks)
- JWT-based security (OAuth2 Resource Server)
- Jetty HTTPS
- Spring Profiles: mock, prod
- API Versioning
- Kubernetes-ready

---

# 2. Technology Stack

- Java 23
- Spring Boot 3.5.x
- Spring MVC + Jetty Server
- Spring Data JPA
- H2 database (local)
- Spring Security + OAuth2 (JWT)
- OpenFeign Clients
- Resilience4j
- CompletableFuture
- Actuator
- Docker
- Kubernetes

---

# 3. Assumptions

1. Driving License API returns `issueDate`, though real spec does not.
2. All dates must follow the format: `yyyy-MM-dd`.
3. Car segment list is fixed (SMALL, MEDIUM, LARGE, EXTRA_LARGE).
4. Timezone = server local; no conversion logic implemented.
5. H2 is used for development; production DB can be plugged in.
6. `mock` profile uses in-memory/stub Feign clients; `prod` uses real endpoints.

---

# 4. Architecture Diagram

```
[Client]
   |
   v
[Booking Controller]
   |
   v
[Booking Service] --------> [Validator]
        |                         |
        |                         +--> (Feign) Driving License API
        |                         |
        |                         +--> (Feign) Pricing API
        |
        v
[BookingTransactionService]
        |
        v
[Database: Booking Table + CarInventory Table]
```

---

# 5. Business Rules

## Reservation Rules

- Reservation cannot exceed 30 days
- Date format must be: yyyy-MM-dd

## License Validation

- Must be valid (not expired)
- Must be issued at least 1 year ago
- API assumed to return issueDate

## Pricing

- Retrieved from external Pricing API based on car segment.

---

# 6. Resilience & Fault Tolerance

Resilience4j decorators used around async Feign calls:

- Circuit Breaker
- Retry
- Timeout
- Fallback

Custom exceptions:

- InvalidBookingException
- BookingNotFoundException
- ExternalApiRequestException
- ExternalApiUnavailableException

Global exception handler returns structured JSON.

---

# 7. Security

- OAuth2 Resource Server (JWT validation with HS256)
- Role-based access via @PreAuthorize
- HTTPS enabled via Jetty (PKCS12 Keystore)

---

# 8. Spring Profiles

## mock Profile

- Connects to mock stub URLs and mock clients
- No real external dependencies

## prod Profile

- Connects to real Driving License API & Pricing API
- Includes Actuator probes

### Run with profiles:

Mock:

```
java -jar booking-service.jar --spring.profiles.active=mock
```

Prod:

```
java -jar booking-service.jar --spring.profiles.active=prod
```

---

# 9. Project Structure

```
.
├── README.md
├── pom.xml
├── Dockerfile
├── booking-deployment.yaml
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
└── src
    ├── main
    │   ├── java/com/xyz/booking
    │   │   ├── aspect
    │   │   ├── clients
    │   │   ├── config
    │   │   ├── constants
    │   │   ├── controller
    │   │   ├── dto
    │   │   ├── entity
    │   │   ├── enums
    │   │   ├── exception
    │   │   ├── properties
    │   │   ├── repository
    │   │   ├── security
    │   │   ├── service
    │   │   └── validation
    │   └── resources
    │       ├── application.yml
    │       ├── application-mock.yml
    │       ├── application-prod.yml
    │       ├── keystore/
    │       └── db
    │           ├── schema.sql
    │           └── data.sql
    └── test
```

---

# 10. API Examples

## Confirm Booking

POST /api/v1/booking/confirm

Request:

```
{
  "drivingLicenseNumber": "DL12345",
  "customerAge": 28,
  "reservationStartDate": "2025-01-10",
  "reservationEndDate": "2025-01-15",
  "carSegment": "MEDIUM"
}
```

Response:

```
{ "bookingId": 101 }
```

---

## Get Booking Details

GET /api/v1/booking/details/{id}

Response:

```
{
  "drivingLicenseNumber": "DL12345",
  "customerName": "John Doe",
  "age": 28,
  "reservationStartDate": "2025-01-10",
  "reservationEndDate": "2025-01-15",
  "carSegment": "MEDIUM",
  "rentalPrice": 7500.0
}
```

---

# 11. Deployment Files

## Dockerfile

Contained at repo root.

## booking-deployment.yaml

Kubernetes deployment:

- Deployment
- Service
- NodePort/ClusterIP
- Probes
- TLS

---

# 12. Summary

This is a production-ready microservice featuring async flows, resiliency, secure HTTPS communication, JWT-based
security, transactional inventory checks, and multiple execution profiles (mock/prod) and API versioning. Suitable for
enterprise deployment and cloud orchestration.

