# Backend Foundation - FE Guide

## Runtime

- Application: `mentify-be`
- Local base URL: `http://localhost:8080`
- Business timezone: `Asia/Ho_Chi_Minh`
- Audit timestamps use ISO 8601 with an explicit `+07:00` offset for display. The persisted value represents the same instant and is not shifted by adding a fixed seven hours.
- PostgreSQL is required for application startup after Task 1.2.

## Security baseline

- `GET /actuator/health` and its health subpaths are public.
- Business APIs under `/api/v1/**` require an authenticated request with a JWT Bearer token (`Authorization: Bearer <token>`). Public authentication endpoints include OAuth initiation/callback code exchange (`POST /api/v1/auth/oauth2/exchange`), password login/reset, and email-change confirmation; password setting, email-change requests, and Google identity linking require an authenticated JWT Bearer token.
- `/api/v1/admin/**` requires the `ADMIN` role.
- Google OAuth and password login flows issue JWT tokens when their production configuration is enabled. Dev-login remains limited to local/dev/test profiles; password reset and email-change links require an email sender configuration in deployed environments.
- The application uses stateless JWT authentication. State-changing requests do not require CSRF cookies (`XSRF-TOKEN`) or headers (`X-XSRF-TOKEN`).
- Configure trusted frontend origins with the comma-separated `CORS_ALLOWED_ORIGINS` environment variable. It is empty by default; wildcard origins are not enabled with credentials.

Unauthenticated and unauthorized requests keep the shared error schema with codes `UNAUTHENTICATED` (401) and `FORBIDDEN` (403), including `requestId` and the `+07:00` timestamp.

## Health endpoint

### `GET /actuator/health`

Use this endpoint only for application availability checks.

Successful response:

```json
{
  "status": "UP"
}
```

Only the `health` actuator endpoint is exposed. Environment, beans, configuration properties and other actuator details are not public.

The health status includes database availability internally, but the response does not expose database URL, host, username, password, or other connection details.

This endpoint is infrastructure-only and is not a user or business API. No authentication contract is defined yet; security will be added in a later task.

## API error contract

Business API errors use one shared shape. Success responses are not wrapped by this contract.

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dữ liệu không hợp lệ",
    "details": [
      { "field": "email", "message": "Email không hợp lệ" }
    ]
  },
  "timestamp": "2026-09-27T15:00:00+07:00",
  "requestId": "..."
}
```

`timestamp` is ISO 8601 with the `Asia/Ho_Chi_Minh` offset. `requestId` is also returned in the `X-Request-Id` response header. A valid incoming `X-Request-Id` (maximum 64 safe characters) is preserved; otherwise the backend generates a UUID.

Error codes map to HTTP status as follows: `VALIDATION_ERROR` 400, `UNAUTHENTICATED` 401, `FORBIDDEN` 403, `RESOURCE_NOT_FOUND` 404, `CONFLICT` 409, and `INTERNAL_ERROR` 500. Frontend validation should read `error.details`; internal errors expose only a generic message.

## API success contract

Controllers explicitly return the shared success shape; responses are not automatically wrapped.

```json
{
  "success": true,
  "data": [{ "id": "example-1" }],
  "meta": {
    "page": 0,
    "size": 20,
    "totalItems": 31,
    "totalPages": 2
  },
  "timestamp": "2026-09-27T15:00:00+07:00",
  "requestId": "..."
}
```

For non-paginated responses, `meta` is `null`. `page` starts at 0; `size` must be greater than 0. `totalPages` is rounded up and is `0` when `totalItems` is `0`. `data` contains DTOs, not JPA entities. The success response uses the same `requestId`, `X-Request-Id` header and `+07:00` timestamp convention as the error response.

## Idempotency for state-changing APIs

Mutation endpoints with an external side effect accept the following header:

```http
Idempotency-Key: <client-generated-key>
```

The key is scoped to the authenticated actor, HTTP method and path. Repeating
the same request with the same key replays the original response. Reusing the
same key with a different request body returns `409 CONFLICT`. Keys are kept
for 24 hours and must not contain credentials, tokens or personal data.
