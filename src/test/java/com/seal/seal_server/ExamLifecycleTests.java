package com.seal.seal_server;

import com.seal.seal_server.model.*;
import com.seal.seal_server.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:examtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "seal.admin.username=exam-test-admin",
        "seal.admin.password=local-test-only-password"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExamLifecycleTests {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired ExamRepository exams;
    @Autowired SubmissionRepository submissions;
    @Autowired PlatformTransactionManager transactions;
    @Autowired com.seal.seal_server.service.SessionService sessions;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private String admin;
    private record Result(int status, JsonNode body) { }
    private record Account(long id, String token) { }

    @BeforeAll void loginAdmin() throws Exception {
        admin = login("exam-test-admin", "local-test-only-password");
    }
    private Result request(String method, String path, String token, Object body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(java.time.Duration.ofSeconds(10)).header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode bodyNode;
        try { bodyNode = json.readTree(response.body()); }
        catch (Exception plainTextResponse) { bodyNode = json.valueToTree(response.body()); }
        return new Result(response.statusCode(), bodyNode);
    }
    private String login(String username, String password) throws Exception {
        Result result = request("POST", "/api/auth/login", null, Map.of("username", username, "password", password));
        assertEquals(200, result.status, result.body.toString());
        return result.body.get("token").asText();
    }
    private Account account(String role) throws Exception {
        String name = "exam-" + UUID.randomUUID();
        Result created = request("POST", "/api/admin/users", admin,
                Map.of("fullName", name, "username", name, "password", "local-test-password", "role", role));
        assertEquals(201, created.status, created.body.toString());
        return new Account(created.body.get("id").asLong(), login(name, "local-test-password"));
    }
    private Map<String, Object> mcq() {
        return Map.of("type", "MCQ", "questionText", "Choose B", "marks", 3,
                "options", List.of("A", "B", "C", "D"), "correctOptionIndex", 1);
    }
    private Map<String, Object> text() {
        return Map.of("type", "TEXT", "questionText", "Explain", "marks", 5, "referenceAnswer", "SECRET_MARKING_GUIDE");
    }
    private Map<String, Object> examRequest(String status, List<?> questions) {
        return Map.of("title", "HTTP examination", "subject", "Computing", "subjectCode", "CS101",
                "durationMinutes", 15, "instructions", "Answer all questions", "status", status, "questions", questions);
    }
    private JsonNode create(Account teacher, String status, List<?> questions) throws Exception {
        Result result = request("POST", "/api/exams", teacher.token, examRequest(status, questions));
        assertEquals(201, result.status, result.body.toString());
        return result.body;
    }
    private Result access(Account student, JsonNode exam) throws Exception {
        return request("POST", "/api/student/exams/access", student.token,
                Map.of("registrationNumber", "unverified-reg", "symbolNumber", "unverified-symbol",
                        "accessCode", exam.get("accessCode").asText()));
    }
    private String path(JsonNode exam, String suffix) { return "/api/student/exams/" + exam.get("id").asLong() + suffix; }
    private void start(Account student, JsonNode exam) throws Exception {
        assertEquals(200, access(student, exam).status);
        assertEquals(200, request("POST", path(exam, "/start"), student.token, null).status);
    }
    private List<?> answers(JsonNode exam, int option) {
        return List.of(Map.of("questionId", exam.get("questions").get(0).get("id").asLong(), "selectedOptionIndex", option),
                Map.of("questionId", exam.get("questions").get(1).get("id").asLong(), "textAnswer", "My explanation"));
    }

    @Test void adminPolicyAndRevocation() throws Exception {
        assertEquals(400, request("POST", "/api/admin/users", admin,
                Map.of("fullName","Invalid admin","username","forbidden-admin","password","test-password","role","ADMIN")).status);
        Account student = account("STUDENT");
        assertEquals(200, request("PATCH", "/api/admin/users/" + student.id + "/active", admin, Map.of("active",false)).status);
        assertEquals(200, request("PATCH", "/api/admin/users/" + student.id + "/active", admin, Map.of("active",true)).status);
        assertEquals(401, request("GET", "/api/student/exams/results", student.token, null).status);
        User disabled = new User();
        disabled.setFullName("Disabled admin"); disabled.setUsername("disabled-" + UUID.randomUUID());
        disabled.setPasswordHash("unused"); disabled.setRole(UserRole.ADMIN); disabled.setActive(false);
        disabled = users.saveAndFlush(disabled);
        String token = sessions.createSession(disabled.getId());
        assertEquals(401, request("GET", "/api/admin/users", token, null).status);
        assertEquals(401, request("POST", "/api/admin/users", token,
                Map.of("fullName","N","username","nope","password","P","role","STUDENT")).status);
        assertEquals(401, request("PATCH", "/api/admin/users/" + student.id + "/active", token, Map.of("active",false)).status);
    }
    @Test void draftEditingResultsAndManualGrading() throws Exception {
        Account teacher = account("TEACHER"), student = account("STUDENT"), stranger = account("TEACHER");
        JsonNode draft = create(teacher, "DRAFT", List.of(mcq()));
        long examId = draft.get("id").asLong();
        Result updated = request("PUT", "/api/exams/" + examId, teacher.token, examRequest("PUBLISHED",List.of(mcq(),text())));
        assertEquals(200, updated.status, updated.body.toString());
        JsonNode exam = updated.body;
        assertEquals(2, exam.get("questions").size());
        assertEquals(409, request("PUT", "/api/exams/" + examId, teacher.token, examRequest("DRAFT",List.of(mcq()))).status);
        start(student, exam);
        Result submitted = request("POST", path(exam,"/submit"), student.token, answers(exam,1));
        long id = submitted.body.get("id").asLong();
        String detail = "/api/exams/" + examId + "/submissions/" + id;
        assertEquals(404, request("GET", detail, stranger.token, null).status);
        assertEquals(403, request("GET", detail, student.token, null).status);
        assertEquals(1, request("GET", "/api/exams/" + examId + "/submissions", teacher.token, null).body.size());
        assertTrue(request("GET", detail, teacher.token, null).body.toString().contains("SECRET_MARKING_GUIDE"));
        long textId = exam.get("questions").get(1).get("id").asLong();
        assertEquals(400, request("PATCH", detail + "/grade", teacher.token, List.of(Map.of("questionId",textId,"awardedMarks",6))).status);
        Result graded = request("PATCH", detail + "/grade", teacher.token, List.of(Map.of("questionId",textId,"awardedMarks",4)));
        assertEquals(200, graded.status, graded.body.toString());
        assertEquals(7, graded.body.get("result").get("totalScore").asInt());
        assertEquals("GRADED", graded.body.get("result").get("status").asText());
        JsonNode visible = request("GET", "/api/student/exams/results", student.token, null).body;
        assertEquals(7, visible.get(0).get("totalScore").asInt());
        assertFalse(visible.toString().contains("referenceAnswer"));
        Account otherStudent = account("STUDENT");
        assertEquals(0, request("GET", "/api/student/exams/results", otherStudent.token, null).body.size());
        assertEquals(200, request("PATCH", "/api/exams/" + examId + "/close", teacher.token, null).status);
        assertEquals(404, access(otherStudent, exam).status);
    }
    @Test void fullLifecyclePersistsGradesAndNeverLeaksStudentKeys() throws Exception {
        Account teacher = account("TEACHER"), student = account("STUDENT");
        JsonNode draft = create(teacher, "DRAFT", List.of(mcq(), text()));
        assertTrue(draft.get("accessCode").isNull());
        long id = draft.get("id").asLong();
        Result published = request("PATCH", "/api/exams/" + id + "/publish", teacher.token, null);
        assertEquals(200, published.status);
        JsonNode exam = published.body;
        assertTrue(exam.get("accessCode").asText().matches("[A-Z0-9]{6}"));
        assertEquals(exam.get("accessCode"), request("PATCH", "/api/exams/" + id + "/publish", teacher.token, null).body.get("accessCode"));
        Result visible = access(student, exam);
        assertEquals(200, visible.status);
        assertFalse(visible.body.get("identityDetailsVerified").asBoolean());
        for (String forbidden : List.of("correctOptionIndex", "referenceAnswer", "passwordHash", "SECRET_MARKING_GUIDE"))
            assertFalse(visible.body.toString().contains(forbidden), forbidden);
        Result started = request("POST", path(exam, "/start"), student.token, null);
        assertEquals(200, started.status);
        assertEquals(started.body, request("POST", path(exam, "/start"), student.token, null).body);
        Result submitted = request("POST", path(exam, "/submit"), student.token, answers(exam, 1));
        assertEquals(200, submitted.status, submitted.body.toString());
        assertEquals(3, submitted.body.get("totalScore").asInt());
        assertEquals("SUBMITTED", submitted.body.get("status").asText());
        assertTrue(submitted.body.get("pendingManualGrading").asBoolean());
        assertEquals(submitted.body, request("POST", path(exam, "/submit"), student.token, answers(exam, 1)).body);
        assertEquals(409, request("POST", path(exam, "/submit"), student.token, answers(exam, 0)).status);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            Submission saved = submissions.findByExamIdAndStudentId(id, student.id).orElseThrow();
            assertEquals(2, saved.getAnswers().size());
            assertEquals(3, saved.getTotalScore());
            Answer written = saved.getAnswers().stream().filter(a -> a.getQuestion().getType() == QuestionType.TEXT).findFirst().orElseThrow();
            assertEquals("My explanation", written.getTextAnswer());
            assertNull(written.getAwardedMarks());
        });
    }
    @Test void rolesOwnershipAndInactiveAccountsAreEnforced() throws Exception {
        Account teacher = account("TEACHER"), other = account("TEACHER"), student = account("STUDENT");
        JsonNode exam = create(teacher, "PUBLISHED", List.of(mcq()));
        assertEquals(401, request("GET", "/api/exams", null, null).status);
        assertEquals(401, request("GET", "/api/exams", "invalid", null).status);
        assertEquals(403, request("GET", "/api/exams", student.token, null).status);
        assertEquals(403, access(teacher, exam).status);
        assertEquals(404, request("GET", "/api/exams/" + exam.get("id").asLong(), other.token, null).status);
        assertEquals(404, request("PATCH", "/api/exams/" + exam.get("id").asLong() + "/publish", other.token, null).status);
        assertEquals(0, request("GET", "/api/exams", other.token, null).body.size());
        assertEquals(1, request("GET", "/api/exams", teacher.token, null).body.size());
        request("PATCH", "/api/admin/users/" + student.id + "/active", admin, Map.of("active", false));
        assertEquals(401, access(student, exam).status);
    }
    @Test void accessAndStartCannotBeSkippedOrBorrowed() throws Exception {
        Account teacher = account("TEACHER"), student = account("STUDENT"), other = account("STUDENT");
        JsonNode exam = create(teacher, "PUBLISHED", List.of(mcq()));
        assertEquals(403, request("POST", path(exam, "/start"), student.token, null).status);
        assertEquals(403, request("POST", path(exam, "/submit"), student.token, List.of()).status);
        assertEquals(200, access(student, exam).status);
        assertEquals(409, request("POST", path(exam, "/submit"), student.token, List.of()).status);
        assertEquals(403, request("POST", path(exam, "/start"), other.token, null).status);
        assertEquals(400, request("POST", "/api/student/exams/access", student.token,
                Map.of("registrationNumber", "", "symbolNumber", "s", "accessCode", "ABCDEF")).status);
        assertEquals(404, request("POST", "/api/student/exams/access", student.token,
                Map.of("registrationNumber", "r", "symbolNumber", "s", "accessCode", "!!!!!!")).status);
    }
    @Test void invalidAnswersDoNotPersistPartialGrades() throws Exception {
        Account teacher = account("TEACHER"), student = account("STUDENT");
        JsonNode exam = create(teacher, "PUBLISHED", List.of(mcq(), text()));
        JsonNode otherExam = create(teacher, "PUBLISHED", List.of(mcq()));
        start(student, exam);
        long q = exam.get("questions").get(0).get("id").asLong();
        long tq = exam.get("questions").get(1).get("id").asLong();
        for (List<?> invalid : List.of(
                List.of(Map.of("questionId", otherExam.get("questions").get(0).get("id").asLong(), "selectedOptionIndex", 1)),
                List.of(Map.of("questionId", q, "selectedOptionIndex", 4)),
                List.of(Map.of("questionId", q), Map.of("questionId", q)),
                List.of(Map.of("questionId", tq, "selectedOptionIndex", 0)),
                List.of(Map.of("questionId", q, "textAnswer", "invalid"))))
            assertEquals(400, request("POST", path(exam, "/submit"), student.token, invalid).status);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            Submission saved = submissions.findByExamIdAndStudentId(exam.get("id").asLong(), student.id).orElseThrow();
            assertEquals(SubmissionStatus.IN_PROGRESS, saved.getStatus());
            assertTrue(saved.getAnswers().isEmpty());
        });
    }
    @Test void mcqOnlyExamsAreGradedAndUnansweredQuestionsScoreZero() throws Exception {
        Account teacher = account("TEACHER"), student = account("STUDENT");
        JsonNode exam = create(teacher, "PUBLISHED", List.of(mcq()));
        start(student, exam);
        Result result = request("POST", path(exam, "/submit"), student.token, List.of());
        assertEquals(200, result.status);
        assertEquals(0, result.body.get("totalScore").asInt());
        assertEquals("GRADED", result.body.get("status").asText());
        assertFalse(result.body.get("pendingManualGrading").asBoolean());
    }
    @Test void concurrentDuplicateSubmissionsProduceOneReceipt() throws Exception {
        Account teacher = account("TEACHER"), student = account("STUDENT");
        JsonNode exam = create(teacher, "PUBLISHED", List.of(mcq(), text()));
        start(student, exam);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Result> send = () -> request("POST", path(exam, "/submit"), student.token, answers(exam, 1));
            List<Future<Result>> results = executor.invokeAll(List.of(send, send));
            Result first = results.get(0).get(), second = results.get(1).get();
            assertEquals(200, first.status);
            assertEquals(200, second.status);
            assertEquals(first.body, second.body);
        }
    }
    @Test void expiredSubmissionsAreRejectedByServerTime() throws Exception {
        Account teacher = account("TEACHER"), student = account("STUDENT");
        JsonNode exam = create(teacher, "PUBLISHED", List.of(mcq()));
        start(student, exam);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            Submission saved = submissions.findByExamIdAndStudentId(exam.get("id").asLong(), student.id).orElseThrow();
            saved.setStartedAt(Instant.now().minusSeconds(1000));
        });
        assertEquals(409, request("POST", path(exam, "/submit"), student.token, List.of()).status);
    }
    @Test void invalidExamDefinitionsAreRejectedAndLegacyCorrectOptionIsAccepted() throws Exception {
        Account teacher = account("TEACHER");
        assertEquals(400, request("POST", "/api/exams", teacher.token, examRequest("CLOSED", List.of(mcq()))).status);
        assertEquals(400, request("POST", "/api/exams", teacher.token, examRequest("PUBLISHED", List.of())).status);
        Map<String, Object> malformed = new HashMap<>(mcq());
        malformed.put("correctOptionIndex", 4);
        assertEquals(400, request("POST", "/api/exams", teacher.token, examRequest("DRAFT", List.of(malformed))).status);
        malformed.put("correctOptionIndex", 1);
        malformed.put("options", List.of("A", "B"));
        assertEquals(400, request("POST", "/api/exams", teacher.token, examRequest("DRAFT", List.of(malformed))).status);
        Map<String, Object> legacy = new HashMap<>(mcq());
        legacy.remove("correctOptionIndex"); legacy.put("correctOption", 1);
        assertEquals(1, create(teacher, "DRAFT", List.of(legacy)).get("questions").get(0).get("correctOptionIndex").asInt());
    }
}
