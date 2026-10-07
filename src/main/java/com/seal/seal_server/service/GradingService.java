package com.seal.seal_server.service;

import com.seal.seal_server.dto.*;
import com.seal.seal_server.model.*;
import com.seal.seal_server.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.springframework.http.HttpStatus.*;

@Service
@Transactional
public class GradingService {
    private final ExamRepository exams;
    private final SubmissionRepository submissions;
    public GradingService(ExamRepository exams, SubmissionRepository submissions) {
        this.exams = exams; this.submissions = submissions;
    }
    private Exam owned(User teacher, Long id) {
        Exam exam = exams.findLockedById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Exam not found."));
        if (!exam.getTeacher().getId().equals(teacher.getId())) throw new ResponseStatusException(NOT_FOUND, "Exam not found.");
        return exam;
    }
    private Submission submission(Long examId, Long id) {
        Submission s = submissions.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Submission not found."));
        if (!s.getExam().getId().equals(examId)) throw new ResponseStatusException(NOT_FOUND, "Submission not found.");
        return s;
    }
    public List<ResultResponse> list(User teacher, Long examId) {
        owned(teacher, examId);
        return submissions.findByExamIdOrderByIdDesc(examId).stream().map(this::result).toList();
    }
    @Transactional(readOnly = true)
    public List<ResultResponse> studentResults(User student) {
        return submissions.findByStudentIdOrderByIdDesc(student.getId()).stream()
                .filter(s -> s.getStatus() != SubmissionStatus.IN_PROGRESS).map(this::result).toList();
    }
    public GradingResponse detail(User teacher, Long examId, Long id) {
        owned(teacher, examId);
        return view(submission(examId, id));
    }
    public GradingResponse grade(User teacher, Long examId, Long id, List<GradeRequest> request) {
        owned(teacher, examId);
        Submission s = submission(examId, id);
        if (s.getStatus() == SubmissionStatus.IN_PROGRESS) throw new ResponseStatusException(CONFLICT, "The attempt is not submitted.");
        if (request == null) throw ExamService.bad("Text grades are required.");
        Map<Long, Integer> grades = new HashMap<>();
        for (GradeRequest grade : request) {
            if (grade == null || grade.questionId() == null || grade.awardedMarks() == null
                    || grades.putIfAbsent(grade.questionId(), grade.awardedMarks()) != null)
                throw ExamService.bad("Each text answer needs one grade.");
        }
        long textCount = s.getAnswers().stream().filter(a -> a.getQuestion().getType() == QuestionType.TEXT).count();
        if (grades.size() != textCount) throw ExamService.bad("Grade every text answer, and only text answers.");
        int total = 0;
        for (Answer answer : s.getAnswers()) {
            Question q = answer.getQuestion();
            if (q.getType() == QuestionType.TEXT) {
                Integer marks = grades.get(q.getId());
                if (marks == null || marks < 0 || marks > q.getMarks()) throw ExamService.bad("Awarded marks must be within the question's maximum.");
                answer.setAwardedMarks(marks);
            }
            total += answer.getAwardedMarks();
        }
        s.setTotalScore(total); s.setStatus(SubmissionStatus.GRADED);
        submissions.flush();
        return view(s);
    }
    private ResultResponse result(Submission s) {
        return new ResultResponse(s.getId(), s.getExam().getId(), s.getExam().getTitle(),
                s.getStudent().getId(), s.getStudent().getFullName(), s.getStatus(), s.getTotalScore(),
                s.getExam().getQuestions().stream().mapToInt(Question::getMarks).sum());
    }
    private GradingResponse view(Submission s) {
        return new GradingResponse(result(s), s.getAnswers().stream().sorted(Comparator.comparing(a -> a.getQuestion().getQuestionOrder()))
                .map(a -> new GradingResponse.Item(a.getQuestion().getId(), a.getQuestion().getType(),
                        a.getQuestion().getQuestionText(), a.getQuestion().getMarks(),
                        a.getQuestion().getOptions().stream().map(QuestionOption::getOptionText).toList(),
                        a.getSelectedOptionIndex(), a.getTextAnswer(), a.getQuestion().getReferenceAnswer(), a.getAwardedMarks())).toList());
    }
}
