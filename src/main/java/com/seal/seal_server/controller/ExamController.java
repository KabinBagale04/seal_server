package com.seal.seal_server.controller;

import com.seal.seal_server.dto.*;
import com.seal.seal_server.model.UserRole;
import com.seal.seal_server.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {
    private final ExamAuthorization authorization;
    private final ExamService exams;
    public ExamController(ExamAuthorization authorization, ExamService exams) {
        this.authorization = authorization; this.exams = exams;
    }
    @PostMapping
    public ResponseEntity<TeacherExamResponse> create(
            @RequestHeader(value = "Authorization", required = false) String header,
            @RequestBody CreateExamRequest request) {
        return ResponseEntity.status(201).body(exams.create(authorization.require(header, UserRole.TEACHER), request));
    }
    @GetMapping
    // Lists only the authenticated teacher's exams.
    public List<TeacherExamResponse> list(@RequestHeader(value = "Authorization", required = false) String header) {
        return exams.list(authorization.require(header, UserRole.TEACHER));
    }
    @GetMapping("/{id}")
    public TeacherExamResponse get(@RequestHeader(value = "Authorization", required = false) String header, @PathVariable("id") Long id) {
        return exams.get(authorization.require(header, UserRole.TEACHER), id);
    }
    @PatchMapping("/{id}/publish")
    public TeacherExamResponse publish(@RequestHeader(value = "Authorization", required = false) String header, @PathVariable("id") Long id) {
        return exams.publish(authorization.require(header, UserRole.TEACHER), id);
    }
    @PutMapping("/{id}")
    public TeacherExamResponse update(@RequestHeader(value = "Authorization", required = false) String header,
                                      @PathVariable("id") Long id, @RequestBody CreateExamRequest request) {
        return exams.update(authorization.require(header, UserRole.TEACHER), id, request);
    }
    @PatchMapping("/{id}/close")
    public TeacherExamResponse close(@RequestHeader(value = "Authorization", required = false) String header,
                                     @PathVariable("id") Long id) {
        return exams.close(authorization.require(header, UserRole.TEACHER), id);
    }
}
