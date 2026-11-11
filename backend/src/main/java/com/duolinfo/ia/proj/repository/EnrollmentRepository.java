package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /*
     * Todas as matrículas de um aluno.
     */
    List<Enrollment> findByStudentId(Long studentId);

    /*
     * Todas as matrículas de uma turma.
     */
    List<Enrollment> findBySchoolClassId(Long schoolClassId);

    /*
     * Matrículas por status.
     */
    List<Enrollment> findByStatus(EnrollmentStatus status);

    /*
     * Matrículas de um aluno com determinado status.
     */
    List<Enrollment> findByStudentIdAndStatus(
        Long studentId,
        EnrollmentStatus status
    );

    /*
     * Matrículas de uma turma com determinado status.
     */
    List<Enrollment> findBySchoolClassIdAndStatus(
        Long schoolClassId,
        EnrollmentStatus status
    );

    /*
     * Matrículas de uma turma ordenadas pela data.
     */
    List<Enrollment> findBySchoolClassIdAndStatusOrderByEnrollmentDateAsc(
        Long schoolClassId,
        EnrollmentStatus status
    );

    /*
     * Verifica se o aluno já possui determinada situação
     * de matrícula naquela turma.
     */
    boolean existsByStudentIdAndSchoolClassIdAndStatus(
        Long studentId,
        Long schoolClassId,
        EnrollmentStatus status
    );

    /*
     * Busca matrículas ativas de uma turma com o aluno e seus idiomas em uma única consulta,
     * eliminando o problema de N+1 queries.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT e FROM Enrollment e " +
        "JOIN FETCH e.student s " +
        "LEFT JOIN FETCH s.languages " +
        "WHERE e.schoolClass.id = :schoolClassId AND e.status = :status"
    )
    List<Enrollment> findActiveWithStudentAndLanguagesBySchoolClassId(
        @org.springframework.data.repository.query.Param("schoolClassId") Long schoolClassId,
        @org.springframework.data.repository.query.Param("status") EnrollmentStatus status
    );
}