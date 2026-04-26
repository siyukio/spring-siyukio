---
name: siyukio-init-springboot
description: "Initialize a new Siyukio-based Spring Boot Maven project from scratch with parent pom, common module, and bootstrap module"
triggers:
  - "init project"
  - "initialize project"
  - "new project"
  - "create project"
  - "start project"
  - "setup project"
---

<Purpose>
Initialize a new Siyukio-based Spring Boot Maven project. This skill creates the project structure with parent pom, common module, and bootstrap module from an empty git directory.
</Purpose>

<Use_When>

- Starting a new Siyukio project from scratch
- Initializing a Spring Boot project with Siyukio framework
- Setting up a new multi-module Maven project

</Use_When>

<Prerequisites>
- Target directory should be an empty git repository (or empty directory)
- Maven 3.8+ installed
- Java 21+ available

</Prerequisites>

<Execution_Protocol>

## Step 1: Determine Project Parameters

From the argument, extract:

- `{project-name}`: Project artifact ID (kebab-case, e.g., `myapp`)
- `{package-name}`: Base Java package (e.g., `com.example.myapp`)
- `{project-version}`: Initial version (e.g., `1.0.0`)

## Step 2: Create Parent pom.xml

Location: `./{project-name}/pom.xml`

If `{project-name}/pom.xml` does not exist, create it:

```xml

<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.github.siyukio</groupId>
        <artifactId>spring-siyukio</artifactId>
        <version>3.5.13</version>
    </parent>
    <groupId>{package-name}</groupId>
    <artifactId>{project-name}</artifactId>
    <version>{project-version}-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>{Project Name}</name>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <skipTests>true</skipTests>
    </properties>

    <modules>
    </modules>

    <dependencyManagement>
        <dependencies>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <skipTests>${skipTests}</skipTests>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

## Step 3: Create Common Module

### 3.1 Create Module Directory

```
./{project-name}/{project-name}-common/
```

### 3.2 Create Common pom.xml

Location: `./{project-name}/{project-name}-common/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>{package-name}</groupId>
        <artifactId>{project-name}</artifactId>
        <version>{project-version}-SNAPSHOT</version>
    </parent>

    <artifactId>{project-name}-common</artifactId>
    <name>{Project Name} Common</name>

</project>
```

## Step 4: Update Parent pom.xml with Common Module

### 4.1 Add to `<modules>`

Update `./{project-name}/pom.xml`:

```xml

<modules>
    <module>{project-name}-common</module>
</modules>
```

### 4.2 Add to `<dependencyManagement>`

Update `./{project-name}/pom.xml`:

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>{package-name}</groupId>
            <artifactId>{project-name}-common</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Step 5: Create Bootstrap Module

### 5.1 Create Module Directory

```
./{project-name}/{project-name}-bootstrap/
```

### 5.2 Create Bootstrap pom.xml

Location: `./{project-name}/{project-name}-bootstrap/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>{package-name}</groupId>
        <artifactId>{project-name}</artifactId>
        <version>{project-version}-SNAPSHOT</version>
    </parent>

    <artifactId>{project-name}-bootstrap</artifactId>
    <name>{Project Name} Bootstrap</name>

    <properties>
        <deployment-profile>full</deployment-profile>
    </properties>

    <dependencies>
        <dependency>
            <groupId>{package-name}</groupId>
            <artifactId>{project-name}-common</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.siyukio</groupId>
            <artifactId>spring-siyukio-application-acp</artifactId>
        </dependency>
    </dependencies>

    <profiles>
        <profile>
            <id>full</id>
            <properties>
                <deployment-profile>full</deployment-profile>
            </properties>
            <dependencies>
            </dependencies>
        </profile>
    </profiles>

    <build>
        <finalName>{project-name}-${deployment-profile}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 5.3 Create Main Class

Location: `./{project-name}/{project-name}-bootstrap/src/main/java/{package-name}/{project-name}Main.java`

```java
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class {project-name}Main {

    public static void main(String[] args) {
        new SpringApplicationBuilder({project-name}Main.class)
                .build()
                .run(args);
    }
}
```

### 5.4 Create Application Configuration

Location: `./{project-name}/{project-name}-bootstrap/src/main/resources/application.yml`

