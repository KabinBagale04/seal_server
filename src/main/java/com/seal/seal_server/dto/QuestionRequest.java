package com.seal.seal_server.dto;
import com.seal.seal_server.model.QuestionType;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;

public record QuestionRequest(QuestionType type, String questionText, Integer marks, List<String> options,
        @JsonAlias("correctOption") Integer correctOptionIndex, String referenceAnswer) { }
