package com.seal.seal_server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "question_options")
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, columnDefinition = "text")
    private String optionText;

    @Column(nullable = false)
    private int optionOrder;

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
    public int getOptionOrder() { return optionOrder; }
    public void setOptionOrder(int optionOrder) { this.optionOrder = optionOrder; }
}
