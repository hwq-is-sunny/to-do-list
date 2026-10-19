# Architecture and Engineering Decisions

## High-level structure

Compose UI
  -> ViewModel / UI state
    -> Repository
      -> Room DAOs -> SQLite
      -> NoticeParser -> ParsedCandidateDraft
      -> LocalRuleDedupService -> DedupDecision
      -> DeepSeekAssist (optional)
           -> input validation
           -> HTTPS Chat Completions request
           -> JSON / timetable schema validation
      -> Candidate review and explicit user confirmation

Background work:
- WorkManager -> task reminders / focus reminders
- Android NotificationListenerService -> optional imported-notice candidate
- ML Kit -> OCR text -> optional LLM timetable structuring

## Key decisions

### Local-first data flow

Tasks, courses, timetable slots, and candidate drafts are persisted locally with Room. Parsing and reminder features can operate without a provider key. Imported text is stored as a candidate and is reviewed before becoming a task.

### Hybrid parsing

Rule-based parsing is deterministic and works offline. Optional LLM assistance helps with irregular text and timetable OCR, but its response is treated as untrusted data. Structured output is checked before it is mapped into application models.

### Duplicate suggestions

The current duplicate scorer combines:
- title similarity (token overlap and edit distance),
- course-name similarity,
- due-time proximity.

The score is heuristic. The repository includes unit tests for exact-match and no-existing-item cases, but it does not yet contain a labeled benchmark or precision/recall evaluation.

### Credential and request safeguards

- API keys are encrypted at rest with AES-GCM and a key managed by Android Keystore.
- The app disables Android data backup so local credentials are not copied with app backup. This also means local tasks and settings are not backed up automatically.
- AI endpoints must use HTTPS and may not embed credentials, query parameters, or fragments.
- Request input sizes are bounded; common prompt-injection patterns are checked heuristically.
- AI task output is validated for required fields, field lengths, dates, times, enums, and confidence range.
- Timetable output is validated for a five-column row schema, valid weekdays, and increasing time ranges.
- Error messages do not include the provider's response body.

### Local password migration

New local demo-account passwords use PBKDF2-HMAC-SHA256 with a random 16-byte salt and 210,000 iterations. A successful verification of a legacy salted SHA-256 hash triggers a rehash with a new salt. Password derivation runs off the main thread.

## Known limitations

- Local accounts are not server-authenticated and do not isolate the shared Room database by account.
- Client-side prompt-injection filtering is not a security boundary; a backend is required for server-side authorization, quotas, and provider credential isolation.
- API-key encryption depends on the local Android Keystore key. If that key becomes unavailable, the saved key is treated as unavailable and must be entered again.
- The app currently uses destructive Room migration fallback; schema changes can erase local data. A production release should add and test explicit Room migrations and enable schema export.
- No device farm, emulator UI tests, accessibility audit, or independent security review has been completed.

## Automated validation

The CI workflow runs:
- ./gradlew testDebugUnitTest
- ./gradlew lintDebug
- ./gradlew assembleDebug
- ./gradlew assembleRelease

CI results establish that these tasks pass on the configured Ubuntu/JDK/Android SDK environment. They do not prove that every UI flow, notification permission path, OCR input, or device-specific behavior works at runtime.
