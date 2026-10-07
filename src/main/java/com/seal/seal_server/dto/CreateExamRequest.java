package com.seal.seal_server.dto;
import com.seal.seal_server.model.ExamStatus;
import java.util.List;

public record CreateExamRequest(String title, String subject, String subjectCode, Integer durationMinutes,
        String instructions, ExamStatus status, List<QuestionRequest> questions) { }
