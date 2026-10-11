# Mentor Application API

## Current user's application

- **Method:** `GET`
- **Path:** `/api/v1/mentor/applications/me`
- **Authentication:** required. Send `Authorization: Bearer <JWT>`.
- **Success:** `200 OK`, wrapped in `ApiSuccessResponse<CurrentMentorApplicationResponse>`.

The endpoint derives the applicant from the authenticated principal. It returns the applicant's most recent attempt, ordered by `attemptNumber` descending.

```json
{
  "success": true,
  "data": {
    "mentorProfileId": "2d263f40-8cef-4390-bb43-e524f571e1b9",
    "mentorApplicationId": "713ba40a-d8cb-4ee4-90a6-8167f2b9829e",
    "mentorStatus": "PENDING",
    "applicationStatus": "PENDING",
    "attemptNumber": 1,
    "appliedUserType": "UNIVERSITY_STUDENT",
    "schoolId": "e229d494-b789-4fa2-bceb-fcead6151740",
    "majorGroupCode": "7480",
    "customMajorName": "Computer Science",
    "bio": "I enjoy mentoring students.",
    "acceptingBookings": false,
    "submittedAt": "2026-10-01T13:30:00Z"
  },
  "meta": null,
  "timestamp": "2026-10-01T20:30:00+07:00",
  "requestId": "90cf85b2-29ee-473d-82d8-79e51c890bd9"
}
```

