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
