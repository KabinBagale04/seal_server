package com.seal.seal_server.controller;

import com.seal.seal_server.dto.*;
import com.seal.seal_server.model.UserRole;
import com.seal.seal_server.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/student/exams")
public class StudentExamController {
    private final ExamAuthorization authorization;
    private final StudentExamService exams;
    public StudentExamController(ExamAuthorization authorization, StudentExamService exams) {
        this.authorization = authorization; this.exams = exams;
    }
    @PostMapping("/access")
    public StudentExamResponse access(@RequestHeader(value = "Authorization", required = false) String header,
                                     @RequestBody ExamAccessRequest request) {
        return exams.access(authorization.require(header, UserRole.STUDENT), request);
    }
    @PostMapping("/{examId}/start")
    public StartExamResponse start(@RequestHeader(value = "Authorization", required = false) String header,
                                  @PathVariable("examId") Long examId) {
        return exams.start(authorization.require(header, UserRole.STUDENT), examId);
    }
    @PostMapping("/{examId}/submit")
    public SubmissionResponse submit(@RequestHeader(value = "Authorization", required = false) String header,
                                     @PathVariable("examId") Long examId, @RequestBody List<SubmitAnswerRequest> answers) {
        return exams.submit(authorization.require(header, UserRole.STUDENT), examId, answers);
    }
}
