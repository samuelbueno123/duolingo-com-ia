package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.StudentModuleProgress;

@Repository
public interface StudentModuleProgressRepository extends JpaRepository<StudentModuleProgress, Long> {
    List<StudentModuleProgress> findByStudentId(Long studentId);
    Optional<StudentModuleProgress> findByStudentIdAndModuleId(Long studentId, Long moduleId);
}
