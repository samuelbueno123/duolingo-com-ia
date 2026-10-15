package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.duolinfo.ia.proj.entity.ExerciseSubmission;

public interface ExerciseSubmissionRepository
        extends JpaRepository<ExerciseSubmission, Long> {

    List<ExerciseSubmission> findByStudent_IdOrderBySubmittedAtDesc(Long studentId);

    List<ExerciseSubmission> findByExercise_Teacher_IdOrderBySubmittedAtDesc(
        Long teacherId
    );

    List<ExerciseSubmission> findBySubmittedAtAfterOrderBySubmittedAtDesc(
        Instant submittedAt
    );
}
