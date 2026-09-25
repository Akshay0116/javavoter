# Multi-stage build for fast deployment and minimal container size
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy source code and web assets
COPY src ./src
COPY web ./web

# Compile all classes
RUN mkdir -p bin && javac -d bin src/com/voting/*.java src/com/voting/exception/*.java src/com/voting/web/*.java src/com/voting/test/*.java

# Production runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy compiled classes and frontend files
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/web ./web

# Default port (overridden automatically by Render/Railway via $PORT)
ENV PORT=8080
EXPOSE 8080

# Launch the Online Voting System
CMD ["java", "-cp", "bin", "com.voting.Main"]
