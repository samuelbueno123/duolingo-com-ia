package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByGoogleId(String googleId);

    Optional<Student> findByEmail(String email);

    /*
     * Busca alunos pelo idioma.
     */
    List<Student> findByLanguages_LanguageNameIgnoreCase(String languageName);

    default List<Student> findByPreferredLanguage(String preferredLanguage) {
        return findByLanguages_LanguageNameIgnoreCase(preferredLanguage);
    }

    /*
     * Busca alunos matriculados em uma determinada turma.
     */
    List<Student> findByEnrollments_SchoolClass_Id(
        Long schoolClassId
    );

    /*
     * Busca alunos de uma turma por status da matrícula.
     *
     * Exemplo:
     * ACTIVE
     * CANCELLED
     * TRANSFERRED
     * COMPLETED
     */
    List<Student> findByEnrollments_SchoolClass_IdAndEnrollments_Status(
        Long schoolClassId,
        EnrollmentStatus status
    );
}