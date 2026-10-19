# CampusTodo — Resume and Interview Notes

> Keep only claims that you can personally explain and demonstrate. The app is a local-first Android prototype, not a production multi-user service.

## Chinese resume bullets

**CampusTodo｜AI 辅助的校园待办与课表管理 Android 应用**  
技术栈：Kotlin、Jetpack Compose、Room、DataStore、Coroutines/Flow、WorkManager、OkHttp、ML Kit、GitHub Actions

- 基于 Kotlin + Jetpack Compose 开发校园任务管理应用，使用 MVVM/Repository 分层组织 UI 状态、业务逻辑与 Room 本地持久化，支持待办、课程、课表、候选箱及本地提醒。
- 实现中文校园通知规则解析与候选任务确认流程，结合标题、课程和截止时间相似度计算重复任务分数，辅助用户识别潜在重复事项。
- 集成 ML Kit 中文 OCR 与兼容 Chat Completions 的 LLM 接口，将课表识别文本结构化为可审阅的课表条目；对模型输入设置长度限制，对任务 JSON、日期、时间、枚举值、置信度和课表行格式进行校验。
- 完成 AI 接口安全加固：仅允许 HTTPS 端点、避免在异常信息中回显服务端响应正文，并使用 Android Keystore + AES-GCM 加密本地保存的 API Key。
- 将本地账户密码哈希升级为 PBKDF2-HMAC-SHA256（随机盐、210,000 次迭代），兼容旧版 SHA-256 哈希并在成功登录后迁移；使用 GitHub Actions 自动执行单元测试、Android Lint 及 Debug/Release 构建。

## English resume bullets

**CampusTodo — AI-Assisted Campus Task and Timetable Manager**  
*Tech: Kotlin, Jetpack Compose, Room, DataStore, Coroutines/Flow, WorkManager, OkHttp, ML Kit, GitHub Actions*

- Built a local-first Android app for campus tasks, courses, timetables, candidate review, and scheduled reminders using Kotlin, Jetpack Compose, Room, and a ViewModel/Repository architecture.
- Implemented rule-based Chinese notice parsing and duplicate-task scoring based on title, course, and due-time similarity, with a user-confirmation step before converting imported notices into tasks.
- Integrated ML Kit Chinese OCR and a configurable Chat Completions API to structure timetable text; validated model-generated JSON fields, enums, dates, time ranges, confidence values, and timetable rows before accepting suggestions.
- Hardened the optional AI path with HTTPS-only endpoint validation, bounded input sizes, heuristic prompt-injection checks, and AES-GCM API-key encryption backed by Android Keystore.
- Improved local password handling with salted PBKDF2-HMAC-SHA256 (210,000 iterations) and successful-login migration from legacy hashes; added GitHub Actions for unit tests, Android Lint, and Debug/Release builds.

## Interview preparation

### 1. Why keep rule-based parsing if the app already calls an LLM?

Rule-based parsing works offline, is predictable, and has no per-request cost. The LLM is an optional fallback for less structured text. The candidate-review step prevents model output from silently becoming an action.

### 2. How does duplicate detection work?

The current implementation computes a weighted score from title similarity, course similarity, and due-time distance. It maps scores to high-duplicate, possible-merge, or new-item. The thresholds are heuristic and should be tuned against a labeled evaluation set before making accuracy claims.

### 3. What does the AI security layer protect against?

It limits input length, blocks a small set of common prompt-injection phrases, marks imported text as untrusted in prompts, validates structured output, requires HTTPS for provider endpoints, and avoids echoing response bodies in errors. These are defense-in-depth checks, not a complete prompt-injection solution.

### 4. Why use PBKDF2 instead of a single SHA-256 hash?

Password hashing should be deliberately expensive. PBKDF2-HMAC-SHA256 with a per-account random salt makes offline guessing more costly. Existing hashes are verified once and upgraded after successful login so current users do not have to reset passwords.

### 5. What are the main limitations?

- Accounts are local demo accounts; there is no server-side authentication or multi-device account isolation.
- The application uses one local Room database, so data is not partitioned by account.
- Prompt-injection checks are heuristic and can miss novel attacks or flag legitimate text.
- There is no production backend, rate limiting, or formal security audit.
- UI behavior and notifications still need manual verification on real devices/emulators; CI only proves that the automated checks and packaging tasks pass.

## Before an interview

1. Install the debug APK and demonstrate creating a task, importing a notice, reviewing a candidate, and viewing the timetable.
2. Open the GitHub Actions run and explain what each automated job checks.
3. Be ready to explain the password-hash migration and why local authentication is not equivalent to server authentication.
4. Do not claim model accuracy, production security, or multi-user isolation without measurements and implementation evidence.
