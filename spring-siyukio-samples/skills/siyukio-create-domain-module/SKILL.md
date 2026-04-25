---
name: siyukio-create-domain-module
description: "Generate a complete domain module for Siyukio-based Spring Boot applications, including API, Application, and Domain layers"
triggers:
  - "add domain module"
  - "create domain module"
  - "new domain module"
  - "domain module add"
  - "domain module create"
  - "add module"
  - "create module"
---

<Purpose>
Generate a complete domain module for Siyukio Spring Boot applications. This skill creates the full module structure including API layer, Application layer, and Domain layer, and updates all necessary build configurations.
</Purpose>

<Use_When>

- Creating a new domain module in the project
- Adding a new business domain to the application
- Setting up a new bounded context

</Use_When>

<Prerequisites>
- Target project must exist: `{project-name}/`
- Must have access to parent pom.xml for module registration
- Must have access to bootstrap pom.xml for profile configuration

</Prerequisites>

## Domain Module Structure

```
{project-name}/{project-name}-domain-{domain}/
├── pom.xml
├── src/main/java/{package-path}/{domain}/
│   ├── api/                    # API layer
│   │   ├── {Context}Controller.java
│   │   ├── paths/
│   │   │   └── {Context}Paths.java
│   │   └── dto/
│   │       ├── {Context}Request.java
│   │       └── {Context}Response.java
│   ├── application/            # Application layer
│   │   └── {Context}Service.java
│   └── domain/                 # Domain layer
│       ├── model/
│       │   └── {Entity}.java
│       ├── policy/
│       │   └── {Entity}Policy.java
│       └── errors/
│           └── {Entity}Errors.java
└── src/main/resources/
    └── {domain}/               # Domain-specific configs
```

<Execution_Protocol>

## Step 1: Determine Module Requirements

From the argument, extract:

- `{project-name}`: Project name (e.g., `myapp`)
- `{package-name}`: Base Java package (e.g., `com.example.myapp`)
- `{package-path}`: Package path (e.g., `/com/example/myapp`)
- `{Domain}`: Domain module name (kebab-case, e.g., `user-management`)
- `{domain}`: Domain variable (same as `{Domain}` but for use in paths)
- `{Entity}`: Main entity name (PascalCase, e.g., `User`)
- `{Context}`: Business context name (PascalCase, e.g., `User`)

## Step 2: Create Domain Module Directory Structure

Create the module directory and pom.xml:

```
{project-name}/{project-name}-domain-{domain}/pom.xml
{project-name}/{project-name}-domain-{domain}/src/main/java/{package-path}/{domain}/
{project-name}/{project-name}-domain-{domain}/src/main/resources/{domain}/
```

## Step 3: Generate Domain Module pom.xml

Location: `{project-name}/{project-name}-domain-{domain}/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>{package-name}</groupId>
        <artifactId>{project-name}</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>{project-name}-domain-{domain}</artifactId>
    <packaging>jar</packaging>
    <name>{Project Name} Domain {Domain}</name>

    <dependencies>
        <dependency>
            <groupId>io.github.siyukio</groupId>
            <artifactId>spring-siyukio-application</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.siyukio</groupId>
            <artifactId>spring-siyukio-postgresql</artifactId>
        </dependency>
    </dependencies>

</project>
```

## Step 4: Update Parent pom.xml

### 4.1 Add to `<modules>`

Location: `{project-name}/pom.xml`

Add the new module to the `<modules>` section:

```xml

<modules>
    <module>{project-name}-domain-{domain}</module>
    <!-- existing modules -->
</modules>
```

### 4.2 Add to `<dependencyManagement>`

Location: `{project-name}/pom.xml`

Add version management for the new module:

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>{package-name}</groupId>
            <artifactId>{project-name}-domain-{domain}</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!-- existing managed dependencies -->
    </dependencies>
</dependencyManagement>
```

## Step 5: Update Bootstrap pom.xml

### 5.1 Add Domain-Specific Profile

Location: `{project-name}/{project-name}-bootstrap/pom.xml`

Add a profile for the new domain:

```xml

<profiles>
    <profile>
        <id>{domain}</id>
        <properties>
            <deployment-profile>{domain}</deployment-profile>
        </properties>
        <dependencies>
            <dependency>
                <groupId>{package-name}</groupId>
                <artifactId>{project-name}-domain-{domain}</artifactId>
            </dependency>
        </dependencies>
    </profile>
    <!-- existing profiles -->
</profiles>
```

### 5.2 Update "full" Profile

Find the profile with `id="full"`. If it doesn't exist, create it. If it exists, add the new domain dependency:

```xml

<profile>
    <id>full</id>
    <properties>
        <deployment-profile>full</deployment-profile>
    </properties>
    <dependencies>
        <dependency>
            <groupId>{package-name}</groupId>
            <artifactId>{project-name}-domain-{domain}</artifactId>
        </dependency>
        <!-- existing full dependencies -->
    </dependencies>
</profile>
```

## Step 6: Generate Domain Layer

Use the `$siyukio-create-domain` skill to generate:

- Entity: `{Entity}.java`
- Policy: `{Entity}Policy.java` (optional)
- Errors: `{Entity}Errors.java` (optional)

## Step 7: Generate Application Layer

Use the `$siyukio-create-application` skill to generate:

- Service: `{Context}Service.java`

## Step 8: Generate API Layer

Use the `$siyukio-create-api` skill to generate:

- Controller: `{Context}Controller.java`
- Paths: `{Context}Paths.java`
- DTOs: `{Context}Request.java`, `{Context}Response.java`

</Execution_Protocol>

<Key_Conventions>

| Item             | Convention                                       |
|------------------|--------------------------------------------------|
| Module Name      | `{project-name}-domain-{domain}`                 |
| Base Package     | `{package-name}.{domain}`                        |
| Module Directory | `{project-name}/{project-name}-domain-{domain}/` |
| Package Path     | `{package-path}/{domain}/`                       |

</Key_Conventions>

<Layer_Reference>

### Domain Layer ($siyukio-create-domain)

Creates domain model (Entity), validation (Policy), and error definitions.

### Application Layer ($siyukio-create-application)

Creates application service that orchestrates domain operations.

### API Layer ($siyukio-create-api)

Creates REST endpoints with Controller, Paths, and DTOs.

</Layer_Reference>

<Dependency_Flow>

```
API Layer (siyukio-create-api)
    │
    ├── Depends on: Application Layer
    │
    ▼
Application Layer (siyukio-create-application)
    │
    ├── Depends on: Domain Layer, DTOs from API Layer
    │
    ▼
Domain Layer (siyukio-create-domain)
    │
    └── Depends on: spring-siyukio-postgresql
```

</Dependency_Flow>

<Verification>
After implementation:
1. Run `./mvnw compile` to verify all layers compile
2. Verify parent pom.xml has the new module registered
3. Verify bootstrap pom.xml has the profile configured
4. Run tests to ensure integration works
</Verification>
