# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /build

# Copy dependency files (pom.xml first for better caching)
COPY pom.xml .

# Download dependencies with BuildKit cache mount
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# Copy mvnw wrapper and .mvn
COPY mvnw .
COPY .mvn/ .mvn/

# Make mvnw executable
RUN chmod +x mvnw

# Copy source code
COPY src ./src

# Build application with BuildKit cache mount
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /build/target/siyukio-bootstrap.jar app.jar

# Expose application port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:80/actuator/health || exit 1

# JVM options
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=80 -jar app.jar"]
