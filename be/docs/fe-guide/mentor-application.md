# Mentor Application API

## Current user's application

- **Method:** `GET`
- **Path:** `/api/v1/mentor/applications/me` (alias: `/api/mentor/applications/me`)
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

The existing submission route remains `POST /api/v1/mentor/applications` (alias `/api/mentor/applications`) and keeps its existing response and duplicate-submission behavior.
