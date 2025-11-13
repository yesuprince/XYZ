# Booking Service – XYZ Car Rental Company

This document provides a simple and clear overview of the **Booking Service**.  
The service enables customers to confirm car rental bookings and retrieve booking details.  
It integrates with external systems for license validation and pricing, follows important business rules, and uses a
straightforward and maintainable architecture.

---

# 1. Features

- Confirm a car rental booking
- Retrieve booking details
- Validate driving license through an external API
- Fetch pricing through an external API
- Perform validations using asynchronous processing (CompletableFuture)
- Apply basic resilience through Resilience4j
- Use JWT-based authentication
- Use HTTPS with Jetty on port **8443**
- Support Spring profiles (`mock`, `prod`)
- Run easily on Docker or Kubernetes

---

# 2. Technology Stack

- Java 23
- Spring Boot 3.5.x
- Spring MVC (Jetty)
- Spring Data JPA
- H2 (local development)
- Spring Security (OAuth2 JWT)
- OpenFeign for API calls
- Resilience4j
- CompletableFuture
- Actuator
- Docker
- Kubernetes (Deployment + NodePort Service)

---

# 3. Assumptions

To keep the implementation practical, the following assumptions are made:

1. The **Driving License API returns `issueDate`**, although the provided spec may not include it.
2. All date values are expected to follow the format: **yyyy-MM-dd**.
3. Car segments are predefined as: SMALL, MEDIUM, LARGE, EXTRA_LARGE.
4. The server timezone is used for all date calculations.
5. H2 is used for development only; any relational database can replace it in production.
6. The `mock` profile uses mock URLs; the `prod` profile uses actual external API URLs.

---

# 4. API Versioning

This service uses a simple and clear versioning approach based on URI paths.

### Current Version

All API endpoints are available under:

```
/api/v1
```

The version is defined in a constant:

```java
public static final String V1 = "/api/v1";
```

### Current Endpoints

- **POST** `/api/v1/booking/confirm`
- **GET** `/api/v1/booking/details/{bookingId}`

### Future Versions

If the API changes in the future, a new version such as `/api/v2` will be introduced without breaking existing
consumers.

---

# 5. Architecture Overview

```
[Client]
   |
   v
[Booking Controller]
   |
   v
[Booking Service] ------> [Validator]
        |                     |
        |                     +--> Driving License API (Feign)
        |                     +--> Pricing API (Feign)
        |
        v
[BookingTransactionService]
        |
        v
[Booking + CarInventory Tables]
```

---

# 6. Business Rules

### Reservation Rules

- Maximum reservation is **30 days**
- All dates follow the format **yyyy-MM-dd**

### License Rules

- License must be valid and not expired
- License must be at least **1 year old**
- The external API is assumed to return `issueDate`

### Pricing Rules

- Pricing is based on car segment and retrieved externally

### Inventory Rules

- Inventory row is locked using SQL `FOR UPDATE`
- Overlapping reservations are verified
- A booking is allowed only if cars are still available

---

# 7. Resilience

Resilience4j is used in a simple and effective way:

- Circuit Breaker
- Retry
- Timeout
- Fallback

Custom exceptions are grouped meaningfully, and a global exception handler ensures consistent error responses.

---

# 8. Security

- OAuth2 Resource Server with HS256 JWT
- Role-based access
- HTTPS enabled on Jetty (port 8443)

---

# 9. Spring Profiles

### mock

- Uses mock URLs & clients for external APIs
- Ideal for development without external dependencies

### prod

- Uses real external API endpoints
- Suitable for production or integration testing

### Running with profiles

Mock:
```
java -jar booking-service.jar --spring.profiles.active=mock
```

Prod:
```
java -jar booking-service.jar --spring.profiles.active=prod
```

---

# 10. Project Structure

```
.
├── README.md
├── pom.xml
├── Dockerfile
├── booking-deployment.yaml
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
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
    │       └── db/
    │           ├── schema.sql
    │           └── data.sql
    └── test
```

---

# 11. API Examples

### 11.1 Confirm Booking

**POST** `/api/v1/booking/confirm`

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

### 11.2 Get Booking Details

**GET** `/api/v1/booking/details/{bookingId}`

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

# 12. Kubernetes Deployment

A simple Kubernetes deployment file is included.  
It contains:

- One **Deployment**  (which can be scaled through replicas)
- One **NodePort Service**

### booking-deployment.yaml

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: booking-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: booking-service
  template:
    metadata:
      labels:
        app: booking-service
    spec:
      containers:
        - name: booking-service
          image: booking-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8443
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: mock
---
apiVersion: v1
kind: Service
metadata:
  name: booking-service
spec:
  selector:
    app: booking-service
  ports:
    - port: 8443
      targetPort: 8443
  type: NodePort
```

---

# 13. Note on JWT Token Utility Classes

To support local development and manual testing, the project includes two classes:

- **JwtUtil**
- **JwtTokenGenerator**

These classes are intended only for local usage to help generate simple JWT tokens.

In production environments:

- These utilities should **not** be used
- JWTs must be issued by an **enterprise-grade Identity Provider**

---

# 14. Summary

This service aims to be simple, clear, and dependable.  
It follows good development practices, keeps the design understandable, and supports future improvements through
versioning and modular structure.

