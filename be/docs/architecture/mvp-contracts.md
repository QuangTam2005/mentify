# Mentify MVP Contracts

This document is the implementation contract for mentor, booking, no-show and
review features. It is deliberately independent from JPA entities so that the
public API and state rules remain stable while persistence evolves.

## Module boundaries

- `mentor` owns the mentor profile, application, verification status and
  `acceptingBookings` flag.
- `availability` owns recurring availability rules and generated slots.
- `booking` owns booking state and booking participants. It asks `mentor` for
  booking eligibility and `availability` for slot ownership through public
  application services; it does not access their repositories or entities.
- `moderation` owns reports, no-show cases, strikes, review hiding and banned
  phrases.
- `review` owns review creation, editing and rating aggregation.
- `audit` owns immutable audit events and is called through its public service.

Cross-module calls must use an application boundary. DTOs or command records
are exchanged across boundaries; JPA entities and repositories are not.

## Mentor status transitions

| From | To | Actor | Preconditions | Side effect |
|---|---|---|---|---|
| `NOT_APPLIED` | `PENDING` | User | User is an eligible university student or working professional and has one valid application | Store application and evidence references |
| `PENDING` | `APPROVED` | Admin | Application and evidence are reviewable | Enable mentor discovery; audit decision |
| `PENDING` | `REJECTED` | Admin | Rejection reason is present | Store reason and review history; audit decision |
| `REJECTED` | `PENDING` | User | Application was updated and resubmitted | Create a new review attempt; preserve history |
| `APPROVED` | `SUSPENDED` | Admin | Safety/moderation decision has a reason | Disable new booking confirmation; audit decision |
| `SUSPENDED` | `APPROVED` | Admin | Suspension review is resolved | Restore eligibility; audit decision |

Changing `userType` never changes mentor status.

## Booking status transitions

| From | To | Actor | Preconditions |
|---|---|---|---|
| `PENDING` | `CONFIRMED` | Mentor | Mentor is approved, accepting bookings, not restricted; slot has no conflicting confirmed booking |
| `PENDING` | `DECLINED` | Mentor/System | Mentor declines, or another request claims the slot |
| `PENDING` | `CANCELLED_BY_LEARNER` | Learner | Learner owns the request |
| `PENDING` | `EXPIRED` | System | Start time has passed without confirmation |
| `CONFIRMED` | `CANCELLED_BY_LEARNER` | Learner | Learner owns booking; cancellation rule is evaluated |
| `CONFIRMED` | `CANCELLED_BY_MENTOR` | Mentor | Mentor owns booking; cancellation rule is evaluated |
| `CONFIRMED` | `RESCHEDULE_REQUESTED` | Either participant | Booking is confirmed and proposed time is valid |
| `CONFIRMED` | `COMPLETED` | Either participant/System | Appointment end time has passed and it is safe to complete |
| `CONFIRMED` | `NO_SHOW` | System/Admin | A no-show case is concluded; booking is not changed to `NO_SHOW` merely by a report |
| `RESCHEDULE_REQUESTED` | `CONFIRMED` | Other participant | Proposed time is accepted and the same overlap checks pass |
| `RESCHEDULE_REQUESTED` | `CANCELLED_BY_LEARNER` / `CANCELLED_BY_MENTOR` | Requesting participant | Cancellation is valid under the cancellation rule |

The `RESCHEDULE_REQUESTED` record stores `previousStatus`, proposed start/end,
requesting actor and decision metadata. It does not create a second booking.
Confirmation must lock all potentially overlapping confirmed bookings for the
mentor before checking and writing the result. The operation is idempotent:
repeating the same command returns the first result; a different command using
the same idempotency key returns `409 CONFLICT`.

## No-show and review rules

- A booking has at most one active no-show case.
- The reported participant has eight hours to respond.
- An unanswered case is concluded by the system; a disputed case is queued for
  admin review.
- A late cancellation creates one strike-equivalent violation. The same
  booking cannot also add another strike for no-show.
- Strikes are counted by calendar month in `Asia/Ho_Chi_Minh`; three strikes
  impose a 14-day new-booking restriction.
- Reviews can only be created after `COMPLETED`, within seven days, and can be
  edited once within the first 24 hours. Admin hiding requires a reason and an
  audit event.

## Mutation idempotency contract

All state-changing endpoints with an external side effect accept:

```http
Idempotency-Key: <client-generated-key>
```

- Maximum length is 128 characters.
- Scope is `actor + HTTP method + path + key`.
- The request hash, HTTP status, response body and expiry are persisted.
- Same scope and same request hash replays the original response.
- Same scope and a different request hash returns `409 CONFLICT`.
- Default retention is 24 hours.
- Keys are required for booking confirmation, cancellation, rescheduling,
  no-show reporting/conclusion, review submission and moderation actions.

## Shared API behavior

Every endpoint must declare authentication, authorization, preconditions,
postconditions, validation errors, not-found behavior and concurrency conflict
behavior. Responses use the existing success/error envelopes and request ID
conventions. Business timestamps are evaluated in `Asia/Ho_Chi_Minh` and are
persisted as instants.
