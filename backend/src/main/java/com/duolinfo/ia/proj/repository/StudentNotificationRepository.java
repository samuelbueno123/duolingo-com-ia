package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.StudentNotification;

@Repository
public interface StudentNotificationRepository extends JpaRepository<StudentNotification, Long> {

    List<StudentNotification> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    long countByStudent_IdAndReadFalse(Long studentId);

    Optional<StudentNotification> findByIdAndStudent_Id(Long id, Long studentId);
}
