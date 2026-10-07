package com.seal.seal_server.controller;
import com.seal.seal_server.dto.*;
import com.seal.seal_server.model.UserRole;
import com.seal.seal_server.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class GradingController {
    private final ExamAuthorization auth;
    private final GradingService grades;
    public GradingController(ExamAuthorization auth, GradingService grades) { this.auth = auth; this.grades = grades; }
    @GetMapping("/api/exams/{examId}/submissions")
    public List<ResultResponse> list(@RequestHeader(value="Authorization", required=false) String token, @PathVariable Long examId) {
        return grades.list(auth.require(token, UserRole.TEACHER), examId);
    }
    @GetMapping("/api/exams/{examId}/submissions/{id}")
    public GradingResponse detail(@RequestHeader(value="Authorization", required=false) String token, @PathVariable Long examId, @PathVariable Long id) {
        return grades.detail(auth.require(token, UserRole.TEACHER), examId, id);
    }
    @PatchMapping("/api/exams/{examId}/submissions/{id}/grade")
    public GradingResponse grade(@RequestHeader(value="Authorization", required=false) String token, @PathVariable Long examId,
                                 @PathVariable Long id, @RequestBody List<GradeRequest> request) {
        return grades.grade(auth.require(token, UserRole.TEACHER), examId, id, request);
    }
    @GetMapping("/api/student/exams/results")
    public List<ResultResponse> results(@RequestHeader(value="Authorization", required=false) String token) {
        return grades.studentResults(auth.require(token, UserRole.STUDENT));
    }
}
