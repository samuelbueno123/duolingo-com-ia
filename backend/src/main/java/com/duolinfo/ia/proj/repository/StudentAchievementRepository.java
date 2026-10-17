package com.duolinfo.ia.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.StudentAchievement;

@Repository
public interface StudentAchievementRepository extends JpaRepository<StudentAchievement, Long> {

    List<StudentAchievement> findByStudentId(Long studentId);

    Optional<StudentAchievement> findByStudentIdAndAchievementId(Long studentId, Long achievementId);

    boolean existsByStudentIdAndAchievementId(Long studentId, Long achievementId);

    void deleteByStudentIdAndAchievementId(Long studentId, Long achievementId);
}
