# spring-siyukio

**spring-siyukio** is a lightweight development framework built on top of **Spring Boot**, designed to accelerate the
creation of **MCP services** with enhanced security, performance, and developer experience.

## Core Highlights

### ⚡ Virtual Thread Native

Built for Java 21+ with full virtual thread support across MCP client and execution layers, enabling massive concurrency
without thread exhaustion.

### 🔒 Enterprise-Grade Security

Comprehensive security controls with JWT authentication (RSA/ECDSA), request signature validation, role-based access
control, and column-level AES-GCM encryption.

### 🚀 Dual Protocol Architecture

Seamlessly expose the same API as both HTTP and MCP protocols with minimal configuration changes—perfect for unified
service ecosystems.

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
- **Unified Exception Handling** — MCP protocol-aligned error responses
- **Automatic OpenAPI Documentation** — Environment-toggled API documentation
- **Spring Boot Actuator** — Health checks, metrics, and runtime management

### [spring-siyukio-application-mcp](./spring-siyukio-application-mcp/README.md)

MCP service application enabling dual protocol support.

**Key Features:**

- **Dual Protocol Support** — Same API supports HTTP and MCP simultaneously
- **Automatic Schema Conversion** — OpenAPI to MCP schema with zero code duplication
- **Enhanced MCP Interactions** — ProgressNotification and AI Sampling support
- **WebSocketStreamable Transport** — Direct Kubernetes deployment without session sharing services
- **Virtual Thread Execution** — Auto-switch for efficient long-running tasks
- **Zero-Config JWT** — Seamless authentication integration

### [spring-siyukio-mcp-client](./spring-siyukio-mcp-client/README.md)

MCP client for accessing MCP streamable protocol services.

**Key Features:**

- **Virtual Thread Native** — Handle thousands of long-running blocking requests
- **WebSocket Transport** — Better performance and clustering support
- **Flexible Authentication** — Static tokens or dynamic token suppliers
- **Progress Notifications** — Built-in progress update handling
- **AI Sampling Handler** — Customizable AI model interaction
- **Spring Integration** — Fluent builder API with property configuration

### [spring-siyukio-http-client](./spring-siyukio-http-client/README.md)

HTTP client for inter-service API communication.

**Key Features:**

- **Transparent Exception Handling** — `ApiException` propagation across services
- **Virtual Thread Support** — Efficient lightweight concurrency
- **Resource-Optimized** — JDK HttpClient reuse per protocol
- **Spring Integration** — Compatible with `@HttpExchange`, specialized + general-purpose

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