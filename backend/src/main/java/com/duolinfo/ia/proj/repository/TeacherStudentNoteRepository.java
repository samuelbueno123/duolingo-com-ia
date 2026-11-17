package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.TeacherStudentNote;

@Repository
public interface TeacherStudentNoteRepository extends JpaRepository<TeacherStudentNote, Long> {

    List<TeacherStudentNote> findByTeacher_IdAndStudent_IdOrderByUpdatedAtDesc(Long teacherId, Long studentId);

    List<TeacherStudentNote> findByTeacher_IdAndSchoolClass_IdOrderByUpdatedAtDesc(Long teacherId, Long schoolClassId);

    List<TeacherStudentNote> findByStudent_IdOrderByUpdatedAtDesc(Long studentId);

    Optional<TeacherStudentNote> findByIdAndTeacher_Id(Long id, Long teacherId);
}
