package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.StudentVocabulary;

@Repository
public interface StudentVocabularyRepository extends JpaRepository<StudentVocabulary, Long> {

    List<StudentVocabulary> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    List<StudentVocabulary> findByStudent_IdAndLanguageIgnoreCaseOrderByCreatedAtDesc(Long studentId, String language);

    Optional<StudentVocabulary> findByIdAndStudent_Id(Long id, Long studentId);

    boolean existsByStudent_IdAndTermIgnoreCaseAndLanguageIgnoreCase(Long studentId, String term, String language);
}
