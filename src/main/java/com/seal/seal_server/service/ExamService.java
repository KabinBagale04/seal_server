package com.seal.seal_server.service;

import com.seal.seal_server.dto.*;
import com.seal.seal_server.model.*;
import com.seal.seal_server.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional
public class ExamService {
    private final ExamRepository exams;
    private final SecureRandom random = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public ExamService(ExamRepository exams) { this.exams = exams; }

    public TeacherExamResponse create(User teacher, CreateExamRequest request) {
        return ExamViews.teacher(exams.saveAndFlush(build(teacher, request)));
    }
    private Exam build(User teacher, CreateExamRequest request) {
        if (request == null) throw bad("Exam request is required.");
        ExamStatus status = request.status() == null ? ExamStatus.DRAFT : request.status();
        if (status != ExamStatus.DRAFT && status != ExamStatus.PUBLISHED) throw bad("Create as DRAFT or PUBLISHED.");
        if (request.durationMinutes() == null || request.durationMinutes() < 1 || request.durationMinutes() > 480)
            throw bad("Duration must be between 1 and 480 minutes.");
        Exam exam = new Exam();
        exam.setTeacher(teacher);
        exam.setTitle(required(request.title(), "Title", 200));
        exam.setSubject(required(request.subject(), "Subject", 200));
        exam.setSubjectCode(required(request.subjectCode(), "Subject code", 50));
        exam.setDurationMinutes(request.durationMinutes());
        exam.setInstructions(optional(request.instructions(), "Instructions", 20000));
        exam.setStatus(status);
        exam.setCreatedAt(Instant.now());
        if (request.questions() == null || request.questions().size() > 200)
            throw bad("Provide a questions list with at most 200 questions.");
        if (status == ExamStatus.PUBLISHED && request.questions().isEmpty()) throw bad("A published exam needs questions.");
        for (int i = 0; i < request.questions().size(); i++) {
            QuestionRequest input = request.questions().get(i);
            if (input == null || input.type() == null || input.marks() == null || input.marks() < 1 || input.marks() > 1000)
                throw bad("Each question needs a type and marks between 1 and 1000.");
            Question question = new Question();
            question.setExam(exam);
            question.setQuestionOrder(i);
            question.setType(input.type());
            question.setQuestionText(required(input.questionText(), "Question text", 20000));
            question.setMarks(input.marks());
            if (input.type() == QuestionType.MCQ) {
                if (input.options() == null || input.options().size() != 4 || input.correctOptionIndex() == null
                        || input.correctOptionIndex() < 0 || input.correctOptionIndex() > 3)
                    throw bad("MCQ requires four options and a zero-based correctOptionIndex (0-3).");
                question.setCorrectOptionIndex(input.correctOptionIndex());
                for (int j = 0; j < 4; j++) {
                    QuestionOption option = new QuestionOption();
                    option.setQuestion(question);
                    option.setOptionOrder(j);
                    option.setOptionText(required(input.options().get(j), "Option text", 5000));
                    question.getOptions().add(option);
                }
            } else {
                if (input.correctOptionIndex() != null || (input.options() != null && !input.options().isEmpty()))
                    throw bad("TEXT questions cannot contain MCQ options or a correctOptionIndex.");
                question.setReferenceAnswer(optional(input.referenceAnswer(), "Reference answer", 20000));
            }
            exam.getQuestions().add(question);
        }
        if (status == ExamStatus.PUBLISHED) exam.setAccessCode(accessCode());
        return exam;
    }
    public TeacherExamResponse update(User teacher, Long id, CreateExamRequest request) {
        Exam exam = owned(teacher, id, true);
        if (exam.getStatus() != ExamStatus.DRAFT) throw new ResponseStatusException(CONFLICT, "Only drafts can be edited.");
        Exam replacement = build(teacher, request);
        exam.setTitle(replacement.getTitle()); exam.setSubject(replacement.getSubject());
        exam.setSubjectCode(replacement.getSubjectCode()); exam.setDurationMinutes(replacement.getDurationMinutes());
        exam.setInstructions(replacement.getInstructions()); exam.setStatus(replacement.getStatus());
        exam.setAccessCode(replacement.getAccessCode());
        exam.getQuestions().clear();
        for (Question q : replacement.getQuestions()) { q.setExam(exam); exam.getQuestions().add(q); }
        exams.flush();
        return ExamViews.teacher(exam);
    }
    public TeacherExamResponse close(User teacher, Long id) {
        Exam exam = owned(teacher, id, true);
        if (exam.getStatus() == ExamStatus.DRAFT) throw new ResponseStatusException(CONFLICT, "A draft cannot be closed.");
        exam.setStatus(ExamStatus.CLOSED);
        return ExamViews.teacher(exam);
    }
    @Transactional(readOnly = true)
    public List<TeacherExamResponse> list(User teacher) {
        return exams.findByTeacherIdOrderByCreatedAtDesc(teacher.getId()).stream().map(ExamViews::teacher).toList();
    }
    @Transactional(readOnly = true)
    public TeacherExamResponse get(User teacher, Long id) { return ExamViews.teacher(owned(teacher, id, false)); }
    public TeacherExamResponse publish(User teacher, Long id) {
        Exam exam = owned(teacher, id, true);
        if (exam.getStatus() == ExamStatus.CLOSED) throw new ResponseStatusException(CONFLICT, "Exam is closed.");
        if (exam.getQuestions().isEmpty()) throw bad("Add questions before publishing.");
        exam.setStatus(ExamStatus.PUBLISHED);
        if (exam.getAccessCode() == null) exam.setAccessCode(accessCode());
        exams.flush();
        return ExamViews.teacher(exam);
    }
    private Exam owned(User teacher, Long id, boolean lock) {
        Exam exam = (lock ? exams.findLockedById(id) : exams.findById(id))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Exam not found."));
        if (!exam.getTeacher().getId().equals(teacher.getId()))
            throw new ResponseStatusException(NOT_FOUND, "Exam not found.");
        return exam;
    }
    private String accessCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder code = new StringBuilder(6);
            for (int i = 0; i < 6; i++) code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            if (!exams.existsByAccessCode(code.toString())) return code.toString();
        }
        throw new ResponseStatusException(CONFLICT, "Could not allocate an access code. Please retry.");
    }
    static String required(String value, String field, int max) {
        if (value == null || value.isBlank() || value.length() > max) throw bad(field + " is required (max " + max + " characters).");
        return value.trim();
    }
    static String optional(String value, String field, int max) {
        if (value != null && value.length() > max) throw bad(field + " is too long.");
        return value == null ? "" : value;
    }
    static ResponseStatusException bad(String message) { return new ResponseStatusException(BAD_REQUEST, message); }
}
