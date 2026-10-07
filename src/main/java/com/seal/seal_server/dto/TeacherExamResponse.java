package com.seal.seal_server.dto;
import com.seal.seal_server.model.ExamStatus;
import java.util.List;
import java.time.Instant;

public record TeacherExamResponse(Long id, String title, String subject, String subjectCode, int durationMinutes,
        String instructions, ExamStatus status, String accessCode, Instant createdAt,
        List<TeacherQuestionResponse> questions) { }
