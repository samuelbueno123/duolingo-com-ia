package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.StudentMistake;

@Repository
public interface StudentMistakeRepository extends JpaRepository<StudentMistake, Long> {

    List<StudentMistake> findByStudent_IdAndResolvedFalseOrderByCreatedAtDesc(Long studentId);

    List<StudentMistake> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    long countByStudent_IdAndResolvedFalse(Long studentId);

    Optional<StudentMistake> findByIdAndStudent_Id(Long id, Long studentId);
}
