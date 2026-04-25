# Spring Siyukio Samples - Unattended Development

This project demonstrates how to combine **Codex** and **oh-my-codex** to achieve **unattended development** (automated
development workflow) using the Siyukio framework.

## Overview

Spring Siyukio Samples is a sample project that showcases:

- How to use Codex skills for automated code generation
- How to configure oh-my-codex for unattended development
- Best practices for Siyukio framework usage

This enables developers to automate repetitive coding tasks and focus on high-level design and architecture.

## Integrating with Codex

To enable Codex to understand and use Siyukio framework, copy the skills folder to Codex:

```bash
# Copy all skills to Codex
cp -r ../skills ~/.codex/skills
```

After copying, Codex will be able to:

- Understand Siyukio framework structure
- Use skills to generate code following Siyukio conventions
- Create domain models, APIs, and application layers correctly

## Available Skills

The following skills are available for Siyukio development:

| Skill                           | Purpose                                  | Usage                           |
|---------------------------------|------------------------------------------|---------------------------------|
| `$siyukio-init-springboot`      | Initialize Spring Boot project structure | `$siyukio-init-springboot`      |
| `$siyukio-create-domain`        | Create domain models and policy logic    | `$siyukio-create-domain`        |
| `$siyukio-create-api`           | Create domain API                        | `$siyukio-create-api`           |
| `$siyukio-create-application`   | Create application layer                 | `$siyukio-create-application`   |
| `$siyukio-create-acp`           | Initialize ACP server session handler    | `$siyukio-create-acp`           |
| `$siyukio-create-domain-module` | Create complete domain feature           | `$siyukio-create-domain-module` |

## Using Skills in Codex

### Prerequisites

1. Ensure Maven Wrapper (`mvnw`) exists in the project
2. If `mvnw` doesn't exist, use `$siyukio-init-springboot` to initialize the project

### Skill Workflow Example

To create a complete sample feature:

1. **Initialize project** (if needed):
   ```
   $siyukio-init-springboot
   ```

2. **Create domain model**:
   ```
   $siyukio-create-domain
   ```
   This creates domain models and policy logic.

3. **Create API**:
   ```
   $siyukio-create-api
   ```
   This creates the domain API layer.

4. **Create application layer**:
   ```
   $siyukio-create-application
   ```
   This creates the application layer that accepts API requests.

5. **Create ACP handler** (if needed):
   ```
   $siyukio-create-acp
   ```
   This initializes ACP server session handler for AI agent services.

6. **Create complete domain module**:
   ```
   $siyukio-create-domain-module
   ```
   This creates a complete domain feature with all dependencies.

### Building and Testing

After using skills to generate code, build and test with Maven Wrapper:

```bash
# Build all samples
./mvnw clean install -DskipTests

# Run tests
./mvnw test

# Build specific sample
cd spring-siyukio-sample-{domain}
../../mvnw clean install -DskipTests
```

## Getting Started

### Step 1: Install Codex and oh-my-codex

```bash
# Install Codex and oh-my-codex globally
npm install -g @openai/codex oh-my-codex

# Setup oh-my-codex
omx setup && omx doctor

# Enable high-performance mode
omx --madmax --high
```

### Step 2: Copy Skills to Codex

```bash
# Copy all skills to Codex
cp -r ./skills ~/.codex/skills
```

### Step 3: Create Project Configuration

Navigate to your target project directory and create `AGENTS.md`. This file should contain:

**1. Project Information** (required):

```yaml
project-name: your-project-name
project-version: 1.0.0-SNAPSHOT
package-name: com.example.yourproject
package-path: /com/example/yourproject
java-version: 21
maven-version: 3.9
```

**2. Development Workflow** (required):
Define the development workflow for unattended development. You can customize the workflow based on your project needs.

Example `AGENTS.md` structure:

```markdown
# Your Project Name

## Project Information

[same as above]

## Development Workflow

Every development task must follow this sequence:

1. **Create a feature branch** from `test/{project-version}` with appropriate prefix (e.g., `feat/`, `fix/`,
   `refactor/`)
2. **Implement changes** following the applicable sub-project skill
3. **Verify** using the sub-project's verification gates
4. **Commit** with Lore-compliant message format: `<type>(<scope>): <intent>`
5. **Push branch and create PR/MR** to `test/{project-version}`
6. **Cleanup**: Switch back to `test/{project-version}` and delete the submitted local branch
```

**Notes:**

- For projects that don't need MR, you can simplify the workflow (e.g., commit directly to main)
- The workflow should match your team's development practices
- oh-my-codex will follow this workflow when executing tasks
- **PR/MR Integration**: If your workflow requires creating Pull Requests (PR) or Merge Requests (MR), you can ask Codex to create a custom skill for it. For example:
  ```
  $skill-creator
  ```
  Then describe the PR/MR creation process for your Git platform (GitHub, GitLab, etc.). Once created, you can add it to your `AGENTS.md` workflow steps.

Create the file:

```bash
cd /path/to/your/project
# Create AGENTS.md with the above information
```

**Important:**

- Do NOT modify `~/.codex/AGENTS.md` as it is managed by `omx`.
- The project-level `AGENTS.md` supplements the global one, it does NOT replace it.

### Step 4: Initialize Project

```bash
# Use oh-my-codex to initialize the project
omx exec "initialize project"
```

This will trigger the `$siyukio-init-springboot` skill to set up the project structure.

### Step 5: Execute Development Tasks

Now you can use `omx exec` to execute development tasks. oh-my-codex will use the skills to automatically generate code:

```bash
# Execute a development task
omx exec "your task description"

# Examples:
omx exec "create user domain with CRUD operations"
omx exec "add JWT authentication to the project"
omx exec "create order management module"
```

**How it works:**

1. `omx` reads the project's `AGENTS.md` to understand the project context
2. It selects the appropriate skill based on the task description
3. The skill generates code following Siyukio framework conventions
4. The generated code is automatically compiled and tested

**Tips:**

- Be specific in your task description
- You can reference domain names, field names, or specific features
- `omx` will handle the entire workflow from code generation to testing

## Notes

- Skills are designed to work with the Siyukio framework conventions
- Always use `mvnw` instead of system `mvn` for consistent builds
