package com.seal.seal_server.service;

import com.seal.seal_server.dto.*;
import com.seal.seal_server.model.*;
import java.util.List;

final class ExamViews {
    private ExamViews() { }
    private static List<String> options(Question q) {
        return q.getOptions().stream().map(QuestionOption::getOptionText).toList();
    }
    static TeacherExamResponse teacher(Exam e) {
        return new TeacherExamResponse(e.getId(), e.getTitle(), e.getSubject(), e.getSubjectCode(),
                e.getDurationMinutes(), e.getInstructions(), e.getStatus(), e.getAccessCode(), e.getCreatedAt(),
                e.getQuestions().stream().map(q -> new TeacherQuestionResponse(q.getId(), q.getType(),
                        q.getQuestionText(), q.getMarks(), options(q), q.getCorrectOptionIndex(), q.getReferenceAnswer())).toList());
    }
    static StudentExamResponse student(Exam e) {
        // Separate DTO construction prevents keys and marking guides crossing the student boundary.
        return new StudentExamResponse(e.getId(), e.getTitle(), e.getSubject(), e.getSubjectCode(),
                e.getDurationMinutes(), e.getInstructions(), false,
                e.getQuestions().stream().map(q -> new StudentQuestionResponse(q.getId(), q.getType(),
                        q.getQuestionText(), q.getMarks(), options(q))).toList());
    }
}
