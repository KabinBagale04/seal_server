package com.seal.seal_server.dto;
import java.util.List;

public record StudentExamResponse(Long id, String title, String subject, String subjectCode, int durationMinutes,
        String instructions, boolean identityDetailsVerified, List<StudentQuestionResponse> questions) { }
