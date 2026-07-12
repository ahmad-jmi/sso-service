# CLAUDE.md

## Project Overview

-   Project name: `sso-service`
-   Language: Java 17
-   Build tool: Maven
-   IDE: IntelliJ IDEA Community Edition
-   Development machine: Intel MacBook running macOS

## Base Package

``` text
com.ahmad.sso.service
```

## Technology Stack

-   Spring Boot 4.x
-   Spring Security
-   JWT Authentication
-   Spring Data JPA
-   PostgreSQL
-   Redis (Lettuce client)
-   Kafka
-   Spring Actuator
-   OpenAPI / Swagger
-   MapStruct
-   Lombok
-   Micrometer Metrics
-   OpenTelemetry dependencies present but OTLP export disabled for
    local development

## Database

-   PostgreSQL database name: `ssoservice`
-   JDBC URL:

``` text
jdbc:postgresql://localhost:5432/ssoservice
```

## Observability

Local development configuration:

``` properties
management.otlp.metrics.export.enabled=false
```

Reason: - No OpenTelemetry collector is running locally. - This avoids
warnings about:

``` text
Failed to publish metrics to OTLP receiver
http://localhost:4318/v1/metrics
```

## Current Authentication Design

-   JWT based authentication
-   `JwtAuthenticationFilter`
-   `JwtService`
-   Spring Security filter chain

## Development Environment

-   IntelliJ IDEA
-   Maven project
-   Java 17
-   PostgreSQL running locally

## Useful IntelliJ Shortcuts (macOS)

### Reformat code

Option + Command + L

### Optimize imports

Control + Option + O

### Replace in entire project

Command + Shift + R

### Rename symbol/package safely

Shift + F6

## Notes For Future AI Sessions

-   The user is actively developing this project.
-   Prefer Spring Boot best practices.
-   Assume package root is:

``` text
com.ahmad.sso.service
```

-   Assume Maven and IntelliJ on Intel macOS unless stated otherwise.
