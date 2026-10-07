package com.seal.seal_server.dto;
import com.seal.seal_server.model.SubmissionStatus;
import java.time.Instant;

public record SubmissionResponse(Long id, Long examId, SubmissionStatus status, Instant submittedAt,
        int totalScore, boolean pendingManualGrading, String message) { }
