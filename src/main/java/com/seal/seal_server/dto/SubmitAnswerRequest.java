package com.seal.seal_server.dto;

public record SubmitAnswerRequest(Long questionId, Integer selectedOptionIndex, String textAnswer) { }
