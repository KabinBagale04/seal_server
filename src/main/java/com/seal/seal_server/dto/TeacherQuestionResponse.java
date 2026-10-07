package com.seal.seal_server.dto;
import com.seal.seal_server.model.QuestionType;
import java.util.List;

public record TeacherQuestionResponse(Long id, QuestionType type, String questionText, int marks,
        List<String> options, Integer correctOptionIndex, String referenceAnswer) { }
