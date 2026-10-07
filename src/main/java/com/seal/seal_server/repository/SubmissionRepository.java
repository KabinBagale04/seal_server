package com.seal.seal_server.repository;

import com.seal.seal_server.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByExamIdAndStudentId(Long examId, Long studentId);
    List<Submission> findByExamIdOrderByIdDesc(Long examId);
    List<Submission> findByStudentIdOrderByIdDesc(Long studentId);
}
