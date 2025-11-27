# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DoorayMCP is an MCP (Model Context Protocol) server implementation for NHN Dooray integration, written in Kotlin. It provides 48 tools across 5 categories (Wiki, Project, Messenger, Calendar, Drive) that enable Claude to interact with Dooray services via standardized MCP protocol over stdin/stdout.

@README for complete feature documentation and API examples.

## Tech Stack

- **Language**: Kotlin 2.1.20 with JVM toolchain 21
- **Build**: Gradle 8.10+ with Shadow plugin for fat JARs
- **Framework**: MCP Kotlin SDK 0.7.7
- **HTTP Client**: Ktor 3.1.1 (with content negotiation, JSON serialization, logging)
- **Logging**: Logback 1.5.18 (stderr-only to avoid stdout pollution)
- **Testing**: JUnit Platform, kotlinx-coroutines-test, ktor-client-mock, mockk
- **Deployment**: Docker with multi-stage builds (AMD64 only; ARM64 temporarily disabled)

## Build & Run Commands

### Development

```bash
# Build fat JAR with all dependencies
./gradlew clean shadowJar

# Run locally with .env file (loads DOORAY_API_KEY, DOORAY_BASE_URL)
./gradlew runLocal

# Run tests (excludes *IntegrationTest* in CI environments)
./gradlew test

# Run in CI (auto-excludes integration tests)
CI=true ./gradlew test

# Validate tool JSON schemas
./gradlew validateSchemas
```

### Docker

```bash
# Build Docker image (specify version via --build-arg)
docker build -t dooray-mcp:local --build-arg VERSION=0.2.1 .

# Run Docker container with environment variables
docker run -e DOORAY_API_KEY="your_key" \
           -e DOORAY_BASE_URL="https://api.dooray.com" \
           dooray-mcp:local

# Pull and run published image
docker pull my13each/dooray-mcp:latest
docker run -e DOORAY_API_KEY="your_key" \
           -e DOORAY_BASE_URL="https://api.dooray.com" \
           my13each/dooray-mcp:latest
```

## Architecture

### MCP Server Flow

```
Main.kt
  ├─ configureSystemLogging()  # Redirect all logs to stderr (MCP uses stdout)
  └─ DoorayMcpServer.initServer()
       ├─ getEnv() - Validates DOORAY_API_KEY, DOORAY_BASE_URL
       ├─ DoorayHttpClient - Ktor-based HTTP client with retry logic
       ├─ Server (MCP SDK) - Handles protocol communication
       ├─ registerTool() - Conditionally registers 48 tools based on DOORAY_ENABLED_CATEGORIES
       └─ StdioServerTransport - stdin/stdout transport (blocking runBlocking)
```

### Tool Registration System

**Category-based filtering** (`ToolCategory.kt`):
- Tools are grouped into 5 categories: WIKI, PROJECT, MESSENGER, CALENDAR, DRIVE
- Environment variable `DOORAY_ENABLED_CATEGORIES` controls which tools are registered
- Format: comma-separated list (e.g., `"wiki,project"`)
- If unset/empty: all 48 tools are registered

**Tool implementation pattern**:
1. Each tool has two components in `src/main/kotlin/com/my13each/dooray/mcp/tools/`:
   - `*Tool.kt` - Defines `Tool` object with name, description, inputSchema
   - Handler function - `suspend (CallToolRequest) -> CallToolResult`
2. Handler calls `DoorayClient` interface methods
3. `DoorayHttpClient` implements actual HTTP requests with error handling

### Critical Design Patterns

**Logging hygiene**:
- MCP protocol uses **stdout** for JSON-RPC messages
- **ALL logs MUST go to stderr** (enforced in `Main.kt` via System properties)
- Default log levels: `DOORAY_LOG_LEVEL=WARN`, `DOORAY_HTTP_LOG_LEVEL=WARN`

**HTTP Client architecture** (`DoorayHttpClient.kt`):
- Implements `DoorayClient` interface with 50+ Dooray API methods
- Uses Ktor's ContentNegotiation + kotlinx.serialization for JSON
- Supports 307 redirects for Drive API (`api.dooray.com` → `file-api.dooray.com`)
- Base64 encoding/decoding for file upload/download

**Error handling**:
- Custom exceptions: `ToolException`, `CustomException`
- Consistent error responses via `DoorayApiErrorType`
- Tool responses wrapped in `ToolResponseTypes` (Success/Error)

## Key Files

