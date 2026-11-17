package com.duolinfo.ia.proj.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;

@Repository
public interface ClassTeacherAssignmentRepository
        extends JpaRepository<ClassTeacherAssignment, Long> {

    @Override
    @EntityGraph(attributePaths = {"schoolClass", "schoolClass.school", "schoolClass.school.institution", "schoolClass.institution", "teacher", "teacher.institution"})
    Optional<ClassTeacherAssignment> findById(Long id);

    @EntityGraph(attributePaths = {"schoolClass", "schoolClass.school", "schoolClass.school.institution", "schoolClass.institution", "teacher", "teacher.institution"})
    List<ClassTeacherAssignment> findBySchoolClassId(
        Long schoolClassId
    );

    @EntityGraph(attributePaths = {"schoolClass", "schoolClass.school", "schoolClass.school.institution", "schoolClass.institution", "teacher", "teacher.institution"})
    List<ClassTeacherAssignment> findByTeacherId(
        Long teacherId
    );

    @EntityGraph(attributePaths = {"schoolClass", "schoolClass.school", "schoolClass.school.institution", "schoolClass.institution", "teacher", "teacher.institution"})
    Optional<ClassTeacherAssignment> findBySchoolClassIdAndEndDateIsNull(
        Long schoolClassId
    );

    @EntityGraph(attributePaths = {"schoolClass", "schoolClass.school", "schoolClass.school.institution", "schoolClass.institution", "teacher", "teacher.institution"})
    List<ClassTeacherAssignment> findBySchoolClassIdOrderByStartDateDesc(
        Long schoolClassId
    );

    @EntityGraph(attributePaths = {"schoolClass", "schoolClass.school", "schoolClass.school.institution", "schoolClass.institution", "teacher", "teacher.institution"})
    List<ClassTeacherAssignment> findByTeacherIdOrderByStartDateDesc(
        Long teacherId
    );

    @EntityGraph(attributePaths = {"schoolClass", "schoolClass.school", "schoolClass.school.institution", "schoolClass.institution", "teacher", "teacher.institution"})
    List<ClassTeacherAssignment> findByStartDateBetween(
        LocalDate startDate,
        LocalDate endDate
    );

    boolean existsBySchoolClassIdAndEndDateIsNull(
        Long schoolClassId
    );
}