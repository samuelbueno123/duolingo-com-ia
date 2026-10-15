package com.duolinfo.ia.proj.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.duolinfo.ia.proj.entity.LearningExercise;

public interface LearningExerciseRepository
        extends JpaRepository<LearningExercise, Long> {

    List<LearningExercise> findBySchoolClass_IdInAndActiveTrueOrderByCreatedAtDesc(
        Collection<Long> classIds
    );

    List<LearningExercise> findByTeacher_IdOrderByCreatedAtDesc(Long teacherId);

    Optional<LearningExercise> findByIdAndTeacher_Id(Long id, Long teacherId);
}
