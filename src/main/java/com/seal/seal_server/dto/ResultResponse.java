package com.seal.seal_server.dto;
import com.seal.seal_server.model.SubmissionStatus;
public record ResultResponse(Long id, Long examId, String examTitle, Long studentId,
                             String studentName, SubmissionStatus status, Integer totalScore, int maximumScore) { }
