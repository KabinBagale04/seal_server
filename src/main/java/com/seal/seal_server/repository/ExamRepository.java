package com.seal.seal_server.repository;

import com.seal.seal_server.model.Exam;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
    Optional<Exam> findByAccessCode(String accessCode);
    boolean existsByAccessCode(String accessCode);

    // Serialize state transitions per exam for the single-backend MVP.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exam e where e.id = :id")
    Optional<Exam> findLockedById(@Param("id") Long id);
}
