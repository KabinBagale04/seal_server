package com.seal.seal_server.service;

import com.seal.seal_server.dto.*;
import com.seal.seal_server.model.*;
import com.seal.seal_server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static org.springframework.http.HttpStatus.*;
import static com.seal.seal_server.service.ExamService.*;

@Service
@Transactional
public class StudentExamService {
    private final ExamRepository exams;
    private final SubmissionRepository submissions;
    public StudentExamService(ExamRepository exams, SubmissionRepository submissions) {
        this.exams = exams; this.submissions = submissions;
    }
    public StudentExamResponse access(User student, ExamAccessRequest request) {
        if (request == null) throw bad("Access information is required.");
        String registration = required(request.registrationNumber(), "Registration number", 100);
        String symbol = required(request.symbolNumber(), "Symbol number", 100);
        String code = required(request.accessCode(), "Access code", 6).toUpperCase(Locale.ROOT);
        Exam found = exams.findByAccessCode(code)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Published exam not found."));
        Exam exam = published(found.getId());
        Submission submission = submissions.findByExamIdAndStudentId(exam.getId(), student.getId()).orElse(null);
        if (submission == null) {
            submission = new Submission();
            submission.setExam(exam); submission.setStudent(student);
            // Collected identifiers only: no StudentProfile/authoritative registration data exists yet.
            submission.setRegistrationNumber(registration); submission.setSymbolNumber(symbol);
            submission.setStatus(SubmissionStatus.IN_PROGRESS);
            submissions.saveAndFlush(submission);
        } else if (submission.getStatus() != SubmissionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(CONFLICT, "This examination has already been submitted.");
        }
        return ExamViews.student(exam);
    }
    public StartExamResponse start(User student, Long examId) {
        Exam exam = published(examId);
        Submission submission = authorizedAttempt(student, examId);
        if (submission.getStatus() != SubmissionStatus.IN_PROGRESS)
            throw new ResponseStatusException(CONFLICT, "This examination has already been submitted.");
        if (submission.getStartedAt() == null) submission.setStartedAt(Instant.now());
        return new StartExamResponse(submission.getId(), examId, submission.getStartedAt(),
                submission.getStartedAt().plus(exam.getDurationMinutes(), ChronoUnit.MINUTES));
    }
    public SubmissionResponse submit(User student, Long examId, List<SubmitAnswerRequest> request) {
        Exam exam = published(examId);
        Submission submission = authorizedAttempt(student, examId);
        if (submission.getStartedAt() == null) throw new ResponseStatusException(CONFLICT, "Start the examination before submitting.");
        Map<Long, SubmitAnswerRequest> answers = normalize(exam, request);
        if (submission.getStatus() != SubmissionStatus.IN_PROGRESS) {
            for (Answer saved : submission.getAnswers()) {
                SubmitAnswerRequest answer = answers.get(saved.getQuestion().getId());
                if (!Objects.equals(saved.getSelectedOptionIndex(), answer.selectedOptionIndex())
                        || !Objects.equals(saved.getTextAnswer(), answer.textAnswer()))
                    throw new ResponseStatusException(CONFLICT, "Already submitted with different answers.");
            }
            return receipt(submission);
        }
        // Allow only a small transport grace period after the displayed deadline.
        Instant deadline = submission.getStartedAt().plus(exam.getDurationMinutes(), ChronoUnit.MINUTES);
        if (Instant.now().isAfter(deadline.plusSeconds(30)))
            throw new ResponseStatusException(CONFLICT, "Submission deadline has passed.");
        int total = 0;
        boolean textPending = false;
        for (Question question : exam.getQuestions()) {
            SubmitAnswerRequest input = answers.get(question.getId());
            Answer answer = new Answer();
            answer.setSubmission(submission); answer.setQuestion(question);
            answer.setSelectedOptionIndex(input.selectedOptionIndex()); answer.setTextAnswer(input.textAnswer());
            if (question.getType() == QuestionType.MCQ) {
                int marks = Objects.equals(question.getCorrectOptionIndex(), input.selectedOptionIndex()) ? question.getMarks() : 0;
                answer.setAwardedMarks(marks); total += marks;
            } else {
                answer.setAwardedMarks(null); textPending = true;
            }
            submission.getAnswers().add(answer);
        }
        submission.setTotalScore(total);
        submission.setSubmittedAt(Instant.now());
        submission.setStatus(textPending ? SubmissionStatus.SUBMITTED : SubmissionStatus.GRADED);
        submissions.saveAndFlush(submission);
        return receipt(submission);
    }
    private Map<Long, SubmitAnswerRequest> normalize(Exam exam, List<SubmitAnswerRequest> request) {
        if (request == null || request.size() > exam.getQuestions().size()) throw bad("Invalid answers list.");
        Map<Long, Question> questions = new HashMap<>();
        exam.getQuestions().forEach(q -> questions.put(q.getId(), q));
        Map<Long, SubmitAnswerRequest> result = new HashMap<>();
        for (SubmitAnswerRequest input : request) {
            if (input == null || input.questionId() == null || !questions.containsKey(input.questionId()))
                throw bad("Every question must belong to this exam.");
            Question question = questions.get(input.questionId());
            if (result.containsKey(question.getId())) throw bad("Duplicate question answer.");
            if (question.getType() == QuestionType.MCQ) {
                if (input.selectedOptionIndex() != null && (input.selectedOptionIndex() < 0 || input.selectedOptionIndex() >= question.getOptions().size()))
                    throw bad("Invalid selectedOptionIndex.");
                if (input.textAnswer() != null && !input.textAnswer().isBlank()) throw bad("MCQ answers cannot contain text.");
                result.put(question.getId(), new SubmitAnswerRequest(question.getId(), input.selectedOptionIndex(), null));
            } else {
                if (input.selectedOptionIndex() != null) throw bad("TEXT answers cannot select an option.");
                result.put(question.getId(), new SubmitAnswerRequest(question.getId(), null, optional(input.textAnswer(), "Text answer", 50000)));
            }
        }
        for (Question question : exam.getQuestions())
            result.putIfAbsent(question.getId(), new SubmitAnswerRequest(question.getId(), null, question.getType() == QuestionType.TEXT ? "" : null));
        return result;
    }
    private Exam published(Long id) {
        Exam exam = exams.findLockedById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Published exam not found."));
        if (exam.getStatus() != ExamStatus.PUBLISHED) throw new ResponseStatusException(NOT_FOUND, "Published exam not found.");
        return exam;
    }
    private Submission authorizedAttempt(User student, Long examId) {
        return submissions.findByExamIdAndStudentId(examId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Validate your exam access code first."));
    }
    private SubmissionResponse receipt(Submission submission) {
        boolean pending = submission.getStatus() == SubmissionStatus.SUBMITTED;
        return new SubmissionResponse(submission.getId(), submission.getExam().getId(), submission.getStatus(),
                submission.getSubmittedAt(), submission.getTotalScore(), pending,
                pending ? "Submission received. MCQ score recorded; text answers await manual grading."
                        : "Submission received and MCQ grading completed.");
    }
}
