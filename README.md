# spring-siyukio

**spring-siyukio** is a lightweight development framework built on top of **Spring Boot**, designed to accelerate the
creation of **API services** with enhanced security, performance, and developer experience.

## Core Highlights

### ⚡ Virtual Thread Native

Built for Java 21+ with full virtual thread support, enabling massive concurrency
without thread exhaustion.

### 🔒 Enterprise-Grade Security

Comprehensive security controls with JWT authentication (RSA/ECDSA), request signature validation, role-based access
control, and column-level AES-GCM encryption.

### 🎯 Zero-Configuration Schema Sync

Auto-detect and synchronize PostgreSQL tables, columns, indexes, and partitions on DAO initialization—no manual DDL
management.

### 🌐 Production-Ready Features

Built-in OpenAPI documentation, health monitoring, master-slave database support, WebSocket transport for cluster
deployments, and progress notifications.

## Requirements

- Java 21 or higher
- Maven 3.8+
- Spring Boot 3.x

## Modules

### [spring-siyukio-application](./spring-siyukio-application/README.md)

Spring Boot application foundation with powerful API development tools.

**Key Features:**

- **`@ApiController` & `@ApiMapping`** — Define REST APIs with built-in authorization, CORS, and OpenAPI
- **Flexible Security Controls** — JWT auth, request signatures, role-based access (independent or combined)
- **Unified Exception Handling** — Consistent error responses across services
- **Automatic OpenAPI Documentation** — Environment-toggled API documentation
- **Spring Boot Actuator** — Health checks, metrics, and runtime management

### [spring-siyukio-http-client](./spring-siyukio-http-client/README.md)

HTTP client for inter-service API communication.

**Key Features:**

- **Transparent Exception Handling** — `ApiException` propagation across services
- **Virtual Thread Support** — Efficient lightweight concurrency
- **Resource-Optimized** — JDK HttpClient reuse per protocol
- **Spring Integration** — Compatible with `@HttpExchange`, specialized + general-purpose

### [spring-siyukio-application-acp](./spring-siyukio-application-acp/README.md)

Agent Client Protocol (ACP) server implementation for building AI agent services.

**Key Features:**

- **Zero-Configuration ACP Server** — Auto-configure WebSocket ACP transport with Spring Boot auto-configuration
- **Bidirectional Communication** — Real-time interaction with ACP clients through `AcpSessionContext`
- **Session Management** — Built-in session lifecycle handling via `AcpSessionHandler`
- **Progress Notifications** — Send progress updates during long-running operations

### [spring-siyukio-acp-client](./spring-siyukio-acp-client/README.md)

Lightweight ACP client implementation for connecting to ACP agent services.

**Key Features:**

- **Simple API** — High-level, intuitive API for ACP operations with minimal boilerplate code
- **WebSocket Transport** — Built-in WebSocket client transport for real-time bidirectional communication
- **Tool Call Support** — Execute remote tool calls with automatic parameter serialization
- **Client-Side Capabilities** — Handle server requests for file operations and terminal commands

### [spring-siyukio-postgresql](./spring-siyukio-postgresql/README.md)

PostgreSQL entity management with automatic schema synchronization.

**Key Features:**

- **Zero-Configuration Schema Sync** — Auto DDL execution on DAO initialization
- **Column-Level AES-GCM Encryption** — Master key + per-record salt + entity keyInfo
- **Built-in Audit Timestamps** — Auto-manage createdAt/updatedAt fields
- **Complex Object Mapping** — Nested records, enums, lists, JSON objects
- **Automatic Partitioning** — YEAR/MONTH/DAY/HOUR strategies with monitoring
- **Master-Slave Architecture** — One master with multiple slaves
- **Spring-Native Integration** — Full `@Transactional` and bean injection support

## License

This project is licensed under the [MIT License](./LICENSE).