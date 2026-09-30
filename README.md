# BotAI

BotAI is a Spring Boot application for managing student records with a PostgreSQL + pgvector backend and a Google Gemini-powered AI assistant.

It exposes REST endpoints for CRUD operations and lets you ask natural-language questions about student data. The app can also detect create/update/delete intent from a question and require confirmation before executing the change.

## Features

- Student management API with create, read, update, and delete operations
- PostgreSQL persistence with JPA
- pgvector-based semantic search for student records
- Google GenAI integration for answering questions from stored student data
- Confirmation workflow for AI-triggered student actions
- Simple front-end dashboard served from the app

## Tech stack

- Java 21
- Spring Boot 4.1.1
- Maven wrapper
- PostgreSQL 17 with pgvector
- Spring AI
- Google Gemini models

## Project structure

- `src/main/java/com/example/BotAI` - application source code
- `src/main/resources/application.properties` - runtime configuration
- `src/main/resources/static/index.html` - browser-based API tester
- `docker-compose.yml` - PostgreSQL + pgvector service
- `pom.xml` - Maven dependencies and build config

## Prerequisites

Before running the project, make sure you have:

- Java 21 or newer
- Docker Desktop or Docker Engine
- Git
- A Google AI API key for Gemini

## Quick start

1. Start PostgreSQL + pgvector:

   ```bash
   docker compose up -d
   ```

2. Update the Google AI credentials in `src/main/resources/application.properties`:

   ```properties
   spring.ai.google.genai.api-key=${GOOGLE_AI_API_KEY}
   spring.ai.google.genai.embedding.api-key=${GOOGLE_AI_API_KEY}
   ```

   Replace the sample values with your own keys. The app expects a valid Google AI key to answer questions and generate embeddings.

3. Start the app:

   On macOS/Linux:

   ```bash
   ./mvnw spring-boot:run
   ```

   On Windows:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Open the app in a browser:

   ```text
   http://localhost:8081
   ```

5. The API is available at:

   ```text
   http://localhost:8081/api
   ```

## Configuration

The main runtime settings are in `src/main/resources/application.properties`.

Important values:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/spring_ai
spring.datasource.username=postgres
spring.datasource.password=postgres
server.port=8081
```

If your local database credentials differ, update them before starting the app.

## API endpoints

### Get all students

```bash
curl http://localhost:8081/api/students
```

### Create a student

```bash
curl -X POST http://localhost:8081/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Johnson",
    "school": "Springfield High",
    "email": "alice@example.com",
    "phone": "+1234567890",
    "description": "Computer science student"
  }'
```

### Update a student

```bash
curl -X PUT http://localhost:8081/api/students/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Johnson",
    "school": "Northfield Academy",
    "email": "alice.new@example.com",
    "phone": "+1234567891",
    "description": "Updated student profile"
  }'
```

### Delete a student

```bash
curl -X DELETE http://localhost:8081/api/students/1
```

### Ask the AI about student data

```bash
curl "http://localhost:8081/api/askai?question=Who%20is%20Alice%20Johnson%3F"
```

### AI action confirmation flow

The app can also interpret instructions such as:

```text
Create student with name=Bob Smith, school=Harvard, email=bob@example.com, phone=1111111, description=Marketing major
```

Then the app returns a confirmation response. If confirmed, it executes the create/update/delete action.

## Example AI prompts

- `Which students attend Springfield High?`
- `Show me the student with email alice@example.com`
- `Create student with name=Bob Smith, school=Harvard, email=bob@example.com, phone=1111111, description=Marketing major`
- `Update student id=2 with name=Alice, school=Riverside, email=alice@school.com`
- `Delete student id=3`

## Running tests

```bash
./mvnw test
```

## Helpful notes

- The database container is configured in `docker-compose.yml` and exposes PostgreSQL on port `5433`.
- The application listens on port `8081`.
- The app includes a front-end page at `/` for quick testing without writing curl commands.
- If you want to change the default database or port, update `src/main/resources/application.properties` before startup.

