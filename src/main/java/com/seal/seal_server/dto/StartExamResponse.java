package com.seal.seal_server.dto;
import java.time.Instant;

public record StartExamResponse(Long submissionId, Long examId, Instant startedAt, Instant expiresAt) { }
