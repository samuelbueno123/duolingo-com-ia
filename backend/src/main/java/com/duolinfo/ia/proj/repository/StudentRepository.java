package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByGoogleId(String googleId);

    Optional<Student> findByEmail(String email);

    List<Student> findByLanguages_LanguageNameIgnoreCase(String languageName);

    default List<Student> findByPreferredLanguage(String preferredLanguage) {
        return findByLanguages_LanguageNameIgnoreCase(preferredLanguage);
    }
}
