# SEAL Backend MVP

## Implemented Scope

Existing opaque bearer authentication, PostgreSQL configuration and layered Spring
architecture are retained. JavaFX communicates only through REST; no database credentials
are added to the client.

- Admin creates teacher/student accounts, lists users and activates/deactivates them.
  Admin creation through the user API is rejected. Deactivation revokes existing sessions.
- Teacher creates, edits drafts, publishes, lists and closes owned exams.
- Student validates an access code, starts one timed attempt and submits answers.
- Server grades MCQs. Teachers grade all text responses with bounded marks; students
  can retrieve only their own result summaries.
- Role, active-account and ownership checks run on the server. Student question DTOs
  omit correct options and marking guides.

## API

All examination routes require the existing Authorization: Bearer token.

| Method | Route | Caller |
| --- | --- | --- |
| POST / GET | /api/exams | Teacher: create / list owned |
| GET / PUT | /api/exams/{id} | Owner: read / update draft |
| PATCH | /api/exams/{id}/publish | Owner |
| PATCH | /api/exams/{id}/close | Owner |
| GET | /api/exams/{id}/submissions | Owner |
| GET | /api/exams/{id}/submissions/{submissionId} | Owner |
| PATCH | /api/exams/{id}/submissions/{submissionId}/grade | Owner |
| POST | /api/student/exams/access | Student |
| POST | /api/student/exams/{id}/start | Student with validated access |
| POST | /api/student/exams/{id}/submit | Student with started attempt |
| GET | /api/student/exams/results | Student, own results only |
| PATCH | /api/admin/users/{id}/active | Active admin |

Access body: registrationNumber, symbolNumber, accessCode.
Submission body is a bare array of questionId plus selectedOptionIndex or textAnswer.
MCQ indices are zero-based, 0-3. Grading body is a bare array of questionId and
awardedMarks for every text question, exactly once. MCQ marks cannot be changed
through manual grading. Activation body is {"active":false} or {"active":true}.

## Presentation Map

Request -> REST Controller -> Authorization + Service -> Repository -> JPA -> PostgreSQL.

ExamService owns exam authoring/lifecycle. StudentExamService owns access/start/submit.
GradingService owns teacher grading and result reads. ExamViews separates student-safe
questions from teacher answer keys. ExamAuthorization reuses SessionService/UserRepository.
SessionService remains an in-memory opaque-token store; restart requires login again.

User(teacher) -> Exams -> Questions -> QuestionOptions.
User(student) + Exam -> Submission -> Answers -> Question.
Cascades persist child records, not users. Unique constraints protect access codes,
student/exam attempts and submission/question answers. Exam-row locks serialize
state changes for this MVP; DTO mapping stays inside transactions.

## Deliberate Limits

Registration and symbol numbers are stored as unverified student-supplied information,
not checked against institutional enrollment. Authenticated identity comes from login.
One attempt per student/exam; start is idempotent. Identical submission retries return
the receipt; altered resubmission is rejected. There is a 30-second transport grace
after server expiry. The client submits on timeout; there is no backend auto-submit job.
Closing an exam blocks subsequent new submissions, including active attempts.
Text-containing submissions show a partial MCQ subtotal until manual grading completes.
There is no answer recovery after desktop exit, exam deletion, extra attempts,
question archive, multi-institution tenancy, webcam, AI proctoring or OS lockdown.

## Run and Verify

Restart an older server before testing new routes, then log in again:

    ./mvnw spring-boot:run

Keep existing local/Neon configuration. No credentials or production configuration were
changed. Existing ddl-auto=update applies schema changes when connected. Review/back up
important data before deployment. The new schema has not been verified against Neon.

Isolated test suite (does not access Neon):

    ./mvnw -Dtest=ExamLifecycleTests,AdminUserServiceTests test

14 tests passed: 10 lifecycle HTTP/JPA tests and 4 admin service tests.
The original SealServerApplicationTests loads the configured database and is deliberately
excluded. Tests use H2; PostgreSQL deployment remains a separate check.
Compiled for Java 21 using installed JDK 26; runtime on JDK 21 remains unverified.

For the real JavaFX integration probe, start the test-only H2 fixture on port 18081:

    ./mvnw -DskipTests test-compile dependency:build-classpath -Dmdep.outputFile=target/test-classpath.txt
    java -cp "target/test-classes:target/classes:$(cat target/test-classpath.txt)" com.seal.seal_server.MvpFixture

Then run sh tools/check-mvp-ui.sh from the client repository. The fixture overrides
database/import/bootstrap settings; its disposable credentials are test-only.
Stop it with Ctrl-C after testing. Never point this probe at a real institutional database.
