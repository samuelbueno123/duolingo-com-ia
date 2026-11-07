package com.duolinfo.ia.proj.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;

@Repository
public interface StudentLanguageRepository extends JpaRepository<StudentLanguage, Long> {
    Optional<StudentLanguage> findByStudentAndLanguageNameIgnoreCase(Student student, String languageName);
}
