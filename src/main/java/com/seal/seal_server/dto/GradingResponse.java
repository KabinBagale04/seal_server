package com.seal.seal_server.dto;
import java.util.List;
import com.seal.seal_server.model.QuestionType;
public record GradingResponse(ResultResponse result, List<Item> answers) {
    public record Item(Long questionId, QuestionType type, String questionText, int marks,
                       List<String> options, Integer selectedOptionIndex, String textAnswer,
                       String referenceAnswer, Integer awardedMarks) { }
}
