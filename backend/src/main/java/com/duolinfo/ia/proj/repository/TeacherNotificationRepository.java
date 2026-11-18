package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.duolinfo.ia.proj.entity.TeacherNotification;

public interface TeacherNotificationRepository extends JpaRepository<TeacherNotification, Long> {

    List<TeacherNotification> findByTeacher_IdOrderByCreatedAtDesc(Long teacherId);

    long countByTeacher_IdAndReadFalse(Long teacherId);

    Optional<TeacherNotification> findByIdAndTeacher_Id(Long id, Long teacherId);
}
