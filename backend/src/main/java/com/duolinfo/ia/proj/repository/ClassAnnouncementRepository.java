package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.ClassAnnouncement;

@Repository
public interface ClassAnnouncementRepository extends JpaRepository<ClassAnnouncement, Long> {

    List<ClassAnnouncement> findBySchoolClass_IdOrderByPinnedDescCreatedAtDesc(Long schoolClassId);

    List<ClassAnnouncement> findByTeacher_IdOrderByCreatedAtDesc(Long teacherId);
}