- `Main.kt` - Entry point with logging configuration
- `DoorayMcpServer.kt` - Server initialization and tool registration (lines 89-286)
- `client/DoorayClient.kt` - Interface defining all Dooray API methods
- `client/DoorayHttpClient.kt` - Ktor-based HTTP client implementation
- `tools/*.kt` - 48 tool definitions and handlers
- `types/*.kt` - Request/response data classes with kotlinx.serialization
- `constants/ToolCategory.kt` - Tool category enum with parsing logic
- `constants/EnvVariableConst.kt` - Environment variable names
- `constants/VersionConst.kt` - Application version constant

## Environment Variables

| Variable | Purpose | Required | Default |
|----------|---------|----------|---------|
| `DOORAY_API_KEY` | Dooray API authentication | Yes | - |
| `DOORAY_BASE_URL` | Dooray API base URL | Yes | - |
| `DOORAY_ENABLED_CATEGORIES` | Filter tools by category (wiki,project,messenger,calendar,drive) | No | All enabled |
| `DOORAY_LOG_LEVEL` | General log level (DEBUG/INFO/WARN/ERROR) | No | WARN |
| `DOORAY_HTTP_LOG_LEVEL` | HTTP client log level | No | WARN |
| `CI` | Auto-excludes integration tests if "true" | No | - |

## Testing Strategy

- **Unit tests**: `src/test/kotlin/**/McpToolsUnitTest.kt`
- **Integration tests**: `src/test/kotlin/**/dooray/*IntegrationTest.kt`
  - Require real Dooray API credentials
  - Automatically excluded in CI environments (`CI=true`)
- **Test utilities**: `util/TestUtil.kt`, `ClientStdio.kt`

## Code Style

- Kotlin idiomatic style with coroutines (`suspend` functions)
- 4-space indentation (Kotlin standard)
- Explicit return types for public APIs
- Interface-based design (`DoorayClient` interface)
- Data classes with kotlinx.serialization annotations
- Nullable types for optional parameters

## Common Development Tasks

### Adding a new tool

1. Create `*Tool.kt` in `tools/` with `Tool` object and handler function
2. Define input schema using JSON schema
3. Add corresponding method to `DoorayClient` interface
4. Implement HTTP call in `DoorayHttpClient`
5. Register tool in `DoorayMcpServer.registerTool()` with appropriate `ToolCategory`
6. Update tool count in comments (line 282)

### Modifying API client

- All API methods are defined in `client/DoorayClient.kt` interface
- Implementations use Ktor's `HttpClient` with retry logic
- Request/response types live in `types/*.kt`
- Follow existing patterns for error handling and JSON serialization

### Debugging MCP communication

- Check stderr for logs (stdout is protocol-only)
- Increase log levels: `DOORAY_LOG_LEVEL=DEBUG DOORAY_HTTP_LOG_LEVEL=DEBUG`
- Use `./gradlew runLocal` for local testing with stdin/stdout
- Docker: Add `-v` mounts and inspect container logs with `docker logs`

## Docker Notes

- **Current support**: AMD64 only
- **ARM64 status**: Temporarily disabled due to QEMU + Gradle dependency download timeouts
- Re-enable ARM64: Set `ENABLE_ARM64: true` in `.github/workflows/docker-publish.yml`
- Multi-stage build: gradle:8.10-jdk21 → eclipse-temurin:21-jre-alpine
- Non-root user: `dooray:dooray` (UID/GID 1000)
- JAR location: `/app/app.jar` (wildcard copy from builder stage)

## Important Implementation Details

### File Upload Strategy

Two methods exist with clear precedence:
1. **`dooray_drive_upload_file_from_path`** (PREFERRED) - Direct file path upload
   - Handles Base64 encoding server-side
   - Bypasses Claude message length limits
   - Supports up to 100MB files
   - Docker path translation: `/Users/{user}/Downloads` → `/host/Downloads`

2. **`dooray_drive_upload_file`** (FALLBACK) - Base64 content upload
   - For small files (<10KB) or when file path unavailable
   - Subject to Claude ~200K character message limit

### Messenger Mention Format

Channels support user mentions and @channel:
- User: `[@Name](dooray://orgId/members/memberId "member")`
- All: `[@Channel](dooray://orgId/channels/channelId "channel")`
- Implemented in `SendChannelMessageTool` with automatic deduplication

### Drive API 307 Redirect Handling

Drive operations require following 307 redirects:
- Initial request: `api.dooray.com`
- Redirected to: `file-api.dooray.com`
- Implemented in `DoorayHttpClient` with Ktor's `followRedirects`

## Version History

Check `git log` for detailed changelog. Recent versions focus on:
- v0.2.27: Task list optimization (lightweight response)
- v0.2.26: Tool category filtering
- v0.2.25: Drive change history API
- v0.2.24: File rename API
- v0.2.21-23: Drive filtering and improvements
