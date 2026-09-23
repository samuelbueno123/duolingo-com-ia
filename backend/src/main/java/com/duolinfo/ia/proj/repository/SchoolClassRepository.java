package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.SchoolClass;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    /*
     * Todas as turmas de uma escola.
     */
    List<SchoolClass> findBySchoolId(Long schoolId);

    /*
     * Turmas ativas de uma escola.
     */
    List<SchoolClass> findBySchoolIdAndActive(
        Long schoolId,
        Boolean active
    );

    /*
     * Turmas de uma escola em determinado ano letivo.
     */
    List<SchoolClass> findBySchoolIdAndSchoolYear(
        Long schoolId,
        String schoolYear
    );

    /*
     * Turmas de uma escola e ano letivo,
     * ordenadas pelo nome da turma.
     *
     * Exemplo:
     * 1º Ano A
     * 1º Ano B
     * 2º Ano A
     */
    List<SchoolClass> findBySchoolIdAndSchoolYearOrderByNameAsc(
        Long schoolId,
        String schoolYear
    );

    /*
     * Turmas ativas de uma escola em determinado ano.
     */
    List<SchoolClass> findBySchoolIdAndSchoolYearAndActive(
        Long schoolId,
        String schoolYear,
        Boolean active
    );

    /*
     * Todas as turmas de determinado ano.
     */
    List<SchoolClass> findBySchoolYear(String schoolYear);

    /*
     * Turmas por turno.
     */
    List<SchoolClass> findByShift(String shift);

    /*
     * Turmas por nível/série.
     */
    List<SchoolClass> findByGradeLevelContainingIgnoreCase(
        String gradeLevel
    );

    /*
     * Busca pelo nome dentro de uma escola.
     */
    List<SchoolClass> findBySchoolIdAndNameContainingIgnoreCase(
        Long schoolId,
        String name
    );

    /*
     * Turmas de uma escola, ano e turno.
     */
    List<SchoolClass> findBySchoolIdAndSchoolYearAndShift(
        Long schoolId,
        String schoolYear,
        String shift
    );

    /*
     * Todas as turmas ativas.
     */
    List<SchoolClass> findByActive(Boolean active);

    /*
     * Verifica se já existe uma turma com a mesma combinação.
     *
     * school + ano + nome + turno
     */
    boolean existsBySchoolIdAndSchoolYearAndNameAndShift(
        Long schoolId,
        String schoolYear,
        String name,
        String shift
    );
}