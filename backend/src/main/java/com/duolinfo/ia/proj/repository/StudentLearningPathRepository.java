package com.duolinfo.ia.proj.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.StudentLearningPath;

@Repository
public interface StudentLearningPathRepository extends JpaRepository<StudentLearningPath, Long> {
    Optional<StudentLearningPath> findByStudentIdAndLanguageIgnoreCase(Long studentId, String language);
}
