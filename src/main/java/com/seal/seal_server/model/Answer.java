package com.seal.seal_server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "answers", uniqueConstraints = @UniqueConstraint(name = "uk_answer_submission_question", columnNames = {"submission_id", "question_id"}))
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    private Integer selectedOptionIndex;

    @Column(columnDefinition = "text")
    private String textAnswer;

    private Integer awardedMarks;

    public Long getId() { return id; }
    public Submission getSubmission() { return submission; }
    public void setSubmission(Submission submission) { this.submission = submission; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public Integer getSelectedOptionIndex() { return selectedOptionIndex; }
    public void setSelectedOptionIndex(Integer selectedOptionIndex) { this.selectedOptionIndex = selectedOptionIndex; }
    public String getTextAnswer() { return textAnswer; }
    public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }
    public Integer getAwardedMarks() { return awardedMarks; }
    public void setAwardedMarks(Integer awardedMarks) { this.awardedMarks = awardedMarks; }
}
