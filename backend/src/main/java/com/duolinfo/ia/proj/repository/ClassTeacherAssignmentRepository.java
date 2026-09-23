package com.duolinfo.ia.proj.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;

@Repository
public interface ClassTeacherAssignmentRepository
        extends JpaRepository<ClassTeacherAssignment, Long> {

    List<ClassTeacherAssignment> findBySchoolClassId(
        Long schoolClassId
    );

    List<ClassTeacherAssignment> findByTeacherId(
        Long teacherId
    );

    Optional<ClassTeacherAssignment> findBySchoolClassIdAndEndDateIsNull(
        Long schoolClassId
    );

    List<ClassTeacherAssignment> findBySchoolClassIdOrderByStartDateDesc(
        Long schoolClassId
    );

    List<ClassTeacherAssignment> findByTeacherIdOrderByStartDateDesc(
        Long teacherId
    );

    List<ClassTeacherAssignment> findByStartDateBetween(
        LocalDate startDate,
        LocalDate endDate
    );

    boolean existsBySchoolClassIdAndEndDateIsNull(
        Long schoolClassId
    );
}