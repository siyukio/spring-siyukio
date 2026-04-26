<!-- This file is Project-Level AGENTS.md -->
<!-- This file SUPPLEMENTS the global ~/.codex/AGENTS.md, it does NOT replace it. -->
<!-- Managed manually - omx setup preserves this file when --force is not used. -->

# Spring Siyukio Samples

## Project Overview

Spring Siyukio Samples is a collection of sample projects demonstrating the usage of the Siyukio framework. It provides
working examples to help developers understand how to use various features of the framework.

## Project Structure

```
spring-siyukio-samples/
```

## Context Parameters

```yaml
project-name: spring-siyukio-samples
project-version: 3.5.13-SNAPSHOT
package-name: io.github.siyukio.samples
package-path: /io/github/siyukio/samples
java-version: 21
maven-version: 3.9
```

## Available Skills

| Skill                           | Purpose                                                                                |
|---------------------------------|----------------------------------------------------------------------------------------|
| `$siyukio-init-springboot`      | Initialize Spring Boot project structure                                               |
| `$siyukio-create-domain`        | Create domain models and policy logic                                                  |
| `$siyukio-create-api`           | Create domain API                                                                      |
| `$siyukio-create-application`   | Create application layer (accept API requests, call domain policy, operate model data) |
| `$siyukio-create-acp`           | Initialize ACP server session handler                                                  |
| `$siyukio-create-domain-module` | Create complete domain feature with module dependencies                                |

## Development Guidelines

### Adding a New Sample

1. Create a new module under `spring-siyukio-samples`
2. Name convention: `spring-siyukio-sample-domain-{domain}`
3. Update parent `pom.xml` to include the new module
4. Implement the sample following the framework's best practices

### Local Environment Configuration

**Required Environment Variables for Local Test:

| Variable                   | Purpose           | Test Value                            |
|----------------------------|-------------------|---------------------------------------|
| SIYUKIO_DB_MASTER_URL      | Database url      | jdbc:postgresql://localhost:5432/root |
| SIYUKIO_DB_MASTER_USERNAME | Database username | root                                  |
| SIYUKIO_DB_MASTER_PASSWORD | Database password | FYm7JqaEcptxUTgy                      |

## Language Policy

- All generated code, comments must be in English only.

## Commit Convention

Format: `<type>(<scope>): <description>`

### Types

`feat`, `fix`, `refactor`, `docs`, `style`, `test`, `chore`, `ci`, `build`, `perf`, `revert`

### Scopes

`sample`, `common`, `config`, `dependency`

## Notes

- This module is skipped during `mvn deploy` (configured via `maven.deploy.skip=true`)
- Samples are for demonstration purposes only and should not be deployed to production