```yaml
# Health check
management:
  endpoints:
    web:
      exposure:
        include: health

# Siyukio configuration
spring:
  siyukio:
    jwt:
      public-key: ${SIYUKIO_JWT_PUBLIC_KEY:}
      private-key: ${SIYUKIO_JWT_PRIVATE_KEY:}
      password: ${SIYUKIO_JWT_PASSWORD:}
    signature:
      salt: ${SIYUKIO_SIGNATURE_SALT:siyukio}
    profiles:
      docs: ${SIYUKIO_PROFILES_DOCS:true}
      active: ${SIYUKIO_PROFILES_ACTIVE:dev}
  datasource:
    postgres:
      master-key: ${SIYUKIO_DB_MASTER_KEY:}
      hikari:
        maximum-pool-size: 18
        minimum-idle: 18
        idle-timeout: 600000
        max-lifetime: 1800000
      master:
        url: ${SIYUKIO_DB_MASTER_URL:jdbc:postgresql://localhost:5432/root}
        username: ${SIYUKIO_DB_MASTER_USERNAME:}
        password: ${SIYUKIO_DB_MASTER_PASSWORD:}

server:
  port: ${SERVER_PORT:8080}
```

## Step 6: Add Bootstrap Module to Parent pom.xml

### 6.1 Add to `<modules>`

Update `./{project-name}/pom.xml`:

```xml

<modules>
    <module>{project-name}-common</module>
    <module>{project-name}-bootstrap</module>
</modules>
```

### 6.2 Add to `<dependencyManagement>`

Update `./{project-name}/pom.xml`:

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>{package-name}</groupId>
            <artifactId>{project-name}-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>{package-name}</groupId>
            <artifactId>{project-name}-bootstrap</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Step 7: Initialize Maven Wrapper

Execute the following command to generate Maven wrapper:

```bash
cd ./{project-name} && mvn -N wrapper:wrapper
```

This creates `./{project-name}/mvnw` and `./{project-name}/mvnw.cmd` for consistent builds across environments.

## Step 8: Create .gitignore

Create a `.gitignore` file at the project root to exclude unnecessary files from version control.

Location: `./{project-name}/.gitignore`

```
# Build output
**/target/

# Local profile configurations (secrets for unit tests)
**/*-local.*

# OMX
.omx/

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo
*~

# OS
.DS_Store
Thumbs.db

# Maven
.mvn/wrapper/maven-wrapper.jar
.settings/
.project
.classpath
```

This ensures that build artifacts, IDE configurations, system files, and local configuration files with secrets are not
committed to the repository.

</Execution_Protocol>

<Project_Structure>

After execution, the project structure will be:

```
{project-name}/
├── pom.xml                                    # Parent pom
├── mvnw                                       # Maven wrapper (Unix)
├── mvnw.cmd                                   # Maven wrapper (Windows)
├── {project-name}-common/
│   └── pom.xml                                # Common module
└── {project-name}-bootstrap/
    ├── pom.xml                                # Bootstrap module
    └── src/main/
        ├── java/{package-path}/
        │   └── {project-name}Main.java       # Main application class
        └── resources/
            └── application.yml                # Application configuration
```

</Project_Structure>

<Key_Conventions>

| Item             | Value                      |
|------------------|----------------------------|
| Parent Artifact  | `{project-name}`           |
| Package          | `{package-name}`           |
| Package Path     | `{package-path}`           |
| Common Module    | `{project-name}-common`    |
| Bootstrap Module | `{project-name}-bootstrap` |
| Siyukio Version  | `3.5.13`                   |
| Java Version     | 21                         |

</Key_Conventions>

<Common_Module>

The `common` module is intended for shared code across the project:

- Utility classes
- Common constants
- Shared configurations
- Base classes/interfaces

</Common_Module>

<Bootstrap_Module>

The `bootstrap` module is the application entry point:

- Spring Boot main class
- Application configurations
- ACP server (via `spring-siyukio-application-acp`)
- Domain modules are added as profile dependencies

</Bootstrap_Module>

<Verification>
After implementation:
1. Run `./mvnw compile` to verify the project compiles
2. Run `./mvnw spring-boot:run` to start the application
3. Verify ACP server starts on the configured port
</Verification>