`applicationStatus` values: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`.
`mentorStatus` values: `NOT_APPLIED`, `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`.
Only applicant-facing application snapshot fields are returned. Review notes and timestamps are not part of this DTO.

### Errors

- `401 UNAUTHENTICATED`: missing, expired, or invalid JWT.
- `403 FORBIDDEN`: authenticated account is locked or otherwise denied by security policy.
- `404 RESOURCE_NOT_FOUND`: the authenticated user has no mentor application.

## Initial submission

- **Method:** `POST`
- **Path:** `/api/v1/mentor/applications`
- **Authentication:** required. Send `Authorization: Bearer <JWT>`.
- **Success:** `201 Created`, wrapped in `ApiSuccessResponse<MentorApplicationSubmissionResponse>`.

This route is for an initial application only. Its existing duplicate-submission behavior is unchanged.

## Resubmitting a rejected application

- **Method:** `POST`
- **Path:** `/api/v1/mentor/applications/resubmit`
- **Authentication:** required. Send `Authorization: Bearer <JWT>`.
- **Success:** `201 Created`, wrapped in `ApiSuccessResponse<MentorApplicationSubmissionResponse>`.

Only the authenticated applicant's **latest** attempt may be resubmitted, and only when its `applicationStatus` is `REJECTED`. An older rejected attempt does not qualify when a newer attempt exists. The backend creates a new application ID on the same mentor profile, increments `attemptNumber`, and sets the new application and mentor profile status to `PENDING`. Previous attempts remain available as history.

### Request body (`SubmitMentorApplicationRequest`)

```json
{
  "schoolId": "e229d494-b789-4fa2-bceb-fcead6151740",
  "majorGroupCode": "7480",
  "customMajorName": "Computer Science",
  "bio": "Updated introduction",
  "expertiseAreas": "Java, Spring Boot",
  "defaultMeetingLink": "https://meet.example/mentor"
}
```

All fields are optional. Constraints: `majorGroupCode` up to 4 characters, `customMajorName` up to 255, `bio` up to 1000, `expertiseAreas` up to 500, and `defaultMeetingLink` up to 512. `schoolId` must be a UUID. Identity, profile/application IDs, attempt number, status, review timestamps, and review decisions are controlled by the server and must not be sent by the client.

The success response uses `MentorApplicationSubmissionResponse`:

```json
{
  "success": true,
  "data": {
    "mentorProfileId": "2d263f40-8cef-4390-bb43-e524f571e1b9",
    "mentorApplicationId": "a13c3fd2-4212-42b0-b612-54f823fd94ba",
    "userId": "d94ee48c-6801-44aa-8ff7-8e9b57a3e3ea",
    "mentorStatus": "PENDING",
    "applicationStatus": "PENDING",
    "attemptNumber": 2,
    "appliedUserType": "UNIVERSITY_STUDENT",
    "acceptingBookings": false,
    "submittedAt": "2026-10-11T15:30:00Z"
  },
  "meta": null,
  "timestamp": "2026-10-11T22:30:00+07:00",
  "requestId": "90cf85b2-29ee-473d-82d8-79e51c890bd9"
}
```

### Resubmission errors

- `400 VALIDATION_ERROR`: invalid JSON or a field exceeds its limit.
- `401 UNAUTHENTICATED`: missing, expired, or invalid JWT.
- `403 FORBIDDEN`: locked account, suspended mentor profile, or ineligible user type.
- `404 RESOURCE_NOT_FOUND`: no mentor profile or no previous application exists.
- `409 CONFLICT`: the latest attempt is not `REJECTED`, or another resubmission has already won a concurrent request.

Do not automatically retry this POST after a timeout or `409`. First call `GET /api/v1/mentor/applications/me` to check whether the new attempt was created. Concurrent requests are serialized by the backend; at most one can create the next attempt.

## Admin application review

All routes in this section require a valid access token with the server-side `ADMIN` role. The API enforces this with Spring Security; hiding controls in the UI is not an authorization check.

### List pending applications

- **Method:** `GET`
- **Path:** `/api/v1/admin/mentor/applications`
- **Query:** `page` (zero-based, default `0`), `size` (default `20`, range `1`–`100`).
- **Success:** `200 OK`, `ApiSuccessResponse<MentorApplicationAdminListItem[]>`.

The response `meta` contains `page`, `size`, `totalItems`, and `totalPages`. Queue entries are ordered by submission time and ID. Only the latest attempt for each Mentor Profile is included, and only when that attempt is `PENDING`.

Each list item contains `applicationId`, `mentorProfileId`, `userId`, applicant `email` and `applicantName` (nullable), `applicationStatus`, `attemptNumber`, `appliedUserType`, and `submittedAt`.

### Review details

- **Method:** `GET`
- **Path:** `/api/v1/admin/mentor/applications/{applicationId}`
- **Success:** `200 OK`, `ApiSuccessResponse<MentorApplicationAdminDetail>`.

The detail DTO includes application/profile/user IDs, applicant email and optional name, applied user type, the application snapshot (`schoolId`, `majorGroupCode`, `customMajorName`, `bio`), current profile `expertiseAreas` and `defaultMeetingLink`, attempt/status fields, booking availability, submission/review timestamps, and the stored rejection reason. Passwords, tokens, and authentication attributes are excluded. Historical applications can be read for context, but cannot be reviewed.

### Approve

- **Method:** `POST`
- **Path:** `/api/v1/admin/mentor/applications/{applicationId}/approve`
- **Body:** none.
- **Success:** `200 OK`, `ApiSuccessResponse<MentorApplicationAdminDecision>`.

Only the latest `PENDING` attempt on a non-suspended, eligible Mentor Profile can be approved. The application and profile become `APPROVED`; `acceptingBookings` remains `false`. The server sets `reviewedAt`.

### Reject

- **Method:** `POST`
- **Path:** `/api/v1/admin/mentor/applications/{applicationId}/reject`
- **Body:** `{"rejectionReason":"Please add more detail about your mentoring experience."}`. The reason is required, non-blank after trimming, and limited to 500 characters.
- **Success:** `200 OK`, `ApiSuccessResponse<MentorApplicationAdminDecision>`.

Only the latest `PENDING` attempt on a non-suspended, eligible Mentor Profile can be rejected. The application and profile become `REJECTED`; prior attempts remain unchanged. The server sets `reviewedAt` and stores the reason in the existing `rejectionReason` field.

The decision DTO contains `applicationId`, `mentorProfileId`, `userId`, `applicationStatus`, `mentorStatus`, `attemptNumber`, `acceptingBookings`, `reviewedAt`, and `rejectionReason`.

### Admin review errors and transitions

- `400 VALIDATION_ERROR`: invalid page/size, malformed UUID, or missing/invalid rejection reason.
- `401 UNAUTHENTICATED`: missing or invalid access token.
- `403 FORBIDDEN`: authenticated non-admin, suspended profile, or applicant no longer eligible.
- `404 RESOURCE_NOT_FOUND`: application or related record does not exist.
- `409 CONFLICT`: application is no longer the latest `PENDING` attempt, or another review decision already won.

Only `PENDING` can transition to `APPROVED` or `REJECTED`. Competing admin decisions serialize on the Mentor Profile; one succeeds and the other receives `409 CONFLICT`. Do not automatically retry a review POST after a timeout or conflict; reload the detail or pending queue first.
